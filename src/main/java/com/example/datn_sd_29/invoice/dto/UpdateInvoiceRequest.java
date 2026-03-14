package com.example.datn_sd_29.invoice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter

public class UpdateInvoiceRequest {
    private BigDecimal discountAmount;
    private Integer usedPoints;
    private String paymentMethod;

}
