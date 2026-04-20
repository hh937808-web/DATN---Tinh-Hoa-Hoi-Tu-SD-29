package com.example.datn_sd_29.invoice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PaymentVoucherResponse {
    private Integer id;
    private String code;
    private String name;
    private Integer percent;
    private java.math.BigDecimal discountAmount; // actual discount amount in VND
    private LocalDate expiresAt;
    private Integer remainingQuantity;
    private String voucherStatus;
    private String voucherType; // CUSTOMER, PRODUCT, COMBO
    private Integer applicableItemId; // product_id or combo_id (for PRODUCT/COMBO vouchers)
    private String applicableItemName; // product_name or combo_name
}
