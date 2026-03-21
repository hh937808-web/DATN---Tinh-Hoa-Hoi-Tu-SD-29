package com.example.datn_sd_29.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    
    private String id;
    
    // User Identification
    private String userEmail;
    private String userRole;
    private Integer userId;
    private String userType;
    private String userFullName;
    
    // Action Details
    private String actionType;
    private String entityType;
    private String entityId;
    private String actionDescription;
    
    // Request Metadata
    private String ipAddress;
    private String userAgent;
    private String requestMethod;
    private String requestEndpoint;
    
    // Response Information
    private Integer responseStatus;
    private String responseMessage;
    
    // Timestamp
    private Instant createdAt;
    
    // Additional Context
    private String severity;
    
    // Performance Metrics
    private Integer executionTimeMs;
}
