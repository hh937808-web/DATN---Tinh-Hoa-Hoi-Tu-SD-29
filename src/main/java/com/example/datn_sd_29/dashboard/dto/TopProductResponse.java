package com.example.datn_sd_29.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProductResponse {
    private Integer id;
    private String name;
    private String category;
    private Integer quantity;
    private BigDecimal revenue;
    private Integer percentage;
}
