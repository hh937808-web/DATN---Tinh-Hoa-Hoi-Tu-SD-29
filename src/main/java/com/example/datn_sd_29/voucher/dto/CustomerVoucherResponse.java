package com.example.datn_sd_29.voucher.dto;

import com.example.datn_sd_29.voucher.entity.CustomerVoucher;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDate;

public class CustomerVoucherResponse {

    private Integer id;

    private Integer personalVoucherId;
    private String voucherCode;
    private String voucherName;

    private Integer customerId;
    private String customerFullName;

    private LocalDate issuedAt;
    private LocalDate expiresAt;

    private Integer remainingQuantity;
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant createdAt;

    public CustomerVoucherResponse(CustomerVoucher voucher) {
        this.id = voucher.getId();

        this.personalVoucherId = voucher.getPersonalVoucher().getId();
        this.voucherCode = voucher.getPersonalVoucher().getVoucherCode();
        this.voucherName = voucher.getPersonalVoucher().getVoucherName();

        this.customerId = voucher.getCustomer().getId();
        this.customerFullName = voucher.getCustomer().getFullName();

        this.issuedAt = voucher.getIssuedAt();
        this.expiresAt = voucher.getExpiresAt();
        this.remainingQuantity = voucher.getRemainingQuantity();
        this.isActive = voucher.getIsActive();
        this.createdAt = voucher.getCreatedAt();
    }

    public Integer getId() {
        return id;
    }

    public Integer getPersonalVoucherId() {
        return personalVoucherId;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public String getVoucherName() {
        return voucherName;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public String getCustomerFullName() {
        return customerFullName;
    }

    public LocalDate getIssuedAt() {
        return issuedAt;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public Integer getRemainingQuantity() {
        return remainingQuantity;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
