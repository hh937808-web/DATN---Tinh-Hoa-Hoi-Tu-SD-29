package com.example.datn_sd_29.audit.controller;

import com.example.datn_sd_29.audit.dto.AuditLogResponse;
import com.example.datn_sd_29.audit.dto.AuditLogSearchRequest;
import com.example.datn_sd_29.audit.service.AuditLogService;
import com.example.datn_sd_29.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Only ADMIN can access audit logs
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Search audit logs with filters
     * GET /api/audit-logs/search
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> searchAuditLogs(
            @RequestBody AuditLogSearchRequest searchRequest) {
        Page<AuditLogResponse> auditLogs = auditLogService.searchAuditLogs(searchRequest);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", auditLogs));
    }

    /**
     * Get audit logs by entity
     * GET /api/audit-logs/entity/{entityType}/{entityId}
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        List<AuditLogResponse> auditLogs = auditLogService.getAuditLogsByEntity(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success("Entity audit logs retrieved successfully", auditLogs));
    }

    /**
     * Get recent actions by user
     * GET /api/audit-logs/user/{userEmail}/recent?limit=10
     */
    @GetMapping("/user/{userEmail}/recent")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getRecentActionsByUser(
            @PathVariable String userEmail,
            @RequestParam(defaultValue = "10") int limit) {
        List<AuditLogResponse> auditLogs = auditLogService.getRecentActionsByUser(userEmail, limit);
        return ResponseEntity.ok(ApiResponse.success("Recent actions retrieved successfully", auditLogs));
    }

    /**
     * Get failed login attempts in last 24 hours
     * GET /api/audit-logs/failed-logins
     */
    @GetMapping("/failed-logins")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getFailedLogins(
            @RequestParam(defaultValue = "24") int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<AuditLogResponse> auditLogs = auditLogService.getFailedLoginsSince(since);
        return ResponseEntity.ok(ApiResponse.success("Failed logins retrieved successfully", auditLogs));
    }

    /**
     * Get critical security events
     * GET /api/audit-logs/critical?page=0&size=20
     */
    @GetMapping("/critical")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getCriticalEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLogResponse> auditLogs = auditLogService.getCriticalEvents(page, size);
        return ResponseEntity.ok(ApiResponse.success("Critical events retrieved successfully", auditLogs));
    }

    /**
     * Get audit logs for today
     * GET /api/audit-logs/today?page=0&size=20
     */
    @PostMapping("/today")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getTodayAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);
        
        AuditLogSearchRequest searchRequest = AuditLogSearchRequest.builder()
                .startDate(startOfDay)
                .endDate(endOfDay)
                .page(page)
                .size(size)
                .build();
        
        Page<AuditLogResponse> auditLogs = auditLogService.searchAuditLogs(searchRequest);
        return ResponseEntity.ok(ApiResponse.success("Today's audit logs retrieved successfully", auditLogs));
    }

    /**
     * Get audit logs by action type
     * GET /api/audit-logs/action/{actionType}?page=0&size=20
     */
    @GetMapping("/action/{actionType}")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogsByAction(
            @PathVariable String actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuditLogSearchRequest searchRequest = AuditLogSearchRequest.builder()
                .actionType(actionType)
                .page(page)
                .size(size)
                .build();
        
        Page<AuditLogResponse> auditLogs = auditLogService.searchAuditLogs(searchRequest);
        return ResponseEntity.ok(ApiResponse.success("Audit logs by action retrieved successfully", auditLogs));
    }

    /**
     * Get audit logs by user role
     * GET /api/audit-logs/role/{role}?page=0&size=20
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogsByRole(
            @PathVariable String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuditLogSearchRequest searchRequest = AuditLogSearchRequest.builder()
                .userRole(role)
                .page(page)
                .size(size)
                .build();
        
        Page<AuditLogResponse> auditLogs = auditLogService.searchAuditLogs(searchRequest);
        return ResponseEntity.ok(ApiResponse.success("Audit logs by role retrieved successfully", auditLogs));
    }
}
