package com.example.datn_sd_29.audit.service;

import com.example.datn_sd_29.audit.document.AuditLogDocument;
import com.example.datn_sd_29.audit.dto.AuditLogRequest;
import com.example.datn_sd_29.audit.dto.AuditLogResponse;
import com.example.datn_sd_29.audit.dto.AuditLogSearchRequest;
import com.example.datn_sd_29.audit.repository.AuditLogMongoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMongoRepository auditLogMongoRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Create audit log entry asynchronously to avoid blocking main operations
     */
    @Async
    public void logAsync(AuditLogRequest request) {
        try {
            AuditLogDocument auditLog = buildAuditLog(request);
            auditLogMongoRepository.save(auditLog);
            log.debug("Audit log created: {} - {} - {}", 
                     request.getUserEmail(), request.getActionType(), request.getEntityType());
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Create audit log entry synchronously
     */
    public AuditLogDocument log(AuditLogRequest request) {
        try {
            AuditLogDocument auditLog = buildAuditLog(request);
            return auditLogMongoRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Helper method to create audit log for simple actions
     */
    @Async
    public void logAction(String userEmail, String userRole, String actionType, 
                         String entityType, String entityId, String description) {
        AuditLogRequest request = AuditLogRequest.builder()
                .userEmail(userEmail)
                .userRole(userRole)
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .actionDescription(description)
                .severity("INFO")
                .build();
        logAsync(request);
    }

    /**
     * Log authentication events
     */
    @Async
    public void logAuth(String userEmail, String actionType, boolean success, 
                       String ipAddress, String userAgent) {
        AuditLogRequest request = AuditLogRequest.builder()
                .userEmail(userEmail)
                .actionType(actionType)
                .actionDescription(success ? "Đăng nhập thành công" : "Đăng nhập thất bại")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .responseStatus(success ? 200 : 401)
                .severity(success ? "INFO" : "WARNING")
                .build();
        logAsync(request);
    }

    /**
     * Log critical security events
     */
    @Async
    public void logSecurityEvent(String userEmail, String actionType, String description, 
                                 String ipAddress, String userAgent) {
        AuditLogRequest request = AuditLogRequest.builder()
                .userEmail(userEmail)
                .actionType(actionType)
                .actionDescription(description)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .severity("CRITICAL")
                .build();
        logAsync(request);
    }

    /**
     * Search audit logs with filters
     */
    public Page<AuditLogResponse> searchAuditLogs(AuditLogSearchRequest searchRequest) {
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(searchRequest.getSortDirection()) 
                        ? Sort.Direction.DESC 
                        : Sort.Direction.ASC,
                searchRequest.getSortBy()
        );
        
        Pageable pageable = PageRequest.of(
                searchRequest.getPage(), 
                searchRequest.getSize(), 
                sort
        );

        // Build dynamic query using MongoTemplate for complex filtering
        Query query = new Query();
        
        if (searchRequest.getUserEmail() != null && !searchRequest.getUserEmail().isEmpty()) {
            query.addCriteria(Criteria.where("userEmail").is(searchRequest.getUserEmail()));
        }
        if (searchRequest.getUserRole() != null && !searchRequest.getUserRole().isEmpty()) {
            query.addCriteria(Criteria.where("userRole").is(searchRequest.getUserRole()));
        }
        if (searchRequest.getActionType() != null && !searchRequest.getActionType().isEmpty()) {
            query.addCriteria(Criteria.where("actionType").is(searchRequest.getActionType()));
        }
        if (searchRequest.getEntityType() != null && !searchRequest.getEntityType().isEmpty()) {
            query.addCriteria(Criteria.where("entityType").is(searchRequest.getEntityType()));
        }
        if (searchRequest.getSeverity() != null && !searchRequest.getSeverity().isEmpty()) {
            query.addCriteria(Criteria.where("severity").is(searchRequest.getSeverity()));
        }
        if (searchRequest.getStartDate() != null) {
            query.addCriteria(Criteria.where("createdAt").gte(searchRequest.getStartDate()));
        }
        if (searchRequest.getEndDate() != null) {
            query.addCriteria(Criteria.where("createdAt").lte(searchRequest.getEndDate()));
        }
        
        query.with(pageable);
        
        List<AuditLogDocument> auditLogs = mongoTemplate.find(query, AuditLogDocument.class);
        long count = mongoTemplate.count(query.skip(-1).limit(-1), AuditLogDocument.class);
        
        Page<AuditLogDocument> page = PageableExecutionUtils.getPage(
                auditLogs, 
                pageable, 
                () -> count
        );

        return page.map(this::toResponse);
    }

    /**
     * Get audit logs by entity
     */
    public List<AuditLogResponse> getAuditLogsByEntity(String entityType, String entityId) {
        List<AuditLogDocument> auditLogs = auditLogMongoRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId);
        return auditLogs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get recent actions by user
     */
    public List<AuditLogResponse> getRecentActionsByUser(String userEmail, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<AuditLogDocument> auditLogs = auditLogMongoRepository
                .findByUserEmailOrderByCreatedAtDesc(userEmail, pageable);
        return auditLogs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get failed login attempts since a specific time
     */
    public List<AuditLogResponse> getFailedLoginsSince(Instant since) {
        List<AuditLogDocument> auditLogs = auditLogMongoRepository.findFailedLoginsSince(since);
        return auditLogs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get critical events
     */
    public Page<AuditLogResponse> getCriticalEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogDocument> auditLogs = auditLogMongoRepository.findCriticalEvents(pageable);
        return auditLogs.map(this::toResponse);
    }

    /**
     * Extract IP address from HTTP request
     */
    public String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    /**
     * Extract user agent from HTTP request
     */
    public String extractUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * Get current authenticated user email
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }

    /**
     * Build AuditLog entity from request
     */
    private AuditLogDocument buildAuditLog(AuditLogRequest request) {
        return AuditLogDocument.builder()
                .userEmail(request.getUserEmail())
                .userRole(request.getUserRole())
                .userId(request.getUserId())
                .userType(request.getUserType())
                .userFullName(request.getUserFullName())
                .actionType(request.getActionType())
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .actionDescription(request.getActionDescription())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .requestMethod(request.getRequestMethod())
                .requestEndpoint(request.getRequestEndpoint())
                .responseStatus(request.getResponseStatus())
                .responseMessage(request.getResponseMessage())
                .severity(request.getSeverity() != null ? request.getSeverity() : "INFO")
                .executionTimeMs(request.getExecutionTimeMs())
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Convert AuditLog entity to response DTO
     */
    private AuditLogResponse toResponse(AuditLogDocument auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userEmail(auditLog.getUserEmail())
                .userRole(auditLog.getUserRole())
                .userId(auditLog.getUserId())
                .userType(auditLog.getUserType())
                .userFullName(auditLog.getUserFullName())
                .actionType(auditLog.getActionType())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .actionDescription(auditLog.getActionDescription())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .requestMethod(auditLog.getRequestMethod())
                .requestEndpoint(auditLog.getRequestEndpoint())
                .responseStatus(auditLog.getResponseStatus())
                .responseMessage(auditLog.getResponseMessage())
                .createdAt(auditLog.getCreatedAt())
                .severity(auditLog.getSeverity())
                .executionTimeMs(auditLog.getExecutionTimeMs())
                .build();
    }
}
