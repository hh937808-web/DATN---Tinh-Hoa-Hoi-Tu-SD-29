package com.example.datn_sd_29.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemResponse {
    private Integer id;
    private String itemName;
    private Integer quantity;
    private BigDecimal price;
    private String status;
}
