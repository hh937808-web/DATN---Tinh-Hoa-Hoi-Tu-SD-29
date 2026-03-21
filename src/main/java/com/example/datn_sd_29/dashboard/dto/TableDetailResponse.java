package com.example.datn_sd_29.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDetailResponse {
    private Integer tableId;
    private String tableName;
    private Integer capacity;
    private String status;
    
    // Invoice info (null if table is AVAILABLE)
    private Integer invoiceId;
    private String invoiceCode;
    private String invoiceStatus;
    private Instant checkedInAt;
    private Instant reservedAt;
    private Integer guestCount;
    private String customerName;
    private String customerPhone;
    private BigDecimal subtotal;
    private BigDecimal finalAmount;
    private Long minutesSinceCheckIn;
    private String staffName;
}
