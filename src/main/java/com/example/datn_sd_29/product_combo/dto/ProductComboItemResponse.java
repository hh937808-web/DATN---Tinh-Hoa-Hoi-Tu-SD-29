package com.example.datn_sd_29.product_combo.dto;

import com.example.datn_sd_29.product_combo.entity.ProductComboItem;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductComboItemResponse {

    private Integer id;

    private Integer productComboId;
    private String productComboName;

    private Integer productId;
    private String productName;

    private Integer quantity;

    public ProductComboItemResponse(ProductComboItem item) {
        this.id = item.getId();

        this.productComboId = item.getProductCombo().getId();
        this.productComboName = item.getProductCombo().getComboName();

        this.productId = item.getProduct().getId();
        this.productName = item.getProduct().getProductName();

        this.quantity = item.getQuantity();
    }
}

