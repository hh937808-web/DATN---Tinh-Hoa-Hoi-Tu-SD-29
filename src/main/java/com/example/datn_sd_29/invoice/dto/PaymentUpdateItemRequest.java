package com.example.datn_sd_29.invoice.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentUpdateItemRequest {
    @Min(0)
    private Integer quantity;
}
