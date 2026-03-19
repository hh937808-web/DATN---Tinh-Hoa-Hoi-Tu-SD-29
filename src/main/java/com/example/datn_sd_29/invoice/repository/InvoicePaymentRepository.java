package com.example.datn_sd_29.invoice.repository;

import com.example.datn_sd_29.invoice.entity.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Integer> {
    List<InvoicePayment> findByInvoiceId(Integer invoiceId);
}
