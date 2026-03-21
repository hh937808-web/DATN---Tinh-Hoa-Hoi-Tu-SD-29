package com.example.datn_sd_29.overtime.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result object for overtime detection execution.
 * Requirements: 7.4
 */
@Data
@Builder
public class OvertimeDetectionResult {
    private Integer tablesProcessed;
    private Integer overtimeDetected;
    private Integer alertsGenerated;
    private Long executionTimeMs;
    private List<String> errors;
}
