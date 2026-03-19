package com.example.datn_sd_29.invoice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class PaymentCheckoutResponse {
    private Integer invoiceId;
    private String invoiceCode;
    private String invoiceStatus;
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal manualDiscount;
    private BigDecimal taxAmount;
    private BigDecimal serviceFeeAmount;
    private BigDecimal totalPayable;
    private BigDecimal cashReceived;
    private BigDecimal changeDue;
    private Instant paidAt;
}
