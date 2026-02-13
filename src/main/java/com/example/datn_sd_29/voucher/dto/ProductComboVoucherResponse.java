package com.example.datn_sd_29.voucher.dto;

import com.example.datn_sd_29.voucher.entity.ProductComboVoucher;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDate;

public class ProductComboVoucherResponse {

    private Integer id;
    private String voucherCode;
    private String voucherName;
    private Integer discountPercent;
    private Integer productComboId;
    private String productComboName;
    private Integer remainingQuantity;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant createdAt;

    public ProductComboVoucherResponse(ProductComboVoucher voucher) {
        this.id = voucher.getId();
        this.voucherCode = voucher.getVoucherCode();
        this.voucherName = voucher.getVoucherName();
        this.discountPercent = voucher.getDiscountPercent();
        this.productComboId = voucher.getProductCombo().getId();
        this.productComboName = voucher.getProductCombo().getComboName();
        this.remainingQuantity = voucher.getRemainingQuantity();
        this.validFrom = voucher.getValidFrom();
        this.validTo = voucher.getValidTo();
        this.isActive = voucher.getIsActive();
        this.createdAt = voucher.getCreatedAt();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getProductComboId() {
        return productComboId;
    }

    public void setProductComboId(Integer productComboId) {
        this.productComboId = productComboId;
    }

    public String getProductComboName() {
        return productComboName;
    }

    public void setProductComboName(String productComboName) {
        this.productComboName = productComboName;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
