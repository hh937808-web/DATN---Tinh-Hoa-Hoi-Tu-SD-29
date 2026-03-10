package com.example.datn_sd_29.invoice.dto;

import lombok.Data;

@Data
public class InvoiceItemRequest {
    private Integer tableId;

    private String itemType; // PRODUCT / COMBO

    private Integer productId;

    private Integer productComboId;

    private Integer quantity;
}
