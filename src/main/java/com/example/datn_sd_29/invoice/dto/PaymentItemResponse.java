package com.example.datn_sd_29.invoice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentItemResponse {
    private Integer id;
    private String name;
    private String type; // PRODUCT | COMBO
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal lineTotal;
    private String voucherCode; // Voucher code applied to this item
    private Integer productId; // For matching with vouchers
    private Integer comboId; // For matching with vouchers
}
