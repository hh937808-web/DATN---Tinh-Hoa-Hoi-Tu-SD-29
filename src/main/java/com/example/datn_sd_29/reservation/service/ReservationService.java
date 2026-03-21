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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private static final int RESERVATION_DURATION_MINUTES = 90;
    private static final int PRE_RESERVE_BUFFER_MINUTES = 30;
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
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
            || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalArgumentException("Vui lòng đăng nhập lại tài khoản!");
        }

        String email = auth.getPrincipal().toString();

        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng đăng nhập tài khoản!"));

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
        List<DiningTable> selectedTables = selectTablesForGuests(availableTables, guestCount);
        if (selectedTables.isEmpty()) {
            throw new IllegalArgumentException("Không còn bàn phù hợp trong thời gian đã chọn");
        }

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

        // Broadcast RESERVED status via WebSocket
        List<Integer> tableIds = selectedTables.stream()
                .map(DiningTable::getId)
                .collect(Collectors.toList());
        tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "RESERVED");

        List<ReservationResponse.TableInfo> tables = toTableInfos(selectedTables);
        return buildReservationResponse(invoice, customer, tables);
    }

    public ReservationResponse getReservationByCode(String reservationCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
            || "anonymousUser".equals(auth.getPrincipal())) {
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
            || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalArgumentException("Vui lòng đăng nhập lại tài khoản!");
        }

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

        // IMPORTANT: Handle table conflicts
        final Integer currentInvoiceId = invoice.getId();
        for (DiningTable table : tables) {
            final Integer tableId = table.getId();
            
            // 1. Cancel all existing IN_PROGRESS invoices for this table
            List<Invoice> inProgressInvoices = invoiceRepository.findAll().stream()
                    .filter(inv -> "IN_PROGRESS".equals(inv.getInvoiceStatus()))
                    .filter(inv -> !inv.getId().equals(currentInvoiceId))
                    .filter(inv -> {
                        List<InvoiceDiningTable> invTables = 
                            invoiceDiningTableRepository.findByInvoiceIdWithTable(inv.getId());
                        return invTables.stream()
                            .anyMatch(idt -> idt.getDiningTable() != null && 
                                           idt.getDiningTable().getId().equals(tableId));
                    })
                    .collect(Collectors.toList());

            for (Invoice existingInvoice : inProgressInvoices) {
                existingInvoice.setInvoiceStatus("CANCELLED");
                invoiceRepository.save(existingInvoice);
            }
            
            // 2. Remove this table from all other RESERVED invoices
            List<Invoice> reservedInvoices = invoiceRepository.findAll().stream()
                    .filter(inv -> "RESERVED".equals(inv.getInvoiceStatus()))
                    .filter(inv -> !inv.getId().equals(currentInvoiceId))
                    .filter(inv -> {
                        List<InvoiceDiningTable> invTables = 
                            invoiceDiningTableRepository.findByInvoiceIdWithTable(inv.getId());
                        return invTables.stream()
                            .anyMatch(idt -> idt.getDiningTable() != null && 
                                           idt.getDiningTable().getId().equals(tableId));
                    })
                    .collect(Collectors.toList());

            for (Invoice reservedInvoice : reservedInvoices) {
                // Remove this table from the reserved invoice
                List<InvoiceDiningTable> reservedTables = 
                    invoiceDiningTableRepository.findByInvoiceIdWithTable(reservedInvoice.getId());
                
                for (InvoiceDiningTable idt : reservedTables) {
                    if (idt.getDiningTable() != null && 
                        idt.getDiningTable().getId().equals(tableId)) {
                        invoiceDiningTableRepository.delete(idt);
                    }
                }
                
                // If the reserved invoice has no tables left, cancel it
                List<InvoiceDiningTable> remainingTables = 
                    invoiceDiningTableRepository.findByInvoiceIdWithTable(reservedInvoice.getId());
                if (remainingTables.isEmpty()) {
                    reservedInvoice.setInvoiceStatus("CANCELLED");
                    invoiceRepository.save(reservedInvoice);
                }
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
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
            || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalArgumentException("Vui lòng đăng nhập lại tài khoản!");
        }

        String email = auth.getPrincipal().toString();

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
                .filter(table -> !invoiceDiningTableRepository.existsOverlappingReservation(
                        table.getId(), ACTIVE_STATUSES, windowStart, windowEnd, RESERVATION_DURATION_MINUTES))
                .collect(Collectors.toList());
    }

    private List<DiningTable> selectTablesForGuests(List<DiningTable> tables, int guestCount) {
        if (tables.isEmpty()) {
            return List.of();
        }

        List<DiningTable> sorted = new ArrayList<>(tables);
        sorted.sort(Comparator.comparingInt(this::capacitySafe));

        for (DiningTable table : sorted) {
            if (capacitySafe(table) >= guestCount) {
                return List.of(table);
            }
        }

        int maxTables = Math.min(MAX_TABLES_TO_COMBINE, sorted.size());
        for (int k = 2; k <= maxTables; k++) {
            List<DiningTable> best = findBestCombo(sorted, guestCount, k);
            if (!best.isEmpty()) {
                return best;
            }
        }

        List<DiningTable> greedy = new ArrayList<>();
        int total = 0;
        for (int i = sorted.size() - 1; i >= 0 && total < guestCount; i--) {
            DiningTable t = sorted.get(i);
            greedy.add(t);
            total += capacitySafe(t);
        }

        return total >= guestCount ? greedy : List.of();
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
        for (DiningTable table : selectedTables) {
            // Query for existing RESERVED reservations for this table
            List<Invoice> existingReservations = invoiceRepository.findAll().stream()
                    .filter(inv -> "RESERVED".equals(inv.getInvoiceStatus()))
                    .filter(inv -> inv.getReservedAt() != null)
                    .filter(inv -> {
                        List<InvoiceDiningTable> invTables = 
                            invoiceDiningTableRepository.findByInvoiceIdWithTable(inv.getId());
                        return invTables.stream()
                            .anyMatch(idt -> idt.getDiningTable() != null && 
                                           idt.getDiningTable().getId().equals(table.getId()));
                    })
                    .collect(Collectors.toList());

            for (Invoice existingReservation : existingReservations) {
                LocalDateTime existingReservedAt = existingReservation.getReservedAt();
                // Calculate end time = reserved_at + 90 minutes (Requirement 6.2)
                LocalDateTime existingEndTime = existingReservedAt.plusMinutes(RESERVATION_DURATION_MINUTES);
                
                // Verify new reservation is at least 120 minutes after existing end time (Requirement 6.1)
                LocalDateTime minimumAllowedTime = existingEndTime.plusMinutes(120);
                
                if (newReservedAt.isBefore(minimumAllowedTime)) {
                    // Format descriptive error message (Requirement 6.3)
                    throw new IllegalArgumentException(
                        String.format("Cannot create reservation: table %s has a reservation ending at %s, minimum 120-minute gap required",
                            table.getTableName(),
                            existingEndTime.toString())
                    );
                }
            }
        }
    }
}
