package com.example.datn_sd_29.invoice.repository;

import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.entity.InvoiceItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem,Integer> {
    List<InvoiceItem> findByStatus(InvoiceItemStatus status);

    List<InvoiceItem> findByInvoiceId(Integer invoiceId);
}
