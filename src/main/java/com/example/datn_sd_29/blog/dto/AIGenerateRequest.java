package com.example.datn_sd_29.blog.dto;

import lombok.Data;

@Data
public class AIGenerateRequest {
    private String title;
    private String summary;
    private String category;
}
