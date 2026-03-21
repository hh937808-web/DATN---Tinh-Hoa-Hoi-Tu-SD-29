package com.example.datn_sd_29.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private RevenueStats todayRevenue;
    private InvoiceStats todayInvoices;
    private CustomerStats todayCustomers;
    private TableStats activeTables;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueStats {
        private BigDecimal amount;
        private Double percentChange;
        private String trend;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceStats {
        private Integer count;
        private Double percentChange;
        private String trend;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerStats {
        private Integer count;
        private Double percentChange;
        private String trend;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableStats {
        private Integer occupied;
        private Integer total;
        private Integer percentOccupied;
    }
}
