package com.example.datn_sd_29.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueChartResponse {
    private List<String> labels;
    private List<BigDecimal> data;
}
