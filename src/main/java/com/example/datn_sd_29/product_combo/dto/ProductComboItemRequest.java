package com.example.datn_sd_29.product_combo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ProductComboItemRequest {

    @NotNull(message = "ProductComboId không được để trống")
    private Integer productComboId;

    @NotNull(message = "ProductId không được để trống")
    private Integer productId;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    public Integer getProductComboId() {
        return productComboId;
    }

    public void setProductComboId(Integer productComboId) {
        this.productComboId = productComboId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
