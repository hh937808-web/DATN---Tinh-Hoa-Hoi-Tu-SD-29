package com.example.datn_sd_29.voucher.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class ProductVoucherRequest {

    // NOTE: Validation removed for update compatibility
    // These fields are validated in create() method but not required for update()
    @Size(max = 8, message = "Mã voucher tối đa 8 ký tự")
    private String voucherCode;

    @Size(max = 50, message = "Tên voucher tối đa 50 ký tự")
    private String voucherName;

    @Min(value = 1, message = "Giảm giá phải >= 1%")
    @Max(value = 100, message = "Giảm giá không được vượt quá 100%")
    private Integer discountPercent;

    private Integer productId;

    @Min(value = 0, message = "Số lượng còn lại phải >= 0")
    private Integer remainingQuantity;

    private LocalDate validFrom;

    private LocalDate validTo;

    // ✅ THÊM isActive
    private Boolean isActive;

    // ===== GETTER SETTER =====

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

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(Integer remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
