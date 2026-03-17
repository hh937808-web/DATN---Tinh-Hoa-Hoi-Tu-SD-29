package com.example.datn_sd_29.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TableOrderRequest {
    @Valid
    @NotEmpty
    private List<OrderItemRequest> items;
}
