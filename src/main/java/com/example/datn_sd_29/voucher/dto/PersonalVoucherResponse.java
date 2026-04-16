package com.example.datn_sd_29.voucher.dto;

import com.example.datn_sd_29.voucher.entity.PersonalVoucher;

import java.math.BigDecimal;

public class PersonalVoucherResponse {

    private Integer id;
    private String voucherCode;
    private String voucherName;
    private Integer discountPercent;
    private String voucherType;
    private BigDecimal minOrderAmount;

    public PersonalVoucherResponse(PersonalVoucher voucher) {
        this.id = voucher.getId();
        this.voucherCode = voucher.getVoucherCode();
        this.voucherName = voucher.getVoucherName();
        this.discountPercent = voucher.getDiscountPercent();
        this.voucherType = voucher.getVoucherType();
        this.minOrderAmount = voucher.getMinOrderAmount();
    }

    public Integer getId() {
        return id;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public String getVoucherName() {
        return voucherName;
    }

    public Integer getDiscountPercent() {
        return discountPercent;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }
}
