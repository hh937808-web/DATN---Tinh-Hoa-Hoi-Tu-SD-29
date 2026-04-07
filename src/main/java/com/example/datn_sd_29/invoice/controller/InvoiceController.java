package com.example.datn_sd_29.invoice.controller;

import com.example.datn_sd_29.invoice.dto.InvoiceDetailResponse;
import com.example.datn_sd_29.invoice.dto.InvoiceResponse;
import com.example.datn_sd_29.invoice.service.InvoiceService;
import com.example.datn_sd_29.security.JwtService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final JwtService jwtService;

    public InvoiceController(
            InvoiceService invoiceService,
            JwtService jwtService
    ) {
        this.invoiceService = invoiceService;
        this.jwtService = jwtService;
    }

    // =========================
    // LẤY HÓA ĐƠN CỦA CHÍNH MÌNH
    // =========================
    @GetMapping("/my")
    public List<InvoiceResponse> getMyInvoices(
            @RequestHeader("Authorization") String authHeader
    ) {

        String token = authHeader.substring(7); // bỏ "Bearer "

        Integer customerId = jwtService.extractCustomerId(token);

        return invoiceService.getByCustomerId(customerId);
    }

    @GetMapping("/{id}")
    public InvoiceDetailResponse getInvoiceDetail(@PathVariable Integer id) {
        return invoiceService.getInvoiceDetail(id);
    }
}