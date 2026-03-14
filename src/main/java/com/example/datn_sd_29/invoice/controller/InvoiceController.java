package com.example.datn_sd_29.invoice.controller;

import com.example.datn_sd_29.invoice.dto.UpdateInvoiceRequest;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    // SEARCH API
    @GetMapping("/search")
    public List<Invoice> searchInvoice(
            @RequestParam(required = false) String invoiceCode,
            @RequestParam(required = false) String reservationCode,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String invoiceType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod
    ) {
        return invoiceService.searchInvoice(
                invoiceCode,
                reservationCode,
                customerName,
                customerPhone,
                employeeName,
                invoiceType,
                status,
                paymentMethod
        );
    }

    // SORT API
    @GetMapping("/sort")
    public List<Invoice> sortInvoice(
            @RequestParam String sortBy,
            @RequestParam String direction
    ) {
        return invoiceService.sortInvoice(sortBy, direction);
    }

    //GetAll
    @GetMapping("/get-all")
    public List<Invoice> getAllInvoice() {
        return invoiceService.getAllInvoice();
    }

    //Update Invoice
    @PutMapping("/update/{invoiceId}")
    public Invoice updateInvoice(
            @PathVariable Integer invoiceId,
            @RequestBody UpdateInvoiceRequest request
    ) {
        return invoiceService.updateInvoice(invoiceId, request);

    }
}
