package com.example.datn_sd_29.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReportResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<ProductSales> topByRevenue;
    private List<ProductSales> topByQuantity;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSales {
        private Integer productId;
        private String productName;
        private String category;
        private Integer quantitySold;
        private BigDecimal revenue;
        private Integer percentage;
    }
}
