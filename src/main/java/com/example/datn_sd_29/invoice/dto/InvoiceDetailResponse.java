package com.example.datn_sd_29.invoice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InvoiceDetailResponse {

    private Integer id;
    private String code;
    private LocalDateTime time;

    private String customerName;
    private String phone;
    private String email;

    private List<String> tables; // 👈 thêm dòng này

    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal serviceFee;
    private BigDecimal tax;
    private BigDecimal finalAmount;

    private String status;
    private String paymentMethod;

    private List<Item> items;

    @Data
    public static class Item {
        private Integer id;
        private String name;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}