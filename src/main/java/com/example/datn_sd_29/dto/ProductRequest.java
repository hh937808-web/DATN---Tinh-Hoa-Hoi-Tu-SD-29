package com.example.datn_sd_29.dto;

import com.example.datn_sd_29.enums.ProductCategory;
import com.example.datn_sd_29.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

public class ProductRequest {
    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name max 100 characters")
    private String productName;

    @NotNull(message = "Product category is required")
    private ProductCategory productCategory;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal unitPrice;

    @Size(max = 300, message = "Description max 300 characters")
    private String description;

    @NotNull(message = "Availability status is required")
    private ProductStatus availabilityStatus;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public ProductCategory getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(ProductCategory productCategory) {
        this.productCategory = productCategory;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProductStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(ProductStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
}
