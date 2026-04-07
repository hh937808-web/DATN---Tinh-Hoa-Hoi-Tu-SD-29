package com.example.datn_sd_29.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceGroupResponse {
    private Integer invoiceId;
    private String invoiceCode;
    private String customerName;
    private Integer guestCount;
    private BigDecimal subtotalAmount;
    private Instant checkedInAt;
    private List<TableInfo> tables;
    private Integer servingStaffId;
    private String servingStaffName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableInfo {
        private Integer tableId;
        private String tableName;
        private Integer floor;
        private String area;
    }
}
