package com.example.datn_sd_29.service;

import com.example.datn_sd_29.dto.ReservationRequest;
import com.example.datn_sd_29.entity.*;
import com.example.datn_sd_29.event.ReservationCreatedEvent;
import com.example.datn_sd_29.repository.CustomerRepository;
import com.example.datn_sd_29.repository.DiningTableRepository;
import com.example.datn_sd_29.repository.EmployeeRepository;
import com.example.datn_sd_29.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@RequiredArgsConstructor
@Transactional
@Service
public class ReservationService {
    private final CustomerRepository customerRepository;
    private final DiningTableRepository diningTableRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EmployeeRepository employeeRepository;

    public void reserveTable(ReservationRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
            || "anonymousUser".equals(auth.getPrincipal())){
        throw new IllegalArgumentException("Vui lòng đăng nhập lại tài khoản!");
        }

        String email = auth.getPrincipal().toString();

        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng đăng nhập tài khoản!"));

        customer.setPhoneNumber(request.getPhoneNumber());
        customerRepository.save(customer);

        Invoice invoice = new Invoice();
        invoice.setCustomer( customer);

        Employee emp = employeeRepository.findById(1)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: 1"));
        invoice.setEmployee(emp);

        invoice.setInvoiceChannel("ONLINE");
        invoice.setInvoiceStatus("RESERVED");
        invoice.setReservationCode("RSV-" +System.currentTimeMillis());
        invoice.setInvoiceCode("INV-" + UUID.randomUUID());

        LocalDateTime reservedAt = request.getReservedAt();
        invoice.setReservedAt(reservedAt);

        Instant reservedAtInstant = reservedAt
                .atZone(ZoneId.systemDefault())
                .toInstant();

        invoice = invoiceRepository.save(invoice);

        for (Integer tableId : request.getDiningTableIds()) {
            DiningTable table = diningTableRepository.findById(tableId)
                    .orElseThrow(() -> new IllegalArgumentException("Not found table with id: " + tableId));
            if (!"AVAILABLE".equals(table.getTableStatus())) {
                throw new IllegalArgumentException("Bàn đã được đặt");
            }

            table.setTableStatus("RESERVED");
            diningTableRepository.save(table);

            InvoiceDiningTable idt = new InvoiceDiningTable();
            idt.setInvoice(invoice);
            idt.setDiningTable(table);

            invoiceDiningTableRepository.save(idt);
        }

        eventPublisher.publishEvent(
                new ReservationCreatedEvent(
                        customer.getEmail(),
                        invoice.getReservationCode(),
                        reservedAtInstant
                )
        );
    }
}
