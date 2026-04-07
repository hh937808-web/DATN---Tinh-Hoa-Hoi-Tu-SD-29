package com.example.datn_sd_29.FeedBack.enums;


import lombok.Getter;

@Getter
public enum ReviewStatus {

    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Từ chối");

    private final String label;

    ReviewStatus(String label) {
        this.label = label;
    }
}
