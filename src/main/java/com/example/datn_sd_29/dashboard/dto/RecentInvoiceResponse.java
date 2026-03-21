package com.example.datn_sd_29.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentInvoiceResponse {
    private Integer id;
    private String code;
    private String table;
    private Instant time;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal serviceFee;
    private BigDecimal tax;
    private BigDecimal finalAmount;
    private String status;
    private String paymentMethod;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
}
