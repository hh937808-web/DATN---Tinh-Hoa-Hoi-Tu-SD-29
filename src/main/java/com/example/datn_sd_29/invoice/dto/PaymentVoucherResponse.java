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
    private LocalDate expiresAt;
    private Integer remainingQuantity;
}
