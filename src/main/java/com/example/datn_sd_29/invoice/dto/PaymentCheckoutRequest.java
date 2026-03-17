package com.example.datn_sd_29.invoice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class PaymentCheckoutRequest {
    @NotNull
    private Integer tableId;

    private Integer customerVoucherId;

    @Min(0)
    private Integer usePoints;

    private String paymentMethod; // CASH | TRANSFER | EWALLET | CARD | MIXED

    private BigDecimal cashReceived;

    // Manual discount (optional)
    private BigDecimal manualDiscountPercent;
    private BigDecimal manualDiscountAmount;

    // VAT / Service fee (optional)
    private BigDecimal taxPercent;
    private BigDecimal serviceFeePercent;

    // Multi payment (optional). If present, will override paymentMethod/cashReceived.
    private List<PaymentLine> payments;

    @Getter
    @Setter
    public static class PaymentLine {
        private String method; // CASH | TRANSFER | EWALLET | CARD | VOUCHER
        private BigDecimal amount;
        private String note;
    }
}
