package com.example.datn_sd_29.overtime.dto;

import com.example.datn_sd_29.overtime.enums.AlertUrgency;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an overtime alert for WebSocket transmission.
 * Requirements: 2.3, 8.1, 8.2, 8.3, 8.4, 8.5
 */
@Data
@Builder
public class OvertimeAlert {
    private String id;
    private List<String> tableNames;
    private List<Integer> tableIds;
    private Long diningDuration;
    private LocalDateTime nextReservationTime;
    private String invoiceCode;
    private Instant generatedAt;
    private AlertUrgency urgency;
}
