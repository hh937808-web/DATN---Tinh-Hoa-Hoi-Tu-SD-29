package com.example.datn_sd_29.service;

import com.example.datn_sd_29.dto.ReservationRequest;
import com.example.datn_sd_29.entity.Customer;
import com.example.datn_sd_29.entity.DiningTable;
import com.example.datn_sd_29.entity.Employee;
import com.example.datn_sd_29.entity.Invoice;
import com.example.datn_sd_29.entity.InvoiceDiningTable;
import com.example.datn_sd_29.repository.CustomerRepository;
import com.example.datn_sd_29.repository.DiningTableRepository;
import com.example.datn_sd_29.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Service
public class ReservationService {
    private final CustomerRepository customerRepository;
    private final DiningTableRepository diningTableRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final EmailService emailService;

    public void reserveTable(ReservationRequest request) {

        // 1. Customer
        Customer customer = customerRepository
                .findByPhoneNumber(request.getPhoneNumber())
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.setFullName(request.getFullName());
                    c.setPhoneNumber(request.getPhoneNumber());
                    c.setEmail(request.getEmail());
                    c.setIsActive(true);
                    return customerRepository.save(c);
                });

        // 2. Invoice
        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);

        Employee emp = new Employee();
        emp.setId(1); // ID employee tồn tại
        invoice.setEmployee(emp);

        invoice.setInvoiceChannel("ONLINE");
        invoice.setInvoiceStatus("RESERVED");
        invoice.setReservationCode("RSV-" + System.currentTimeMillis());
        invoice.setReservedAt(request.getReservedAt());

        invoice = invoiceRepository.save(invoice);

        // 3. Gán bàn
        for (Integer tableId : request.getDiningTableIds()) {

            DiningTable table = diningTableRepository.findById(tableId)
                    .orElseThrow(() -> new RuntimeException("Bàn không tồn tại"));

            if (!"AVAILABLE".equals(table.getTableStatus())) {
                throw new RuntimeException("Bàn đã được đặt");
            }

            table.setTableStatus("RESERVED");
            diningTableRepository.save(table);

            InvoiceDiningTable idt = new InvoiceDiningTable();
            idt.setInvoice(invoice);
            idt.setDiningTable(table);

            invoiceDiningTableRepository.save(idt);
        }

        // 4. Gửi mail
        /*
        emailService.sendReservationEmail(
                customer.getEmail(),
                invoice.getReservationCode(),
                request.getReservedAt()
        );
        */
    }
}
