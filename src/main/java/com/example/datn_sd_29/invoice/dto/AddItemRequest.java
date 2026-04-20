package com.example.datn_sd_29.invoice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddItemRequest {
    @NotNull(message = "tableId is required")
    private Integer tableId;

    private Integer productId;
    private Integer comboId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;
}
