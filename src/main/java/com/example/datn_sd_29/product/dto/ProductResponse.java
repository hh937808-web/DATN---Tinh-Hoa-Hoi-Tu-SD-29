package com.example.datn_sd_29.product.dto;

import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product.enums.ProductCategory;
import com.example.datn_sd_29.product.enums.ProductStatus;

import java.math.BigDecimal;

public class ProductResponse {
    private Integer id;
    private String productName;
    private ProductCategory productCategory;  // Chỉ có enum
    private BigDecimal unitPrice;
    private String description;
    private ProductStatus availabilityStatus;  // Chỉ có enum

    // Contructor từ entity (dễ maintain) (recommend cho các response khác)
    public ProductResponse(Product product) {
        this.id = product.getId();
        this.productName = product.getProductName();
        this.productCategory = product.getProductCategory();
        this.unitPrice = product.getUnitPrice();
        this.description = product.getDescription();
        this.availabilityStatus = product.getAvailabilityStatus();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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
