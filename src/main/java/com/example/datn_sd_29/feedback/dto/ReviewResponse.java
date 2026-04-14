package com.example.datn_sd_29.feedback.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private String name;
    private Integer rating;
    private String content;

    private String visitType;
    private String tip;

    private Integer serviceScore;
    private Integer foodScore;
    private Integer valueScore;
    private Integer atmosphereScore;

    private String avatarUrl;
    private LocalDateTime createdAt;

    private String status;       // PENDING / APPROVED / REJECTED
    private String statusLabel;  // Chờ duyệt / Đã duyệt / Từ chối
}