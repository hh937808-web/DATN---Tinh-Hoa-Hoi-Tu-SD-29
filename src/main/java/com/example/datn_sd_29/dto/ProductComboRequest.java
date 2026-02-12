package com.example.datn_sd_29.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductComboRequest {

    @NotBlank(message = "Tên combo không được để trống")
    @Size(max = 100, message = "Tên combo tối đa 100 ký tự")
    private String comboName;

    @Size(max = 300, message = "Mô tả tối đa 300 ký tự")
    private String description;

    @NotNull(message = "Giá combo không được để trống")
    private BigDecimal comboPrice;

    @NotNull(message = "Trạng thái isActive không được để trống")
    private Boolean isActive;

    public String getComboName() {
        return comboName;
    }

    public void setComboName(String comboName) {
        this.comboName = comboName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getComboPrice() {
        return comboPrice;
    }

    public void setComboPrice(BigDecimal comboPrice) {
        this.comboPrice = comboPrice;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
