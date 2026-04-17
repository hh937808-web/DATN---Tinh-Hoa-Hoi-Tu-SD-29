package com.example.datn_sd_29.voucher.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class CustomerVoucherRequest {

    // Option 1: From template (personalVoucherId required)
    private Integer personalVoucherId;

    // Option 2: Direct creation (voucherCode, voucherName, discountPercent required)
    @Size(max = 8, message = "Mã voucher tối đa 8 ký tự")
    private String voucherCode;
    
    @Size(max = 50, message = "Tên voucher tối đa 50 ký tự")
    private String voucherName;
    
    @Min(value = 1, message = "Giảm giá phải >= 1%")
    @Max(value = 100, message = "Giảm giá không được vượt quá 100%")
    private Integer discountPercent;

    @Min(value = 0, message = "Số tiền tối thiểu phải >= 0")
    private java.math.BigDecimal minOrderAmount;

    // Customer ID is now optional - voucher can apply to all customers
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

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getVoucherName() {
        return voucherName;
    }

    public void setVoucherName(String voucherName) {
        this.voucherName = voucherName;
    }

    public Integer getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
    }

    public java.math.BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(java.math.BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
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
