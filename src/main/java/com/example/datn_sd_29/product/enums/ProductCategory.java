package com.example.datn_sd_29.product.enums;

public enum ProductCategory {
    RAW_FOOD("Thực phẩm sống"),
    COOKED_FOOD("Món ăn chín"),
    HOT_POT_BROTH("Nước lẩu"),
    DRINK("Đồ uống"),
    DESSERT("Đồ tráng miệng");

    private final String description;
    ProductCategory(final String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
