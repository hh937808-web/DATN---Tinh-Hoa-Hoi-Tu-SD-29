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
    private String invoiceChannel; // WALK_IN or ONLINE

    private String customerType; // MEMBER | GUEST
    private String customerName;
    private String customerPhone;
    private Integer loyaltyPoints;
    private LocalDateTime reservedAt;
    private Instant checkedInAt;

    private Integer guestCount;
    private String staffName;       // NV phục vụ (servingStaff)
    private String receptionistName; // Lễ tân (employee)
    private List<TableSummary> tables;

    private List<PaymentItemResponse> items;
    private List<PaymentVoucherResponse> vouchers;

    private BigDecimal subtotal;
    private BigDecimal foodSubtotal;    // subtotal of non-drink items (VAT 8% base)
    private BigDecimal drinkSubtotal;   // subtotal of DRINK items (VAT 10% base)
    private BigDecimal itemVoucherDiscount;
    private BigDecimal vatPercent;       // food VAT rate (8%)
    private BigDecimal drinkVatPercent;  // drink VAT rate (10%)
    private BigDecimal serviceFeePercent;
    private Integer pointValue;
    private Integer maxPointsAllowed;  // giới hạn điểm được dùng cho hóa đơn này
    
    // Auto-applied invoice voucher info
    private Integer autoAppliedVoucherId;
    private String autoAppliedVoucherCode;
    private String autoAppliedVoucherName;
    private Integer autoAppliedVoucherPercent;
    private BigDecimal autoAppliedVoucherDiscount;
    
    // Pre-calculated totalPayable from backend (to ensure frontend and backend use same value)
    private BigDecimal totalPayable;

    @Getter
    @Setter
    public static class TableSummary {
        private Integer id;
        private String tableName;
        private Integer seatingCapacity;
    }
}
