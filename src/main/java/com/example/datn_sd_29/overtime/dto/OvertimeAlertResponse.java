package com.example.datn_sd_29.overtime.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for REST API response containing overtime alert information.
 * Timestamps are formatted as ISO 8601 strings for frontend consumption.
 */
@Data
@Builder
public class OvertimeAlertResponse {
    /**
     * Unique identifier for the alert
     */
    private String id;
    
    /**
     * List of tables involved in the overtime alert
     */
    private List<TableInfo> tables;
    
    /**
     * Duration of dining session in minutes
     */
    private Long diningDuration;
    
    /**
     * Next reservation time in ISO 8601 format
     */
    private String nextReservationTime;
    
    /**
     * Reference to the current invoice code
     */
    private String invoiceCode;
    
    /**
     * Urgency level (CRITICAL, HIGH, MEDIUM)
     */
    private String urgency;
    
    /**
     * Alert generation time in ISO 8601 format
     */
    private String generatedAt;
    
    /**
     * Nested class representing table information in the alert
     */
    @Data
    @Builder
    public static class TableInfo {
        /**
         * Table ID
         */
        private Integer id;
        
        /**
         * Table name
         */
        private String name;
        
        /**
         * Current table status
         */
        private String status;
    }
}
