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
public class AuditLogSearchRequest {
    
    private String userEmail;
    private String userRole;
    private String actionType;
    private String entityType;
    private String severity;
    private Instant startDate;
    private Instant endDate;
    
    // Pagination
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}
