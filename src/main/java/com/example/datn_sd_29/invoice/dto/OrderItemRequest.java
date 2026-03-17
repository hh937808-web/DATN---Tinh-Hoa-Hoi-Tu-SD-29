package com.example.datn_sd_29.invoice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {
    @NotBlank
    private String itemType; // PRODUCT | COMBO

    private Integer productId;

    private Integer productComboId;

    @Min(1)
    private Integer quantity;
}
