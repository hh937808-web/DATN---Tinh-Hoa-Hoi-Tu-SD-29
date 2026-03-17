package com.example.datn_sd_29.invoice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PaymentDetailResponse {
    private Integer invoiceId;
    private String invoiceCode;
    private String invoiceStatus;

    private String customerType; // MEMBER | GUEST
    private String customerName;
    private String customerPhone;
    private Integer loyaltyPoints;
    private LocalDateTime reservedAt;
    private Instant checkedInAt;

    private Integer guestCount;
    private String staffName;
    private List<TableSummary> tables;

    private List<PaymentItemResponse> items;
    private List<PaymentVoucherResponse> vouchers;

    private BigDecimal subtotal;
    private BigDecimal itemVoucherDiscount;
    private BigDecimal manualDiscountPercent;
    private BigDecimal manualDiscountAmount;
    private BigDecimal taxPercent;
    private BigDecimal serviceFeePercent;
    private Integer pointValue;

    @Getter
    @Setter
    public static class TableSummary {
        private Integer id;
        private String tableName;
        private Integer seatingCapacity;
    }
}
