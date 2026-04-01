package com.example.datn_sd_29.walkin.service;

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
import com.example.datn_sd_29.walkin.dto.WalkInCheckInRequest;
import com.example.datn_sd_29.walkin.dto.WalkInCheckInResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalkInService {
    
    private final InvoiceRepository invoiceRepository;
    private final DiningTableRepository diningTableRepository;
    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final TableStatusBroadcastService tableStatusBroadcastService;
    
    @org.springframework.beans.factory.annotation.Value("${security.api.enabled:true}")
    private boolean securityEnabled;

    @Transactional
    public WalkInCheckInResponse checkInWalkIn(WalkInCheckInRequest request) {
        // Validate authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Nếu security bị tắt, bỏ qua authentication check
        if (securityEnabled && (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
            || "anonymousUser".equals(auth.getPrincipal()))) {
            throw new IllegalArgumentException("Vui lòng đăng nhập lại tài khoản!");
        }

        // Get employee based on security mode
        Employee employee;
        if (securityEnabled) {
            // Production mode: lấy employee từ JWT token
            String email = auth.getPrincipal().toString();
            employee = employeeRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        } else {
            // Development mode: sử dụng employee mặc định (ID = 1)
            employee = employeeRepository.findById(1)
                    .orElseThrow(() -> new IllegalArgumentException("Default employee not found with id: 1"));
        }

        // Validate request
        if (request.getTableIds() == null || request.getTableIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn bàn");
        }

        if (request.getGuestCount() == null || request.getGuestCount() < 1) {
            throw new IllegalArgumentException("Số khách phải lớn hơn 0");
        }

        // Get tables
        List<DiningTable> tables = diningTableRepository.findAllById(request.getTableIds());
        if (tables.size() != request.getTableIds().size()) {
            throw new IllegalArgumentException("Một số bàn không tồn tại");
        }

        // Validate capacity: guest count must not exceed total table capacity
        int totalCapacity = tables.stream()
                .mapToInt(DiningTable::getSeatingCapacity)
                .sum();
        
        if (request.getGuestCount() > totalCapacity) {
            throw new IllegalArgumentException(
                String.format("Số khách (%d) vượt quá sức chứa của bàn (tối đa %d chỗ)", 
                    request.getGuestCount(), totalCapacity)
            );
        }

        // Check for conflicts BEFORE creating new invoice - fail explicitly if table in use
        for (DiningTable table : tables) {
            final Integer tableId = table.getId();
            
            List<Invoice> conflictingInvoices = invoiceDiningTableRepository
                    .findDistinctInvoicesByTableAndStatuses(
                            tableId,
                            List.of("IN_PROGRESS", "RESERVED")
                    );

            if (!conflictingInvoices.isEmpty()) {
                Invoice conflict = conflictingInvoices.get(0);
                throw new IllegalStateException(
                    String.format("Bàn %s đã có hóa đơn %s (trạng thái: %s). Vui lòng chọn bàn khác hoặc hủy hóa đơn hiện tại.",
                        table.getTableName(),
                        conflict.getInvoiceCode(),
                        conflict.getInvoiceStatus())
                );
            }
        }

        // Create new invoice
        Invoice invoice = new Invoice();
        invoice.setEmployee(employee);
        invoice.setInvoiceChannel("OFFLINE");
        invoice.setInvoiceStatus("IN_PROGRESS");
        invoice.setInvoiceCode("INV-" + UUID.randomUUID());
        invoice.setCheckedInAt(Instant.now());
        invoice.setGuestCount(request.getGuestCount());
        
        // Create temporary customer if customer name is provided
        if (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()) {
            Customer customer = new Customer();
            customer.setFullName(request.getCustomerName().trim());
            customer.setCreatedAt(Instant.now());
            customer.setIsActive(true);
            customer.setLoyaltyPoints(0);
            customer = customerRepository.save(customer);
            invoice.setCustomer(customer);
        }
        
        invoice = invoiceRepository.save(invoice);

        // Link tables to invoice
        for (DiningTable table : tables) {
            InvoiceDiningTable idt = new InvoiceDiningTable();
            idt.setInvoice(invoice);
            idt.setDiningTable(table);
            invoiceDiningTableRepository.save(idt);
        }

        // Update table status to IN_USE
        List<Integer> tableIds = tables.stream()
                .map(DiningTable::getId)
                .collect(Collectors.toList());
        
        if (!tableIds.isEmpty()) {
            diningTableRepository.updateTableStatusByIdIn(tableIds, "IN_USE");
            tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "IN_USE");
        }

        return new WalkInCheckInResponse(
            invoice.getId(),
            invoice.getInvoiceCode(),
            "Check-in thành công"
        );
    }

    @Transactional
    public void cancelTable(String invoiceCode) {
        // Find invoice by code
        Invoice invoice = invoiceRepository.findByInvoiceCode(invoiceCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));

        // Only allow cancelling IN_PROGRESS invoices
        if (!"IN_PROGRESS".equals(invoice.getInvoiceStatus())) {
            throw new IllegalArgumentException("Chỉ có thể hủy hóa đơn đang phục vụ");
        }

        // Get all tables linked to this invoice
        List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
        List<Integer> tableIds = invoiceTables.stream()
                .map(idt -> idt.getDiningTable().getId())
                .collect(Collectors.toList());

        // Cancel invoice
        invoice.setInvoiceStatus("CANCELLED");
        invoiceRepository.save(invoice);

        // Set tables back to AVAILABLE
        if (!tableIds.isEmpty()) {
            diningTableRepository.updateTableStatusByIdIn(tableIds, "AVAILABLE");
            tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "AVAILABLE");
        }
    }
}
