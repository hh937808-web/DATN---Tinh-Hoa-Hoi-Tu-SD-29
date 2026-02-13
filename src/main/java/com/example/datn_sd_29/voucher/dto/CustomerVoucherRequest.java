package com.example.datn_sd_29.voucher.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class CustomerVoucherRequest {

    @NotNull(message = "Personal voucher id is required")
    private Integer personalVoucherId;

    @NotNull(message = "Customer id is required")
    private Integer customerId;

    private LocalDate issuedAt;

    private LocalDate expiresAt;

    @Min(value = 0, message = "Remaining quantity must be >= 0")
    private Integer remainingQuantity;

    private Boolean isActive;

    public Integer getPersonalVoucherId() {
        return personalVoucherId;
    }

    public void setPersonalVoucherId(Integer personalVoucherId) {
        this.personalVoucherId = personalVoucherId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public LocalDate getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDate issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(Integer remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
