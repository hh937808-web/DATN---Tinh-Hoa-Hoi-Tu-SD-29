package com.example.datn_sd_29.voucher.dto;

import com.example.datn_sd_29.voucher.entity.ProductVoucher;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;

import java.time.Instant;
import java.time.LocalDate;

public class ProductVoucherResponse {

    private Integer id;
    private String voucherCode;
    private String voucherName;
    private Integer discountPercent;
    private Integer productId;
    private String productName;
    private Integer remainingQuantity;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Column(name = "created_at")
    private Instant createdAt;


    public ProductVoucherResponse(ProductVoucher voucher) {
        this.id = voucher.getId();
        this.voucherCode = voucher.getVoucherCode();
        this.voucherName = voucher.getVoucherName();
        this.discountPercent = voucher.getDiscountPercent();
        this.productId = voucher.getProduct().getId();
        this.productName = voucher.getProduct().getProductName();
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

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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
