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
        // Khi trace theo entity cụ thể → sort ASC (timeline chronological, dễ dựng lại câu chuyện)
        boolean isEntityTrace = searchRequest.getEntityType() != null && !searchRequest.getEntityType().isEmpty()
                && searchRequest.getEntityId() != null && !searchRequest.getEntityId().isEmpty();
        Sort.Direction direction = isEntityTrace
                ? Sort.Direction.ASC
                : ("DESC".equalsIgnoreCase(searchRequest.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC);
        Sort sort = Sort.by(direction, searchRequest.getSortBy());
        
        Pageable pageable = PageRequest.of(
                searchRequest.getPage(), 
                searchRequest.getSize(), 
                sort
        );

        // Build dynamic query using MongoTemplate for complex filtering
        Query query = new Query();
        
        // Keyword search — tìm từ khóa trong mô tả, tài khoản, tên đầy đủ, mã đối tượng
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().trim().isEmpty()) {
            String kw = searchRequest.getKeyword().trim();
            String escaped = java.util.regex.Pattern.quote(kw);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("actionDescription").regex(escaped, "i"),
                    Criteria.where("userEmail").regex(escaped, "i"),
                    Criteria.where("userFullName").regex(escaped, "i"),
                    Criteria.where("entityId").regex(escaped, "i")
            ));
        }

        if (searchRequest.getUserEmail() != null && !searchRequest.getUserEmail().isEmpty()) {
            query.addCriteria(Criteria.where("userEmail").is(searchRequest.getUserEmail()));
        }
        if (searchRequest.getUserRole() != null && !searchRequest.getUserRole().isEmpty()) {
            query.addCriteria(Criteria.where("userRole").is(searchRequest.getUserRole()));
        }
        if (searchRequest.getActionType() != null && !searchRequest.getActionType().isEmpty()) {
            query.addCriteria(Criteria.where("actionType").is(searchRequest.getActionType()));
        } else if (searchRequest.getActionCategory() != null && !searchRequest.getActionCategory().isEmpty()) {
            // Category filter — regex match trên actionType (vd: "CREATE" → mọi action có "CREATE" trong tên)
            String escaped = java.util.regex.Pattern.quote(searchRequest.getActionCategory());
            query.addCriteria(Criteria.where("actionType").regex(escaped));
        }
        if (searchRequest.getEntityType() != null && !searchRequest.getEntityType().isEmpty()) {
            query.addCriteria(Criteria.where("entityType").is(searchRequest.getEntityType()));
        }
        if (searchRequest.getEntityId() != null && !searchRequest.getEntityId().isEmpty()) {
            query.addCriteria(Criteria.where("entityId").is(searchRequest.getEntityId()));
        }
        if (searchRequest.getSeverity() != null && !searchRequest.getSeverity().isEmpty()) {
            query.addCriteria(Criteria.where("severity").is(searchRequest.getSeverity()));
        }
        // Gộp range createdAt vào 1 criteria — MongoDB không cho thêm 2 criteria cùng 1 field riêng lẻ
        if (searchRequest.getStartDate() != null || searchRequest.getEndDate() != null) {
            Criteria dateCriteria = Criteria.where("createdAt");
            if (searchRequest.getStartDate() != null) {
                dateCriteria = dateCriteria.gte(searchRequest.getStartDate());
            }
            if (searchRequest.getEndDate() != null) {
                dateCriteria = dateCriteria.lte(searchRequest.getEndDate());
            }
            query.addCriteria(dateCriteria);
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
        Instant now = Instant.now();
        Instant expiresAt = now.plus(computeRetention(request.getActionType()));

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
                .changes(request.getChanges())
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * Retention policy theo loại action — tuân thủ pháp luật & best practice:
     *  - Tài chính/hoá đơn: 10 năm (Luật Kế toán 88/2015 yêu cầu lưu chứng từ tối thiểu 10 năm)
     *  - Bảo mật/nhạy cảm : 1 năm (đăng nhập thất bại, đổi mật khẩu, vô hiệu hóa, xóa cứng)
     *  - Nghiệp vụ thường : 90 ngày (CRUD blog, product, table...)
     */
    private java.time.Duration computeRetention(String actionType) {
        if (actionType == null) return java.time.Duration.ofDays(90);

        // Tài chính — giữ 10 năm
        if (actionType.startsWith("PAYMENT_")
                || actionType.startsWith("INVOICE_")
                || actionType.startsWith("KITCHEN_ITEM_")
                || actionType.equals("INVOICE_ORDER_ADD_ITEMS")) {
            return java.time.Duration.ofDays(365L * 10);
        }

        // Bảo mật & hành vi nhạy cảm — giữ 1 năm
        if (actionType.equals("LOGIN_FAILED")
                || actionType.contains("PASSWORD")
                || actionType.endsWith("_HARD_DELETE")
                || actionType.endsWith("_DISABLE")
                || actionType.endsWith("_CANCEL")
                || actionType.equals("EMPLOYEE_DISABLE")
                || actionType.equals("CUSTOMER_STATUS_CHANGE")) {
            return java.time.Duration.ofDays(365);
        }

        // Mặc định
        return java.time.Duration.ofDays(90);
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
                .changes(auditLog.getChanges())
                .build();
    }
}
