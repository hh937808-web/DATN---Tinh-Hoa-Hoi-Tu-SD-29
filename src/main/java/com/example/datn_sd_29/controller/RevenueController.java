package com.example.datn_sd_29.controller;

import com.example.datn_sd_29.service.RevenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/revenue")
public class RevenueController {
    private final RevenueService revenueService;

    public RevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping("/month")
    public ResponseEntity<?> revenueByMonth() {
        return ResponseEntity.ok(revenueService.revenueByMonth());
    }

    @GetMapping("/year")
    public ResponseEntity<?> revenueByYear() {
        return ResponseEntity.ok(revenueService.revenueByYear());
    }

    @GetMapping("/invoice")
    public ResponseEntity<?> revenueByInvoice() {
        return ResponseEntity.ok(revenueService.revenueByInvoice());
    }
}
