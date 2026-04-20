package com.example.datn_sd_29.querybuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private Long id;
    private String dashboardName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
