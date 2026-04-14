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
public class RevenueReportResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalRevenue;
    private Integer totalInvoices;
    private BigDecimal averageOrderValue;
    private BigDecimal cashRevenue;
    private BigDecimal transferRevenue;
    private List<DailyRevenue> dailyRevenues;
    private List<PaymentMethodBreakdown> paymentMethods;
    
    // Comparison with previous period
    private BigDecimal previousPeriodRevenue;
    private Double revenueGrowthPercentage;
    private Integer previousPeriodInvoices;
    private Double invoiceGrowthPercentage;
    
    // Peak hours analysis
    private List<HourlyRevenue> hourlyRevenues;
    
    // Category breakdown
    private List<CategoryBreakdown> categoryBreakdown;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private LocalDate date;
        private BigDecimal revenue;
        private Integer invoiceCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodBreakdown {
        private String method;
        private BigDecimal amount;
        private Integer count;
        private Double percentage;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyRevenue {
        private Integer hour;
        private BigDecimal revenue;
        private Integer invoiceCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private String category;
        private BigDecimal revenue;
        private Integer itemCount;
        private Double percentage;
    }
}
