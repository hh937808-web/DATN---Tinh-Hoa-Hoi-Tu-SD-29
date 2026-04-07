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

import java.time.Instant;
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
    private static final int CLEANUP_BUFFER_MINUTES = 30;
    private static final int FUTURE_RESERVATION_CHECK_HOURS = 3;
    private static final int CHECK_IN_EARLY_BUFFER_MINUTES = 60;
    private static final int CHECK_IN_LATE_BUFFER_MINUTES = 30;
    private static final int MAX_TABLES_TO_COMBINE = 5;
    private static final Set<String> ACTIVE_STATUSES = Set.of("RESERVED", "CONFIRMED", "IN_PROGRESS");
    private static final Set<String> PROMOTION_OPTIONS = Set.of(
            "Ưu đãi sinh nhật 10% tổng hóa đơn",
            "Có mã ưu đãi riêng",
            "Đầy tiền không cần ưu đãi"
    );
    
    // Area-based scoring constants
    private static final int SINGLE_AREA_COMPLETE_BONUS = 10000;
    private static final int FLOOR_1_BONUS = 2000;
    private static final int FLOOR_2_BONUS = 1000;
    private static final int AREA_A_BONUS = 500;
    private static final int SINGLE_TABLE_BONUS = 300;
    private static final int CONSECUTIVE_ID_BONUS = 50;
    private static final int ID_DISTANCE_PENALTY = 30;
    private static final int TABLE_COUNT_PENALTY = 50;
    private static final int EXCESS_CAPACITY_PENALTY = 10;

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
        
        // Check if user is authenticated (works in both production and development mode)
        boolean isAuthenticated = auth != null && auth.isAuthenticated() 
                && auth.getPrincipal() != null 
                && !"anonymousUser".equals(auth.getPrincipal());
        
        // In production mode, authentication is required
        if (securityEnabled && !isAuthenticated) {
            throw new IllegalArgumentException("Vui lòng đăng nhập lại tài khoản!");
        }

        // Get customer from JWT token if authenticated, otherwise use test account (dev mode only)
        String email;
        Customer customer;
        
        if (isAuthenticated) {
            // User is logged in: use email from JWT token (works in both prod and dev mode)
            email = auth.getPrincipal().toString();
            customer = customerRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new IllegalArgumentException("Vui lòng đăng nhập tài khoản!"));
        } else {
            // No authentication: only allowed in development mode, use test account
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

        // NEW WORKFLOW: ALWAYS create PENDING_CONFIRMATION, do NOT auto-assign tables
        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);

        // Employee will be assigned later when reception confirms the reservation
        // Do not set employee here for online reservations
        invoice.setEmployee(null);

        invoice.setInvoiceChannel("ONLINE");
        invoice.setInvoiceStatus("PENDING_CONFIRMATION");
        invoice.setReservationNote(request.getNote());
        invoice.setReservationCode("RSV-" + System.currentTimeMillis());
        invoice.setInvoiceCode("INV-" + UUID.randomUUID());
        invoice.setReservedAt(reservedAt);
        invoice.setGuestCount(guestCount);
        invoice.setPromotionType(request.getPromotionType());
        invoice.setFoodNote(request.getFoodNote());

        // Thêm guest info cho đặt bàn hộ
        invoice.setGuestName(request.getGuestName());
        invoice.setGuestPhone(request.getGuestPhone());

        invoice = invoiceRepository.save(invoice);
        
        log.info("Created PENDING_CONFIRMATION reservation {} for {} guests at {}", 
                 invoice.getReservationCode(), guestCount, reservedAt);

        // Return response WITHOUT tables (customer-facing)
        return buildReservationResponse(invoice, customer, List.of());
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

        Customer customer = invoice.getCustomer();
        
        // Customer-facing: NEVER show tables (internal information only)
        // Tables are only visible to reception staff via internal APIs
        return buildReservationResponse(invoice, customer, List.of());
    }

    public ReservationResponse checkInReservation(String reservationCode) {
        Invoice invoice = invoiceRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        String status = invoice.getInvoiceStatus();
        
        // Only allow check-in for RESERVED status
        if (!"RESERVED".equals(status)) {
            if ("PENDING_CONFIRMATION".equals(status)) {
                throw new IllegalArgumentException("Đặt bàn chưa được xác nhận. Vui lòng xác nhận trước khi check-in.");
            }
            if ("CANCELLED".equals(status) || "NO_SHOW".equals(status)) {
                throw new IllegalArgumentException("Đặt bàn đã bị hủy hoặc không hợp lệ");
            }
            throw new IllegalArgumentException("Trạng thái đặt bàn không hợp lệ để check-in");
        }

        // Get tables for this reservation
        List<InvoiceDiningTable> links =
                invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());

        if (links.isEmpty()) {
            throw new IllegalArgumentException("Đặt bàn chưa được xếp bàn. Vui lòng xác nhận trước.");
        }

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
        if (securityEnabled && !email.equalsIgnoreCase(customer.getEmail())) {
            throw new IllegalArgumentException("Không có quyền gửi email cho đặt bàn này");
        }

        // Customer-facing email: NEVER include table codes
        // Tables are internal information only visible to reception staff
        emailService.sendReservationDetailsEmail(
                customer.getEmail(),
                invoice.getReservationCode(),
                invoice.getReservedAt(),
                invoice.getGuestCount(),
                invoice.getPromotionType(),
                invoice.getReservationNote(),
                List.of() // Empty list - no table codes for customer
        );
        
        log.info("Sent customer-facing reservation email to {} (no table information)", customer.getEmail());
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
                                    link.getDiningTable().getSeatingCapacity(),
                                    link.getDiningTable().getArea(),
                                    link.getDiningTable().getFloor()
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
                            invoice.getGuestName(),
                            invoice.getGuestPhone(),
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

        String status = invoice.getInvoiceStatus();
        
        // Allow cancellation for PENDING_CONFIRMATION and RESERVED only
        if (!"PENDING_CONFIRMATION".equals(status) && !"RESERVED".equals(status)) {
            throw new IllegalArgumentException("Chỉ có thể hủy đặt bàn có trạng thái PENDING_CONFIRMATION hoặc RESERVED");
        }

        // Get tables before cancelling (may be empty for PENDING_CONFIRMATION)
        List<InvoiceDiningTable> links = 
            invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
        
        List<Integer> tableIds = links.stream()
                .map(link -> link.getDiningTable().getId())
                .collect(Collectors.toList());

        // Cancel the reservation
        invoice.setInvoiceStatus("CANCELLED");
        invoiceRepository.save(invoice);

        // Broadcast table status change to AVAILABLE only if tables were assigned
        if (!tableIds.isEmpty()) {
            diningTableRepository.updateTableStatusByIdIn(tableIds, "AVAILABLE");
            tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "AVAILABLE");
            log.info("Released {} tables for cancelled reservation {}", tableIds.size(), invoice.getReservationCode());
        } else {
            log.info("Cancelled pending reservation {} (no tables assigned)", invoice.getReservationCode());
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
                                    link.getDiningTable().getSeatingCapacity(),
                                    link.getDiningTable().getArea(),
                                    link.getDiningTable().getFloor()
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
                            invoice.getGuestName(),
                            invoice.getGuestPhone(),
                            invoice.getInvoiceStatus(),
                            invoice.getPromotionType(),
                            invoice.getReservationNote(),
                            invoice.getFoodNote(),
                            tables
                    );
                })
                .collect(Collectors.toList());
    }

    public List<ReservationListResponse> findPendingConfirmationReservations() {
        List<Invoice> invoices = invoiceRepository.findPendingConfirmationReservations();
        
        return invoices.stream()
                .map(invoice -> {
                    Customer customer = invoice.getCustomer();
                    return new ReservationListResponse(
                            invoice.getId(),
                            invoice.getReservationCode(),
                            invoice.getReservedAt(),
                            invoice.getGuestCount(),
                            customer != null ? customer.getFullName() : "",
                            customer != null ? customer.getPhoneNumber() : "",
                            invoice.getGuestName(),
                            invoice.getGuestPhone(),
                            invoice.getInvoiceStatus(),
                            invoice.getPromotionType(),
                            invoice.getReservationNote(),
                            invoice.getFoodNote(),
                            List.of() // No tables assigned yet
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse confirmReservation(String reservationCode) {
        Invoice invoice = invoiceRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt bàn"));

        if (!"PENDING_CONFIRMATION".equals(invoice.getInvoiceStatus())) {
            throw new IllegalArgumentException("Chỉ có thể xác nhận đặt bàn có trạng thái PENDING_CONFIRMATION");
        }

        LocalDateTime reservedAt = invoice.getReservedAt();
        Integer guestCount = invoice.getGuestCount();

        // Get all available tables (no area filter - use full system)
        List<DiningTable> availableTables = getAvailableDiningTables(reservedAt, guestCount);

        if (availableTables.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Không có bàn trống nào cho %d khách vào thời gian %s. Vui lòng chọn thời gian khác hoặc liên hệ khách hàng.",
                    guestCount, reservedAt)
            );
        }

        // Use existing algorithm to select best tables across entire system
        List<DiningTable> selectedTables = selectTablesForGuests(availableTables, guestCount, reservedAt);

        if (selectedTables.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Không thể xếp bàn cho %d khách. Vui lòng liên hệ khách hàng để điều chỉnh số lượng hoặc thời gian.",
                    guestCount)
            );
        }

        // Lock tables
        List<Integer> tableIds = selectedTables.stream()
                .map(DiningTable::getId)
                .collect(Collectors.toList());
        diningTableRepository.lockTablesForReservation(tableIds);

        // Validate 120-minute gap
        validateReservationGap(selectedTables, reservedAt);

        // Assign tables
        for (DiningTable table : selectedTables) {
            InvoiceDiningTable idt = new InvoiceDiningTable();
            idt.setInvoice(invoice);
            idt.setDiningTable(table);
            invoiceDiningTableRepository.save(idt);
        }

        // Update invoice status to RESERVED
        invoice.setInvoiceStatus("RESERVED");
        invoice = invoiceRepository.save(invoice);

        // Broadcast RESERVED status via WebSocket
        tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "RESERVED");

        log.info("Confirmed reservation {} with {} tables automatically selected", 
                 reservationCode, selectedTables.size());

        Customer customer = invoice.getCustomer();
        List<ReservationResponse.TableInfo> tables = toTableInfos(selectedTables);
        return buildReservationResponse(invoice, customer, tables);
    }

    private ReservationResponse buildReservationResponse(
            Invoice invoice,
            Customer customer,
            List<ReservationResponse.TableInfo> tables
    ) {
        // LOGIC: Nếu có guestName/guestPhone (đặt bàn hộ) → dùng guest info
        //        Nếu không → dùng customer info (đặt cho chính mình)
        String fullName;
        String phoneNumber;
        
        if (invoice.getGuestName() != null && !invoice.getGuestName().trim().isEmpty()) {
            // Đặt bàn hộ - hiển thị thông tin người dùng bữa
            fullName = invoice.getGuestName();
            phoneNumber = invoice.getGuestPhone() != null ? invoice.getGuestPhone() : "";
        } else {
            // Đặt cho chính mình - hiển thị thông tin người đặt
            fullName = customer != null ? customer.getFullName() : "";
            phoneNumber = customer != null ? customer.getPhoneNumber() : "";
        }

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
                        table.getSeatingCapacity(),
                        table.getArea(),
                        table.getFloor()
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
     * Selects tables for guests using Area-Based Scoring System:
     * 
     * Priority scoring formula:
     * Score = +2000 (floor=1) + 1000 (floor=2) + 500 (area=A) + 300 (single table)
     *         - (max_id - min_id) * 5 - table_count * 30 - |excess_capacity| * 2
     * 
     * Process:
     * 1. Classify tables by freshness (no recent reservation within 120 minutes)
     * 2. Scan each area (A-F) and find best combo with highest score
     * 3. Select area with highest score
     * 4. Fallback: same floor → greedy → pending reservation
     */
    private List<DiningTable> selectTablesForGuests(List<DiningTable> tables, int guestCount, LocalDateTime reservedAt) {
        if (tables.isEmpty()) {
            return List.of();
        }

        // Step 1: Classify tables by freshness
        List<DiningTable> freshTables = new ArrayList<>();
        List<DiningTable> recentlyUsedTables = new ArrayList<>();
        
        for (DiningTable table : tables) {
            if (hasRecentReservation(table.getId(), reservedAt)) {
                recentlyUsedTables.add(table);
            } else {
                freshTables.add(table);
            }
        }
        
        // Try fresh tables first with area-based scoring
        List<DiningTable> selected = trySelectWithAreaScoring(freshTables, guestCount);
        if (!selected.isEmpty()) {
            return selected;
        }
        
        // Fallback to recently used tables with area-based scoring
        selected = trySelectWithAreaScoring(recentlyUsedTables, guestCount);
        if (!selected.isEmpty()) {
            return selected;
        }
        
        // Final fallback: try all tables together
        return trySelectFromTables(tables, guestCount);
    }
    
    /**
     * Attempts to select tables using area-based scoring system
     * Scans each area, calculates score for best combo, then selects highest scoring area
     */
    private List<DiningTable> trySelectWithAreaScoring(List<DiningTable> tables, int guestCount) {
        if (tables.isEmpty()) {
            return List.of();
        }

        // Group tables by area
        var tablesByArea = tables.stream()
                .filter(t -> t.getArea() != null && t.getFloor() != null)
                .collect(Collectors.groupingBy(
                    DiningTable::getArea,
                    Collectors.toList()
                ));
        
        // Scan each area and find best combo with score
        String bestArea = null;
        List<DiningTable> bestCombo = List.of();
        int bestScore = Integer.MIN_VALUE;
        
        for (var entry : tablesByArea.entrySet()) {
            String area = entry.getKey();
            List<DiningTable> areaTables = entry.getValue();
            areaTables.sort(Comparator.comparingInt(DiningTable::getId));
            
            // Find best combo in this area
            var result = findBestComboInArea(areaTables, guestCount);
            if (!result.isEmpty()) {
                int score = calculateAreaScore(result, guestCount);
                if (score > bestScore) {
                    bestScore = score;
                    bestCombo = result;
                    bestArea = area;
                }
            }
        }
        
        if (!bestCombo.isEmpty()) {
            log.info("Selected {} tables in area {} with score {} for {} guests", 
                     bestCombo.size(), bestArea, bestScore, guestCount);
            return bestCombo;
        }
        
        // Fallback: try same floor (any area)
        var tablesByFloor = tables.stream()
                .filter(t -> t.getFloor() != null)
                .collect(Collectors.groupingBy(
                    DiningTable::getFloor,
                    Collectors.toList()
                ));
        
        for (var entry : tablesByFloor.entrySet()) {
            Integer floor = entry.getKey();
            List<DiningTable> floorTables = entry.getValue();
            floorTables.sort(Comparator.comparingInt(DiningTable::getId));
            
            List<DiningTable> combo = trySelectFromTables(floorTables, guestCount);
            if (!combo.isEmpty()) {
                log.info("Selected {} tables on floor {} (fallback) for {} guests", 
                         combo.size(), floor, guestCount);
                return combo;
            }
        }
        
        return List.of();
    }
    
    /**
     * Finds best table combination in a single area
     * PRIORITY: Consecutive tables (A2+A3+A4) > Non-consecutive (A1+A3+A6)
     * Tries ALL combos (1, 2, 3, 4 tables) and returns the one with HIGHEST score
     */
    private List<DiningTable> findBestComboInArea(List<DiningTable> areaTables, int guestCount) {
        List<DiningTable> bestCombo = List.of();
        int bestScore = Integer.MIN_VALUE;
        
        // Try single table first
        for (DiningTable table : areaTables) {
            if (capacitySafe(table) >= guestCount) {
                int score = calculateAreaScore(List.of(table), guestCount);
                if (score > bestScore) {
                    bestScore = score;
                    bestCombo = List.of(table);
                }
            }
        }
        
        // Try combinations of 2-5 tables
        // PRIORITY: Try consecutive combos first (higher score due to lower ID distance)
        for (int k = 2; k <= Math.min(MAX_TABLES_TO_COMBINE, areaTables.size()); k++) {
            // Step 1: Try consecutive combos first
            List<DiningTable> consecutiveCombo = findBestConsecutiveCombo(areaTables, guestCount, k);
            if (!consecutiveCombo.isEmpty()) {
                int score = calculateAreaScore(consecutiveCombo, guestCount);
                if (score > bestScore) {
                    bestScore = score;
                    bestCombo = consecutiveCombo;
                }
            }
            
            // Step 2: Try all combos (including non-consecutive) as fallback
            List<DiningTable> anyCombo = findBestCombo(areaTables, guestCount, k);
            if (!anyCombo.isEmpty()) {
                int score = calculateAreaScore(anyCombo, guestCount);
                if (score > bestScore) {
                    bestScore = score;
                    bestCombo = anyCombo;
                }
            }
        }
        
        return bestCombo;
    }
    
    /**
     * Finds best CONSECUTIVE table combination (e.g., A2+A3+A4, not A1+A3+A6)
     * Only considers tables with consecutive IDs
     */
    private List<DiningTable> findBestConsecutiveCombo(List<DiningTable> areaTables, int guestCount, int k) {
        if (k > areaTables.size()) {
            return List.of();
        }
        
        List<DiningTable> bestCombo = List.of();
        int bestCapacity = Integer.MAX_VALUE;
        
        // Sliding window: try each consecutive sequence of k tables
        for (int i = 0; i <= areaTables.size() - k; i++) {
            List<DiningTable> window = areaTables.subList(i, i + k);
            
            // Check if truly consecutive (ID difference = 1 between adjacent tables)
            boolean isConsecutive = true;
            for (int j = 0; j < window.size() - 1; j++) {
                if (window.get(j + 1).getId() - window.get(j).getId() != 1) {
                    isConsecutive = false;
                    break;
                }
            }
            
            if (!isConsecutive) {
                continue;
            }
            
            // Check if capacity is sufficient
            int totalCapacity = window.stream().mapToInt(this::capacitySafe).sum();
            if (totalCapacity >= guestCount && totalCapacity < bestCapacity) {
                bestCapacity = totalCapacity;
                bestCombo = new ArrayList<>(window);
            }
        }
        
        return bestCombo;
    }
    
    /**
     * Calculates area score for a table combination
     * 
     * PRIORITY HIERARCHY:
     * 1. SINGLE AREA COMPLETENESS (+10000) - If one area can handle all guests, it ALWAYS wins
     * 2. Floor preference (+2000 floor 1, +1000 floor 2)
     * 3. Area A bonus (+500)
     * 4. Consecutive IDs (+100 per table)
     * 5. Single table (+300)
     * 6. Penalties: ID distance, table count, excess capacity
     */
    private int calculateAreaScore(List<DiningTable> tables, int guestCount) {
        if (tables.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        
        int score = 0;
        
        // CRITICAL: Single area completeness bonus - HIGHEST PRIORITY
        // If all tables are in the same area AND capacity is sufficient, this should always win
        boolean allSameArea = tables.stream()
                .map(DiningTable::getArea)
                .distinct()
                .count() == 1;
        
        int totalCapacity = tables.stream().mapToInt(this::capacitySafe).sum();
        
        if (allSameArea && totalCapacity >= guestCount) {
            score += SINGLE_AREA_COMPLETE_BONUS;
            log.debug("Single area bonus applied: +{} for area {}", SINGLE_AREA_COMPLETE_BONUS, tables.get(0).getArea());
        }
        
        // Floor bonus
        Integer floor = tables.get(0).getFloor();
        if (floor != null) {
            if (floor == 1) {
                score += FLOOR_1_BONUS;
            } else if (floor == 2) {
                score += FLOOR_2_BONUS;
            }
        }
        
        // Area A bonus
        String area = tables.get(0).getArea();
        if ("A".equals(area)) {
            score += AREA_A_BONUS;
        }
        
        // Consecutive ID bonus - reward tables with consecutive IDs (e.g., E1, E2, E3, E4, E5)
        if (tables.size() > 1) {
            List<Integer> ids = tables.stream()
                    .map(DiningTable::getId)
                    .sorted()
                    .collect(Collectors.toList());
            
            boolean allConsecutive = true;
            for (int i = 1; i < ids.size(); i++) {
                if (ids.get(i) != ids.get(i-1) + 1) {
                    allConsecutive = false;
                    break;
                }
            }
            
            if (allConsecutive) {
                score += CONSECUTIVE_ID_BONUS * tables.size();
                log.debug("Consecutive ID bonus applied: +{}", CONSECUTIVE_ID_BONUS * tables.size());
            }
        }
        
        // Single table bonus
        if (tables.size() == 1) {
            score += SINGLE_TABLE_BONUS;
        }
        
        // ID distance penalty (tables far apart)
        int minId = tables.stream().mapToInt(DiningTable::getId).min().orElse(0);
        int maxId = tables.stream().mapToInt(DiningTable::getId).max().orElse(0);
        score -= (maxId - minId) * ID_DISTANCE_PENALTY;
        
        // Table count penalty (prefer fewer tables)
        score -= tables.size() * TABLE_COUNT_PENALTY;
        
        // Excess capacity penalty (avoid wasting seats)
        int excessCapacity = Math.abs(totalCapacity - guestCount);
        score -= excessCapacity * EXCESS_CAPACITY_PENALTY;
        
        return score;
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
        validateReservationGap(selectedTables, newReservedAt, null);
    }

    private void validateReservationGap(List<DiningTable> selectedTables, LocalDateTime newReservedAt, Integer excludeInvoiceId) {
        LocalDateTime newEndTime = newReservedAt.plusMinutes(RESERVATION_DURATION_MINUTES);
        
        for (DiningTable table : selectedTables) {
            // Use optimized query instead of findAll()
            List<Invoice> existingReservations = invoiceRepository.findReservedReservationsByTableId(table.getId());

            for (Invoice existingReservation : existingReservations) {
                // Skip validation for the invoice being reassigned
                if (excludeInvoiceId != null && existingReservation.getId().equals(excludeInvoiceId)) {
                    continue;
                }
                
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

    /**
     * Get available tables for reassignment of a RESERVED reservation.
     * Returns all available tables that can accommodate the guest count.
     */
    public List<DiningTable> getAvailableTablesForReassignment(String reservationCode) {
        Invoice invoice = invoiceRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationCode));

        if (!"RESERVED".equals(invoice.getInvoiceStatus())) {
            throw new IllegalStateException("Can only get available tables for RESERVED reservations");
        }

        LocalDateTime reservedAt = invoice.getReservedAt();
        Integer guestCount = invoice.getGuestCount();

        // Get all available tables for this time slot
        List<DiningTable> availableTables = getAvailableDiningTables(reservedAt, guestCount);
        
        // Also include current assigned tables (so staff can deselect/reselect them)
        List<InvoiceDiningTable> currentAssignments = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
        List<Integer> currentTableIds = currentAssignments.stream()
                .map(idt -> idt.getDiningTable().getId())
                .collect(Collectors.toList());
        
        // Add current tables if not already in available list
        if (!currentTableIds.isEmpty()) {
            List<DiningTable> currentTables = diningTableRepository.findAllById(currentTableIds);
            for (DiningTable currentTable : currentTables) {
                if (availableTables.stream().noneMatch(t -> t.getId().equals(currentTable.getId()))) {
                    availableTables.add(currentTable);
                }
            }
        }
        
        return availableTables;
    }

    /**
     * Get recommended tables for a reservation based on guest count and optional area filter.
     * Returns the best table combination using the existing selection algorithm.
     */
    public List<DiningTable> getRecommendedTablesForReassignment(String reservationCode, String area) {
        Invoice invoice = invoiceRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationCode));

        if (!"RESERVED".equals(invoice.getInvoiceStatus())) {
            throw new IllegalStateException("Can only get recommended tables for RESERVED reservations");
        }

        LocalDateTime reservedAt = invoice.getReservedAt();
        Integer guestCount = invoice.getGuestCount();

        // Get all available tables
        List<DiningTable> availableTables = getAvailableDiningTables(reservedAt, guestCount);

        // Filter by area if specified
        if (area != null && !area.isEmpty()) {
            availableTables = availableTables.stream()
                    .filter(table -> area.equals(table.getArea()))
                    .collect(Collectors.toList());
        }

        if (availableTables.isEmpty()) {
            return List.of();
        }

        // Use existing algorithm to select best tables
        return selectTablesForGuests(availableTables, guestCount, reservedAt);
    }

    /**
     * Get alternative table options for a RESERVED reservation.
     * Returns multiple table combinations that can accommodate the guest count.
     */
    public List<List<DiningTable>> getAlternativeTablesForReservation(String reservationCode) {
        log.info("Getting alternative tables for reservation: {}", reservationCode);
        
        Invoice invoice = invoiceRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationCode));

        if (!"RESERVED".equals(invoice.getInvoiceStatus())) {
            throw new IllegalStateException("Can only get alternative tables for RESERVED reservations");
        }

        LocalDateTime reservedAt = invoice.getReservedAt();
        Integer guestCount = invoice.getGuestCount();
        
        log.info("Reservation details - reservedAt: {}, guestCount: {}", reservedAt, guestCount);

        // Get all available tables for this time slot
        List<DiningTable> availableTables = getAvailableDiningTables(reservedAt, guestCount);
        
        log.info("Found {} available tables", availableTables.size());

        if (availableTables.isEmpty()) {
            log.warn("No available tables found for reservation {}", reservationCode);
            return List.of();
        }

        // Generate multiple table combinations
        List<List<DiningTable>> alternatives = new ArrayList<>();

        // Try to get up to 3 different combinations
        List<DiningTable> option1 = selectTablesForGuests(availableTables, guestCount, reservedAt);
        if (!option1.isEmpty()) {
            alternatives.add(option1);
            log.info("Option 1: {} tables", option1.size());
            
            // Remove used tables and try again for option 2
            List<DiningTable> remainingTables = new ArrayList<>(availableTables);
            remainingTables.removeAll(option1);
            
            if (!remainingTables.isEmpty()) {
                List<DiningTable> option2 = selectTablesForGuests(remainingTables, guestCount, reservedAt);
                if (!option2.isEmpty()) {
                    alternatives.add(option2);
                    log.info("Option 2: {} tables", option2.size());
                    
                    // Try for option 3
                    remainingTables.removeAll(option2);
                    if (!remainingTables.isEmpty()) {
                        List<DiningTable> option3 = selectTablesForGuests(remainingTables, guestCount, reservedAt);
                        if (!option3.isEmpty()) {
                            alternatives.add(option3);
                            log.info("Option 3: {} tables", option3.size());
                        }
                    }
                }
            }
        }

        log.info("Returning {} alternative options for reservation {}", alternatives.size(), reservationCode);
        return alternatives;
    }

    /**
     * Reassign tables for a RESERVED reservation.
     * Removes old table assignments and creates new ones.
     */
    @Transactional
    public ReservationResponse reassignReservationTables(String reservationCode, List<Integer> newTableIds) {
        Invoice invoice = invoiceRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationCode));

        if (!"RESERVED".equals(invoice.getInvoiceStatus())) {
            throw new IllegalStateException("Can only reassign tables for RESERVED reservations");
        }

        if (newTableIds == null || newTableIds.isEmpty()) {
            throw new IllegalArgumentException("New table IDs cannot be empty");
        }

        // Validate new tables exist and are available
        List<DiningTable> newTables = new ArrayList<>();
        for (Integer tableId : newTableIds) {
            DiningTable table = diningTableRepository.findById(tableId)
                    .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
            
            if (!isTableServiceable(table)) {
                throw new IllegalStateException("Table " + table.getTableName() + " is not serviceable");
            }
            
            newTables.add(table);
        }

        // Validate no conflicts with new tables (exclude current invoice from validation)
        validateReservationGap(newTables, invoice.getReservedAt(), invoice.getId());

        // Remove old table assignments
        List<InvoiceDiningTable> oldLinks = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
        invoiceDiningTableRepository.deleteAll(oldLinks);

        // Create new table assignments
        for (DiningTable table : newTables) {
            InvoiceDiningTable link = new InvoiceDiningTable();
            link.setInvoice(invoice);
            link.setDiningTable(table);
            link.setCreatedAt(Instant.now());
            invoiceDiningTableRepository.save(link);
        }

        // Broadcast table status update via WebSocket
        tableStatusBroadcastService.broadcastTableStatusChange(newTableIds, "RESERVED");

        // Return updated reservation response
        Customer customer = invoice.getCustomer();
        return buildReservationResponse(invoice, customer, toTableInfos(newTables));
    }
}
