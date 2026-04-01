package com.example.datn_sd_29.reservation.service;

import com.example.datn_sd_29.common.service.TableStatusBroadcastService;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.repository.EmployeeRepository;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.common.service.EmailService;
import com.example.datn_sd_29.reservation.dto.AvailableTableResponse;
import com.example.datn_sd_29.reservation.dto.ReservationRequest;
import com.example.datn_sd_29.reservation.dto.ReservationResponse;
import com.example.datn_sd_29.reservation.dto.ReservationListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ReservationService {
    private final CustomerRepository customerRepository;
    private final DiningTableRepository diningTableRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;
    private final TableStatusBroadcastService tableStatusBroadcastService;
    
    @org.springframework.beans.factory.annotation.Value("${security.api.enabled:true}")
    private boolean securityEnabled;

    private static final int RESERVATION_DURATION_MINUTES = 90;
    private static final int PRE_RESERVE_BUFFER_MINUTES = 30;
    private static final int CLEANUP_BUFFER_MINUTES = 30; // Thời gian dọn dẹp sau khi khách ăn xong
    private static final int FUTURE_RESERVATION_CHECK_HOURS = 3; // Check future reservations within 3 hours
    private static final int CHECK_IN_EARLY_BUFFER_MINUTES = 60; // Allow check-in up to 60 minutes early
    private static final int CHECK_IN_LATE_BUFFER_MINUTES = 30; // Allow check-in up to 30 minutes late
    private static final int MAX_TABLES_TO_COMBINE = 5;
    private static final Set<String> ACTIVE_STATUSES = Set.of("RESERVED", "CONFIRMED", "IN_PROGRESS");
    private static final Set<String> PROMOTION_OPTIONS = Set.of(
            "Ưu đãi sinh nhật 10% tổng hóa đơn",
            "Có mã ưu đãi riêng",
            "Đầy tiền không cần ưu đãi"
    );

    public List<AvailableTableResponse> findAvailableTables(LocalDateTime reservedAt, Integer guestCount) {
        List<DiningTable> available = getAvailableDiningTables(reservedAt, guestCount);

        return available.stream()
                .map(table -> new AvailableTableResponse(
                        table.getId(),
                        "MB-" + table.getId(),
                        table.getTableName(),
                        table.getSeatingCapacity()
                ))
                .collect(Collectors.toList());
    }

    public ReservationResponse reserveTable(ReservationRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Nếu security bị tắt, bỏ qua authentication check
        if (securityEnabled && (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
            || "anonymousUser".equals(auth.getPrincipal()))) {
            throw new IllegalArgumentException("Vui lòng đăng nhập lại tài khoản!");
        }

        // Khi security tắt, sử dụng email mặc định hoặc tạo customer mới
        String email;
        Customer customer;
        
        if (securityEnabled) {
            // Production mode: lấy email từ JWT token
            email = auth.getPrincipal().toString();
            customer = customerRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new IllegalArgumentException("Vui lòng đăng nhập tài khoản!"));
        } else {
            // Development mode: tạo hoặc tìm customer với email test
            email = "test@example.com";
            customer = customerRepository.findByEmailIgnoreCase(email)
                    .orElseGet(() -> {
                        Customer newCustomer = new Customer();
                        newCustomer.setEmail(email);
                        newCustomer.setFullName(request.getFullName());
                        newCustomer.setPhoneNumber(request.getPhoneNumber());
                        return customerRepository.save(newCustomer);
                    });
        }

        customer.setFullName(request.getFullName());
        customer.setPhoneNumber(request.getPhoneNumber());
        customerRepository.save(customer);

        if (!PROMOTION_OPTIONS.contains(request.getPromotionType())) {
            throw new IllegalArgumentException("Promotion type is invalid");
        }

        LocalDateTime reservedAt = request.getReservedAt();
        if (reservedAt == null || reservedAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reserved time is invalid");
        }

        Integer guestCount = request.getGuestCount();
        if (guestCount == null || guestCount < 1) {
            throw new IllegalArgumentException("Guest count must be greater than 0");
        }

        List<DiningTable> availableTables = getAvailableDiningTables(reservedAt, guestCount);
        List<DiningTable> selectedTables = selectTablesForGuests(availableTables, guestCount, reservedAt);
        if (selectedTables.isEmpty()) {
            throw new IllegalArgumentException("Không còn bàn phù hợp trong thời gian đã chọn");
        }

        // Lock tables to prevent race condition (pessimistic write lock)
        List<Integer> tableIds = selectedTables.stream()
                .map(DiningTable::getId)
                .collect(Collectors.toList());
        diningTableRepository.lockTablesForReservation(tableIds);

        // Validate 120-minute gap from existing reservations (Requirements 6.1, 6.2, 6.3, 6.4, 6.5)
        validateReservationGap(selectedTables, reservedAt);

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);

        Employee emp = employeeRepository.findById(1)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: 1"));
        invoice.setEmployee(emp);

        invoice.setInvoiceChannel("ONLINE");
        invoice.setInvoiceStatus("RESERVED");
        invoice.setReservationCode("RSV-" + System.currentTimeMillis());
        invoice.setInvoiceCode("INV-" + UUID.randomUUID());

        invoice.setReservedAt(reservedAt);
        invoice.setGuestCount(guestCount);
        invoice.setPromotionType(request.getPromotionType());
        invoice.setReservationNote(request.getNote());
        invoice.setFoodNote(request.getFoodNote());

        invoice = invoiceRepository.save(invoice);

        for (DiningTable table : selectedTables) {
            InvoiceDiningTable idt = new InvoiceDiningTable();
            idt.setInvoice(invoice);
            idt.setDiningTable(table);
            invoiceDiningTableRepository.save(idt);
        }

        // Broadcast RESERVED status via WebSocket (reuse tableIds from lock)
        tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "RESERVED");

        List<ReservationResponse.TableInfo> tables = toTableInfos(selectedTables);
        return buildReservationResponse(invoice, customer, tables);
    }

    public ReservationResponse getReservationByCode(String reservationCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Nếu security bị tắt, bỏ qua authentication check
        if (securityEnabled && (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
            || "anonymousUser".equals(auth.getPrincipal()))) {
            throw new IllegalArgumentException("Vui lòng đăng nhập lại tài khoản!");
        }

        Invoice invoice = invoiceRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        List<InvoiceDiningTable> links =
                invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());

        List<DiningTable> tables = links.stream()
                .map(InvoiceDiningTable::getDiningTable)
                .collect(Collectors.toList());

        Customer customer = invoice.getCustomer();
        return buildReservationResponse(invoice, customer, toTableInfos(tables));
    }

    public ReservationResponse checkInReservation(String reservationCode) {
        Invoice invoice = invoiceRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        String status = invoice.getInvoiceStatus();
        if (status == null || "CANCELLED".equals(status) || "NO_SHOW".equals(status)) {
            throw new IllegalArgumentException("Reservation is not valid for check-in");
        }

        // Get tables for this reservation
        List<InvoiceDiningTable> links =
                invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());

        List<DiningTable> tables = links.stream()
                .map(InvoiceDiningTable::getDiningTable)
                .collect(Collectors.toList());

        // Validate reserved time
        LocalDateTime reservedAt = invoice.getReservedAt();
        if (reservedAt == null) {
            throw new IllegalArgumentException("Reservation has no reserved time");
        }

        // Calculate flexible time window for conflict detection
        // Allow early check-in (up to 60 minutes before) and late check-in (up to 30 minutes after)
        LocalDateTime windowStart = reservedAt.minusMinutes(CHECK_IN_EARLY_BUFFER_MINUTES);
        LocalDateTime windowEnd = reservedAt.plusMinutes(RESERVATION_DURATION_MINUTES + CHECK_IN_LATE_BUFFER_MINUTES);

        // Check for conflicts within time window
        final Integer currentInvoiceId = invoice.getId();
        for (DiningTable table : tables) {
            final Integer tableId = table.getId();
            
            // Use time-window aware conflict detection
            List<Invoice> conflictingInvoices = invoiceDiningTableRepository
                    .findConflictingInvoicesByTableWithinWindow(
                            tableId,
                            List.of("IN_PROGRESS", "RESERVED"),
                            windowStart,
                            windowEnd,
                            RESERVATION_DURATION_MINUTES
                    );
            
            // Filter out current invoice
            conflictingInvoices = conflictingInvoices.stream()
                    .filter(inv -> !inv.getId().equals(currentInvoiceId))
                    .collect(Collectors.toList());
            
            if (!conflictingInvoices.isEmpty()) {
                Invoice conflict = conflictingInvoices.get(0);
                throw new IllegalStateException(
                    String.format("Bàn %s đã có hóa đơn %s (trạng thái: %s) trong khoảng thời gian xung đột. Vui lòng chọn bàn khác hoặc hủy hóa đơn xung đột.",
                        table.getTableName(),
                        conflict.getInvoiceCode(),
                        conflict.getInvoiceStatus())
                );
            }
        }

        // Now set this invoice to IN_PROGRESS
        invoice.setInvoiceStatus("IN_PROGRESS");
        invoice.setCheckedInAt(java.time.Instant.now());
        invoice = invoiceRepository.save(invoice);

        // Update table status to IN_USE (fix for table status display mismatch)
        List<Integer> tableIds = tables.stream()
                .map(DiningTable::getId)
                .collect(Collectors.toList());
        
        if (!tableIds.isEmpty()) {
            diningTableRepository.updateTableStatusByIdIn(tableIds, "IN_USE");
            tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "IN_USE");
        }

        Customer customer = invoice.getCustomer();
        return buildReservationResponse(invoice, customer, toTableInfos(tables));
    }

    public void sendReservationDetailsEmail(String reservationCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Nếu security bị tắt, bỏ qua authentication check
        if (securityEnabled && (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
            || "anonymousUser".equals(auth.getPrincipal()))) {
            throw new IllegalArgumentException("Vui lòng đăng nhập lại tài khoản!");
        }

        String email = securityEnabled ? auth.getPrincipal().toString() : null;

        Invoice invoice = invoiceRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        Customer customer = invoice.getCustomer();
        if (customer == null || customer.getEmail() == null) {
            throw new IllegalArgumentException("Customer email not found");
        }
        if (!email.equalsIgnoreCase(customer.getEmail())) {
            throw new IllegalArgumentException("Không có quyền gửi email cho đặt bàn này");
        }

        List<InvoiceDiningTable> links =
                invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());

        List<String> tableCodes = links.stream()
                .map(link -> "MB-" + link.getDiningTable().getId())
                .collect(Collectors.toList());

        emailService.sendReservationDetailsEmail(
                customer.getEmail(),
                invoice.getReservationCode(),
                invoice.getReservedAt(),
                invoice.getGuestCount(),
                invoice.getPromotionType(),
                invoice.getReservationNote(),
                tableCodes
        );
    }

    public List<ReservationListResponse> findReservationsByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
        }

        String trimmedPhone = phoneNumber.trim();
        List<Invoice> invoices = invoiceRepository.findReservationsByPhoneNumber(trimmedPhone);
        
        return invoices.stream()
                .map(invoice -> {
                    List<InvoiceDiningTable> links = 
                        invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
                    
                    List<ReservationListResponse.TableInfo> tables = links.stream()
                            .map(link -> new ReservationListResponse.TableInfo(
                                    link.getDiningTable().getId(),
                                    "MB-" + link.getDiningTable().getId(),
                                    link.getDiningTable().getTableName(),
                                    link.getDiningTable().getSeatingCapacity()
                            ))
                            .collect(Collectors.toList());
                    
                    Customer customer = invoice.getCustomer();
                    return new ReservationListResponse(
                            invoice.getId(),
                            invoice.getReservationCode(),
                            invoice.getReservedAt(),
                            invoice.getGuestCount(),
                            customer != null ? customer.getFullName() : "",
                            customer != null ? customer.getPhoneNumber() : "",
                            invoice.getInvoiceStatus(),
                            invoice.getPromotionType(),
                            invoice.getReservationNote(),
                            invoice.getFoodNote(),
                            tables
                    );
                })
                .collect(Collectors.toList());
    }

    public void cancelReservation(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt bàn"));

        if (!"RESERVED".equals(invoice.getInvoiceStatus())) {
            throw new IllegalArgumentException("Chỉ có thể hủy đặt bàn có trạng thái RESERVED");
        }

        // Get tables before cancelling
        List<InvoiceDiningTable> links = 
            invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
        
        List<Integer> tableIds = links.stream()
                .map(link -> link.getDiningTable().getId())
                .collect(Collectors.toList());

        // Cancel the reservation
        invoice.setInvoiceStatus("CANCELLED");
        invoiceRepository.save(invoice);

        // Broadcast table status change to AVAILABLE
        if (!tableIds.isEmpty()) {
            tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "AVAILABLE");
        }
    }

    public List<ReservationListResponse> findAllReservedReservations() {
        List<Invoice> invoices = invoiceRepository.findAllReservedReservations();
        
        return invoices.stream()
                .map(invoice -> {
                    List<InvoiceDiningTable> links = 
                        invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
                    
                    List<ReservationListResponse.TableInfo> tables = links.stream()
                            .map(link -> new ReservationListResponse.TableInfo(
                                    link.getDiningTable().getId(),
                                    "MB-" + link.getDiningTable().getId(),
                                    link.getDiningTable().getTableName(),
                                    link.getDiningTable().getSeatingCapacity()
                            ))
                            .collect(Collectors.toList());
                    
                    Customer customer = invoice.getCustomer();
                    return new ReservationListResponse(
                            invoice.getId(),
                            invoice.getReservationCode(),
                            invoice.getReservedAt(),
                            invoice.getGuestCount(),
                            customer != null ? customer.getFullName() : "",
                            customer != null ? customer.getPhoneNumber() : "",
                            invoice.getInvoiceStatus(),
                            invoice.getPromotionType(),
                            invoice.getReservationNote(),
                            invoice.getFoodNote(),
                            tables
                    );
                })
                .collect(Collectors.toList());
    }

    private ReservationResponse buildReservationResponse(
            Invoice invoice,
            Customer customer,
            List<ReservationResponse.TableInfo> tables
    ) {
        String fullName = customer != null ? customer.getFullName() : "";
        String phoneNumber = customer != null ? customer.getPhoneNumber() : "";

        return new ReservationResponse(
                invoice.getReservationCode(),
                invoice.getReservedAt(),
                invoice.getGuestCount(),
                fullName,
                phoneNumber,
                invoice.getInvoiceStatus(),
                invoice.getPromotionType(),
                invoice.getReservationNote(),
                invoice.getFoodNote(),
                tables
        );
    }

    private List<ReservationResponse.TableInfo> toTableInfos(List<DiningTable> tables) {
        return tables.stream()
                .map(table -> new ReservationResponse.TableInfo(
                        table.getId(),
                        "MB-" + table.getId(),
                        table.getTableName(),
                        table.getSeatingCapacity()
                ))
                .collect(Collectors.toList());
    }

    private List<DiningTable> getAvailableDiningTables(LocalDateTime reservedAt, Integer guestCount) {
        if (reservedAt == null) {
            throw new IllegalArgumentException("Reserved time is required");
        }
        if (guestCount == null || guestCount < 1) {
            throw new IllegalArgumentException("Guest count must be greater than 0");
        }

        LocalDateTime windowStart = reservedAt.minusMinutes(PRE_RESERVE_BUFFER_MINUTES);
        LocalDateTime windowEnd = reservedAt.plusMinutes(RESERVATION_DURATION_MINUTES);

        List<DiningTable> candidates = diningTableRepository
                .findBySeatingCapacityGreaterThanEqual(1);

        return candidates.stream()
                .filter(this::isTableServiceable)
                // Filter 1: No overlapping reservations in the booking window
                .filter(table -> !invoiceDiningTableRepository.existsOverlappingReservation(
                        table.getId(), ACTIVE_STATUSES, windowStart, windowEnd, RESERVATION_DURATION_MINUTES))
                // Filter 2: No future reservations soon (prevents walk-in conflicts)
                .filter(table -> !hasFutureReservationSoon(table.getId(), reservedAt))
                .collect(Collectors.toList());
    }

    /**
     * Selects tables for guests with smart prioritization:
     * - Prioritizes "fresh" tables (no recent reservations within 120 minutes before)
     * - Only uses "recently used" tables when no fresh tables available
     * This mimics real staff behavior and maximizes overtime alert effectiveness
     */
    private List<DiningTable> selectTablesForGuests(List<DiningTable> tables, int guestCount, LocalDateTime reservedAt) {
        if (tables.isEmpty()) {
            return List.of();
        }

        // Classify tables by priority
        List<DiningTable> freshTables = new ArrayList<>();
        List<DiningTable> recentlyUsedTables = new ArrayList<>();
        
        for (DiningTable table : tables) {
            if (hasRecentReservation(table.getId(), reservedAt)) {
                recentlyUsedTables.add(table);
            } else {
                freshTables.add(table);
            }
        }
        
        // Try to select from fresh tables first (highest priority)
        List<DiningTable> selected = trySelectFromTables(freshTables, guestCount);
        if (!selected.isEmpty()) {
            return selected;
        }
        
        // Only use recently used tables when no fresh tables available
        // This is when overtime alert system becomes truly useful
        return trySelectFromTables(recentlyUsedTables, guestCount);
    }
    
    /**
     * Checks if a table has any reservation within 120 minutes before the target time
     * This helps identify "recently used" tables that should be deprioritized
     */
    private boolean hasRecentReservation(Integer tableId, LocalDateTime reservedAt) {
        // Check for reservations in the 120-minute window before this reservation
        LocalDateTime checkStart = reservedAt.minusMinutes(120);
        
        return invoiceRepository.hasRecentReservationInWindow(tableId, checkStart, reservedAt);
    }
    
    /**
     * Checks if a table has any future reservation within the next few hours
     * This prevents walk-in customers from being assigned to tables with upcoming reservations
     * 
     * Example: If walk-in at 10:00 and table has reservation at 12:00,
     * this method returns true to prevent conflict
     */
    private boolean hasFutureReservationSoon(Integer tableId, LocalDateTime currentTime) {
        LocalDateTime futureCheckEnd = currentTime.plusHours(FUTURE_RESERVATION_CHECK_HOURS);
        return invoiceRepository.hasFutureReservationInWindow(tableId, currentTime, futureCheckEnd);
    }
    
    /**
     * Attempts to select optimal tables from a given list
     * For recently used tables, sorts by priority score (real-time status)
     * Uses the same algorithm as before: single table → combinations → greedy
     */
    private List<DiningTable> trySelectFromTables(List<DiningTable> tables, int guestCount) {
        if (tables.isEmpty()) {
            return List.of();
        }

        List<DiningTable> sorted = new ArrayList<>(tables);
        
        // Sort by priority score (descending) first, then by capacity
        sorted.sort((t1, t2) -> {
            int score1 = calculateTablePriorityScore(t1.getId());
            int score2 = calculateTablePriorityScore(t2.getId());
            if (score1 != score2) {
                return Integer.compare(score2, score1); // Higher score first
            }
            return Integer.compare(capacitySafe(t1), capacitySafe(t2)); // Then by capacity
        });

        // Try single table first
        for (DiningTable table : sorted) {
            if (capacitySafe(table) >= guestCount) {
                return List.of(table);
            }
        }

        // Try combinations of tables
        int maxTables = Math.min(MAX_TABLES_TO_COMBINE, sorted.size());
        for (int k = 2; k <= maxTables; k++) {
            List<DiningTable> best = findBestCombo(sorted, guestCount, k);
            if (!best.isEmpty()) {
                return best;
            }
        }

        // Greedy approach as last resort
        List<DiningTable> greedy = new ArrayList<>();
        int total = 0;
        for (int i = sorted.size() - 1; i >= 0 && total < guestCount; i--) {
            DiningTable t = sorted.get(i);
            greedy.add(t);
            total += capacitySafe(t);
        }

        return total >= guestCount ? greedy : List.of();
    }
    
    /**
     * Calculates priority score for a table based on real-time status
     * Higher score = higher priority (safer to assign)
     * 
     * Score breakdown:
     * - 100: Table's current invoice is PAID or CANCELLED (definitely available)
     * - 80-99: Table's current invoice is IN_PROGRESS and near completion (90+ minutes)
     * - 60-79: Table's current invoice is IN_PROGRESS and mid-way (60-89 minutes)
     * - 40-59: Table's current invoice is IN_PROGRESS and just started (30-59 minutes)
     * - 20-39: Table's current invoice is IN_PROGRESS and very recent (< 30 minutes)
     * - 10: Table's current invoice is RESERVED but not checked in yet
     * - 0: No recent activity
     */
    private int calculateTablePriorityScore(Integer tableId) {
        // Find the CURRENT active invoice for this table (prioritizes IN_PROGRESS over RESERVED)
        List<Invoice> invoices = invoiceRepository.findCurrentActiveInvoicesByTableId(tableId);
        
        if (invoices == null || invoices.isEmpty()) {
            return 0; // No recent activity
        }
        
        Invoice invoice = invoices.get(0); // Get first result (highest priority)
        String status = invoice.getInvoiceStatus();
        
        // PAID or CANCELLED = definitely available
        if ("PAID".equals(status) || "CANCELLED".equals(status)) {
            return 100;
        }
        
        // RESERVED but not checked in yet = low priority (might still check in)
        if ("RESERVED".equals(status)) {
            return 10;
        }
        
        // IN_PROGRESS = calculate based on dining duration
        if ("IN_PROGRESS".equals(status) && invoice.getCheckedInAt() != null) {
            long diningMinutes = java.time.Duration.between(
                invoice.getCheckedInAt(), 
                java.time.Instant.now()
            ).toMinutes();
            
            if (diningMinutes >= 90) {
                // Near or past expected completion time
                return 80 + (int) Math.min(19, (diningMinutes - 90) / 5); // 80-99
            } else if (diningMinutes >= 60) {
                // Mid-way through meal
                return 60 + (int) ((diningMinutes - 60) * 20 / 30); // 60-79
            } else if (diningMinutes >= 30) {
                // Just started
                return 40 + (int) ((diningMinutes - 30) * 20 / 30); // 40-59
            } else {
                // Very recent
                return 20 + (int) (diningMinutes * 20 / 30); // 20-39
            }
        }
        
        return 0; // Default
    }

    private List<DiningTable> findBestCombo(List<DiningTable> tables, int guestCount, int k) {
        List<DiningTable>[] best = new List[]{List.of()};
        int[] bestCap = new int[]{Integer.MAX_VALUE};

        backtrack(tables, guestCount, k, 0, new ArrayList<>(), 0, best, bestCap);
        return best[0];
    }

    private void backtrack(
            List<DiningTable> tables,
            int guestCount,
            int k,
            int start,
            List<DiningTable> picked,
            int totalCap,
            List<DiningTable>[] best,
            int[] bestCap
    ) {
        if (picked.size() == k) {
            if (totalCap >= guestCount && totalCap < bestCap[0]) {
                bestCap[0] = totalCap;
                best[0] = new ArrayList<>(picked);
            }
            return;
        }

        if (totalCap >= bestCap[0]) {
            return;
        }

        for (int i = start; i < tables.size(); i++) {
            DiningTable t = tables.get(i);
            picked.add(t);
            backtrack(tables, guestCount, k, i + 1, picked, totalCap + capacitySafe(t), best, bestCap);
            picked.remove(picked.size() - 1);
        }
    }

    private boolean isTableServiceable(DiningTable table) {
        String status = table.getTableStatus();
        return status == null || !"OUT_OF_SERVICE".equals(status);
    }

    private int capacitySafe(DiningTable table) {
        return table.getSeatingCapacity() == null ? 0 : table.getSeatingCapacity();
    }

    /**
     * Validates that the new reservation has a 120-minute gap from existing reservations
     * Requirements 6.1, 6.2, 6.3, 6.4, 6.5
     */
    private void validateReservationGap(List<DiningTable> selectedTables, LocalDateTime newReservedAt) {
        LocalDateTime newEndTime = newReservedAt.plusMinutes(RESERVATION_DURATION_MINUTES);
        
        for (DiningTable table : selectedTables) {
            // Use optimized query instead of findAll()
            List<Invoice> existingReservations = invoiceRepository.findReservedReservationsByTableId(table.getId());

            for (Invoice existingReservation : existingReservations) {
                LocalDateTime existingReservedAt = existingReservation.getReservedAt();
                LocalDateTime existingEndTime = existingReservedAt.plusMinutes(RESERVATION_DURATION_MINUTES);
                
                // Check if slots overlap or cleanup buffer is insufficient
                boolean hasConflict = false;
                String conflictReason = "";
                
                if (newReservedAt.isBefore(existingEndTime) && newEndTime.isAfter(existingReservedAt)) {
                    // Slots overlap
                    hasConflict = true;
                    conflictReason = String.format(
                        "Bàn %s đã có đặt bàn từ %s đến %s (trùng thời gian)",
                        table.getTableName(),
                        existingReservedAt.toString(),
                        existingEndTime.toString()
                    );
                } else if (newReservedAt.isAfter(existingEndTime)) {
                    // New is after existing: check cleanup buffer from existingEnd to newStart
                    long gapMinutes = java.time.Duration.between(existingEndTime, newReservedAt).toMinutes();
                    if (gapMinutes < CLEANUP_BUFFER_MINUTES) {
                        hasConflict = true;
                        conflictReason = String.format(
                            "Bàn %s đã có đặt bàn kết thúc lúc %s, cần %d phút dọn dẹp (hiện tại chỉ %d phút)",
                            table.getTableName(),
                            existingEndTime.toString(),
                            CLEANUP_BUFFER_MINUTES,
                            gapMinutes
                        );
                    }
                } else if (existingReservedAt.isAfter(newEndTime)) {
                    // Existing is after new: check cleanup buffer from newEnd to existingStart
                    long gapMinutes = java.time.Duration.between(newEndTime, existingReservedAt).toMinutes();
                    if (gapMinutes < CLEANUP_BUFFER_MINUTES) {
                        hasConflict = true;
                        conflictReason = String.format(
                            "Bàn %s sẽ có đặt bàn lúc %s, cần %d phút dọn dẹp (hiện tại chỉ %d phút)",
                            table.getTableName(),
                            existingReservedAt.toString(),
                            CLEANUP_BUFFER_MINUTES,
                            gapMinutes
                        );
                    }
                }
                
                if (hasConflict) {
                    throw new IllegalArgumentException(conflictReason);
                }
            }
        }
    }
}
