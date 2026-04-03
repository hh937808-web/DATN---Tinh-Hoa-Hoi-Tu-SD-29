package com.example.datn_sd_29.voucher.dto;

import com.example.datn_sd_29.voucher.entity.PersonalVoucher;

public class PersonalVoucherResponse {

    private Integer id;
    private String voucherCode;
    private String voucherName;
    private Integer discountPercent;
    private String voucherType;

    public PersonalVoucherResponse(PersonalVoucher voucher) {
        this.id = voucher.getId();
        this.voucherCode = voucher.getVoucherCode();
        this.voucherName = voucher.getVoucherName();
        this.discountPercent = voucher.getDiscountPercent();
        this.voucherType = voucher.getVoucherType();
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
}
