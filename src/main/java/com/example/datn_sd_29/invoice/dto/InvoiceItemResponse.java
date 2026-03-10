package com.example.datn_sd_29.invoice.dto;

import com.example.datn_sd_29.invoice.entity.InvoiceItemStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceItemResponse {
    private Integer id;

    private String itemName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal lineTotal;

    private InvoiceItemStatus status;
}
