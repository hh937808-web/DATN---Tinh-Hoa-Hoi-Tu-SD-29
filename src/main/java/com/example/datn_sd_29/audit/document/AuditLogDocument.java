package com.example.datn_sd_29.audit.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLogDocument {
    
    @Id
    private String id;
    
    // User Identification
    @Indexed
    private String userEmail;
    
    @Indexed
    private String userRole;
    
    private Integer userId;
    private String userType;
    private String userFullName;
    
    // Action Details
    @Indexed
    private String actionType;
    
    @Indexed
    private String entityType;
    
    @Indexed
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
    @Indexed(expireAfterSeconds = 7776000) // TTL: 90 days (90 * 24 * 60 * 60)
    private Instant createdAt;
    
    // Additional Context
    @Indexed
    private String severity;
    
    // Performance Metrics
    private Integer executionTimeMs;
}
