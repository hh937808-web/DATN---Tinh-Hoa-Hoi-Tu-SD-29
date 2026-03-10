package com.example.datn_sd_29.invoice.controller;

import com.example.datn_sd_29.invoice.dto.InvoiceItemRequest;
import com.example.datn_sd_29.invoice.service.InvoiceItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoice-items")
public class InvoiceItemController {
    private final InvoiceItemService invoiceItemService;

    @Autowired
    public InvoiceItemController(InvoiceItemService invoiceItemService) {
        this.invoiceItemService = invoiceItemService;
    }

    /**
     * Thêm món vào hóa đơn
     */
    @PostMapping
    public ResponseEntity<?> createInvoiceItem(@RequestBody InvoiceItemRequest request) {

        invoiceItemService.orderItem(request);

        return ResponseEntity.ok("Add item to invoice successfully");
    }
}
