package com.example.datn_sd_29.dining_table.enums;

public enum DiningTableStatus {
    AVAILABLE("Bàn trống"),
    RESERVED("Đã đặt"),
    OCCUPIED("Đang sử dụng"),
    OUT_OF_SERVICE("Ngưng hoạt động"),
    CLEANING("Đang dọn dẹp");

    private final String description;

    DiningTableStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}