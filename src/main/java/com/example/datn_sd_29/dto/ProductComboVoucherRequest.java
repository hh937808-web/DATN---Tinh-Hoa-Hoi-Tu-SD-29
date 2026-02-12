package com.example.datn_sd_29.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class ProductComboVoucherRequest {

    @NotBlank(message = "Voucher code must not be blank")
    @Size(max = 8)
    private String voucherCode;

    @NotBlank(message = "Voucher name must not be blank")
    @Size(max = 50)
    private String voucherName;

    @NotNull
    @Min(1)
    @Max(100)
    private Integer discountPercent;

    @NotNull
    private Integer productComboId;

    @Min(0)
    private Integer remainingQuantity;

    private LocalDate validFrom;
    private LocalDate validTo;

    private Boolean isActive;


    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public String getVoucherName() { return voucherName; }
    public void setVoucherName(String voucherName) { this.voucherName = voucherName; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    public Integer getProductComboId() { return productComboId; }
    public void setProductComboId(Integer productComboId) { this.productComboId = productComboId; }

    public Integer getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(Integer remainingQuantity) { this.remainingQuantity = remainingQuantity; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}
