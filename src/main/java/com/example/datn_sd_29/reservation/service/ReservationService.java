package com.example.datn_sd_29.reservation.service;

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

    private static final int RESERVATION_DURATION_MINUTES = 90;
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

        invoice = invoiceRepository.save(invoice);

        for (DiningTable table : selectedTables) {
            InvoiceDiningTable idt = new InvoiceDiningTable();
            idt.setInvoice(invoice);
            idt.setDiningTable(table);
            invoiceDiningTableRepository.save(idt);
        }

        List<ReservationResponse.TableInfo> tables = selectedTables.stream()
                .map(table -> new ReservationResponse.TableInfo(
                        table.getId(),
                        "MB-" + table.getId(),
                        table.getTableName(),
                        table.getSeatingCapacity()
                ))
                .collect(Collectors.toList());

        return new ReservationResponse(
                invoice.getReservationCode(),
                invoice.getReservedAt(),
                invoice.getGuestCount(),
                customer.getFullName(),
                customer.getPhoneNumber(),
                invoice.getPromotionType(),
                invoice.getReservationNote(),
                tables
        );
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

    private List<DiningTable> getAvailableDiningTables(LocalDateTime reservedAt, Integer guestCount) {
        if (reservedAt == null) {
            throw new IllegalArgumentException("Reserved time is required");
        }
        if (guestCount == null || guestCount < 1) {
            throw new IllegalArgumentException("Guest count must be greater than 0");
        }

        LocalDateTime windowStart = reservedAt.minusMinutes(RESERVATION_DURATION_MINUTES);
        LocalDateTime windowEnd = reservedAt.plusMinutes(RESERVATION_DURATION_MINUTES);

        List<DiningTable> candidates = diningTableRepository
                .findBySeatingCapacityGreaterThanEqual(1);

        return candidates.stream()
                .filter(this::isTableServiceable)
                .filter(table -> !invoiceDiningTableRepository.existsOverlappingReservation(
                        table.getId(), ACTIVE_STATUSES, windowStart, windowEnd))
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
        return status == null || "AVAILABLE".equals(status);
    }

    private int capacitySafe(DiningTable table) {
        return table.getSeatingCapacity() == null ? 0 : table.getSeatingCapacity();
    }
}
