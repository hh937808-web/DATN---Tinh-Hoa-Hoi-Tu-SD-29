package com.example.datn_sd_29.invoice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentItemResponse {
    private Integer id;
    private String code; // productCode or comboCode
    private String name;
    private String type; // PRODUCT | COMBO
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private Integer discountPercent; // Percentage of discount applied to this item
    private BigDecimal lineTotal;
    private String voucherCode; // Voucher code applied to this item
    private Integer productId; // For matching with vouchers
    private Integer comboId; // For matching with vouchers
    private String status; // ORDERED | IN_PROGRESS | DONE | SERVED | CANCELLED
}
