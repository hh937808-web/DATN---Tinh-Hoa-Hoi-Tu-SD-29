package com.example.datn_sd_29.walkin.service;

import com.example.datn_sd_29.common.service.TableStatusBroadcastService;
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
    private final TableStatusBroadcastService tableStatusBroadcastService;

    @Transactional
    public WalkInCheckInResponse checkInWalkIn(WalkInCheckInRequest request) {
        // Validate authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
            || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalArgumentException("Vui lòng đăng nhập lại tài khoản!");
        }

        String email = auth.getPrincipal().toString();
        
        // Get employee from authentication
        Employee employee = employeeRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

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

        // Cancel any existing IN_PROGRESS invoices for these tables
        for (DiningTable table : tables) {
            final Integer tableId = table.getId();
            
            List<Invoice> conflictingInvoices = invoiceRepository.findAll().stream()
                    .filter(inv -> "IN_PROGRESS".equals(inv.getInvoiceStatus()))
                    .filter(inv -> {
                        List<InvoiceDiningTable> invTables = 
                            invoiceDiningTableRepository.findByInvoiceIdWithTable(inv.getId());
                        return invTables.stream()
                            .anyMatch(idt -> idt.getDiningTable() != null && 
                                           idt.getDiningTable().getId().equals(tableId));
                    })
                    .collect(Collectors.toList());

            for (Invoice conflictingInvoice : conflictingInvoices) {
                conflictingInvoice.setInvoiceStatus("CANCELLED");
                invoiceRepository.save(conflictingInvoice);
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
}
