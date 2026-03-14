package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.invoice.dto.UpdateInvoiceRequest;
import com.example.datn_sd_29.invoice.entity.Invoice;

import java.util.List;

public interface InvoiceService {

    List<Invoice> searchInvoice(
            String invoiceCode,
            String reservationCode,
            String customerName,
            String customerPhone,
            String employeeName,
            String invoiceType,
            String status,
            String paymentMethod
    );

    List<Invoice> sortInvoice(String sortBy, String direction);

    List<Invoice> getAllInvoice();

    Invoice updateInvoice(Integer invoiceId, UpdateInvoiceRequest request);
}
