package com.example.datn_sd_29.audit.aspect;

import com.example.datn_sd_29.audit.annotation.Audited;
import com.example.datn_sd_29.audit.dto.AuditLogRequest;
import com.example.datn_sd_29.audit.service.AuditLogService;
import com.example.datn_sd_29.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.example.datn_sd_29.audit.annotation.Audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // Get method and annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Audited audited = method.getAnnotation(Audited.class);
        
        // Get HTTP request
        HttpServletRequest request = getCurrentHttpRequest();
        
        // Extract user information
        String userEmail = auditLogService.getCurrentUserEmail();
        String userRole = extractUserRole();
        
        // Build audit log request
        AuditLogRequest.AuditLogRequestBuilder auditBuilder = AuditLogRequest.builder()
                .userEmail(userEmail)
                .userRole(userRole)
                .actionType(audited.actionType())
                .entityType(audited.entityType().isEmpty() ? null : audited.entityType())
                .actionDescription(audited.description().isEmpty() 
                        ? generateDescription(audited.actionType(), audited.entityType()) 
                        : audited.description())
                .severity(audited.severity());
        
        // Add request metadata if available
        if (request != null) {
            auditBuilder
                    .ipAddress(auditLogService.extractIpAddress(request))
                    .userAgent(auditLogService.extractUserAgent(request))
                    .requestMethod(request.getMethod())
                    .requestEndpoint(request.getRequestURI());
        }
        
        Object result = null;
        Integer responseStatus = null;
        String responseMessage = null;
        
        try {
            // Execute the method
            result = joinPoint.proceed();
            
            // Extract response information
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                responseStatus = responseEntity.getStatusCode().value();
                responseMessage = "Thành công";
            } else {
                responseStatus = 200;
                responseMessage = "Thành công";
            }
            
        } catch (Exception e) {
            // Log error information
            responseStatus = 500;
            responseMessage = e.getMessage();
            auditBuilder.severity("ERROR");
            throw e;
        } finally {
            // Calculate execution time
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Complete audit log
            auditBuilder
                    .responseStatus(responseStatus)
                    .responseMessage(responseMessage)
                    .executionTimeMs((int) executionTime);
            
            // Save audit log asynchronously
            auditLogService.logAsync(auditBuilder.build());
        }
        
        return result;
    }

    /**
     * Get current HTTP request from RequestContextHolder
     */
    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract user role from JWT token or Spring Security context
     */
    private String extractUserRole() {
        try {
            // Try to get from Spring Security context first
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getAuthorities() != null) {
                return authentication.getAuthorities().stream()
                        .findFirst()
                        .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                        .orElse("UNKNOWN");
            }
            
            // Try to extract from JWT token
            HttpServletRequest request = getCurrentHttpRequest();
            if (request != null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    return jwtService.extractRole(token);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract user role: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    /**
     * Generate description from action type and entity type
     */
    private String generateDescription(String actionType, String entityType) {
        if (entityType == null || entityType.isEmpty()) {
            return actionType;
        }
        return actionType + " " + entityType;
    }
}
