package com.example.datn_sd_29.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableStatusResponse {
    private Integer id;
    private String name;
    private Integer capacity;
    private String status;
    private Long minutesSinceCheckIn; // For OCCUPIED/OVERTIME tables
    private Instant reservedAt; // For RESERVED tables
    private String customerName; // Customer name for OCCUPIED/RESERVED tables
}
