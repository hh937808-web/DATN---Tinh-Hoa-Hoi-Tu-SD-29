package com.example.datn_sd_29.enums;

public enum ProductStatus {
    AVAILABLE("Còn hàng"), // Món ăn vẫn đang được bán
    OUT_OF_STOCK("Tạm hết hàng"), // Tạm hết món
    DISCONTINUED("Ngừng kinh doanh"); // Món ăn đã ngừng bán

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
