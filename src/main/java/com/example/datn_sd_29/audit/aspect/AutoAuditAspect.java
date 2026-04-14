package com.example.datn_sd_29.audit.aspect;

import com.example.datn_sd_29.audit.dto.AuditLogRequest;
import com.example.datn_sd_29.audit.service.AuditLogService;
import com.example.datn_sd_29.security.JwtService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * Automatic audit aspect that logs all REST controller methods
 * without requiring @Audited annotation
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AutoAuditAspect {

    private final AuditLogService auditLogService;
    private final JwtService jwtService;

    /**
     * Automatically audit all controller methods in specific packages
     * Excludes: GET requests (read operations), auth/login, audit-logs endpoints
     */
    @Around("execution(* com.example.datn_sd_29..controller.*.*(..)) " +
            "&& !execution(* com.example.datn_sd_29.audit.controller.*.*(..)) " +
            "&& (@annotation(org.springframework.web.bind.annotation.PostMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.PutMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.PatchMapping))")
    public Object autoAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // Get method information
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // Get HTTP request
        HttpServletRequest request = getCurrentHttpRequest();
        
        // Skip if no request context (e.g., async methods)
        if (request == null) {
            return joinPoint.proceed();
        }
        
        // Extract user information from JWT token
        String userEmail = null;
        String userRole = null;
        Integer userId = null;
        
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String subject = jwtService.extractEmail(token); // This is username for employees, email for customers
                userRole = jwtService.extractRole(token);
                userId = jwtService.extractCustomerId(token); // This is employeeId for employees, customerId for customers
                
                // For employees, subject is username, not email
                // For customers (USER role), subject is email
                if ("USER".equals(userRole)) {
                    userEmail = subject; // Customer email
                } else {
                    userEmail = subject; // Employee username (we'll use this as identifier)
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract user info from token: {}", e.getMessage());
        }
        
        // Skip if user is not authenticated
        if (userEmail == null || userEmail.isEmpty() || "anonymousUser".equals(userEmail)) {
            return joinPoint.proceed();
        }
        
        String userFullName = null; // Can be enhanced later to fetch from database
        
        // Determine action type and entity from method and request
        String actionType = determineActionType(method, request);
        String entityType = determineEntityType(request.getRequestURI());
        
        // Extract request body for detailed description
        Object requestBody = extractRequestBody(joinPoint);
        String description = generateDetailedDescription(actionType, entityType, request.getRequestURI(), requestBody, userRole);
        String severity = determineSeverity(actionType);
        
        // Build audit log request
        AuditLogRequest.AuditLogRequestBuilder auditBuilder = AuditLogRequest.builder()
                .userEmail(userEmail)
                .userRole(userRole)
                .userId(userId)
                .userFullName(userFullName)
                .actionType(actionType)
                .entityType(entityType)
                .actionDescription(description)
                .severity(severity)
                .ipAddress(auditLogService.extractIpAddress(request))
                .userAgent(auditLogService.extractUserAgent(request))
                .requestMethod(request.getMethod())
                .requestEndpoint(request.getRequestURI());
        
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
            try {
                auditLogService.logAsync(auditBuilder.build());
            } catch (Exception e) {
                log.error("Failed to save audit log: {}", e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Extract request body from method arguments
     */
    private Object extractRequestBody(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg != null && !arg.getClass().getName().startsWith("org.springframework")) {
                    // Skip Spring framework objects
                    return arg;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract request body: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Generate detailed description based on request body data
     */
    private String generateDetailedDescription(String actionType, String entityType, String uri, Object requestBody, String userRole) {
        try {
            // Reservation endpoints - extract booking details
            if (uri != null && uri.contains("/reservations") && "CREATE".equals(actionType) && requestBody != null) {
                return extractReservationDetails(requestBody, userRole);
            }
            
            // Payment/Checkout endpoints - extract payment details
            if (uri != null && (uri.contains("/checkout") || uri.contains("/payment")) && requestBody != null) {
                return extractPaymentDetails(requestBody);
            }
            
            // Order endpoints - extract order details
            if (uri != null && uri.contains("/orders") && "CREATE".equals(actionType) && requestBody != null) {
                return extractOrderDetails(requestBody);
            }
            
            // Walk-in endpoints
            if (uri != null && uri.contains("/walk-in") && "CREATE".equals(actionType) && requestBody != null) {
                return extractWalkInDetails(requestBody);
            }
            
            // Product endpoints
            if (uri != null && uri.contains("/products") && requestBody != null) {
                return extractProductDetails(requestBody, actionType);
            }
            
            // Combo endpoints
            if (uri != null && uri.contains("/combos") && requestBody != null) {
                return extractComboDetails(requestBody, actionType);
            }
            
            // Voucher endpoints
            if (uri != null && uri.contains("/vouchers") && requestBody != null) {
                return extractVoucherDetails(requestBody, actionType);
            }
            
            // Employee endpoints
            if (uri != null && uri.contains("/employees") && requestBody != null) {
                return extractEmployeeDetails(requestBody, actionType);
            }
            
            // Customer endpoints
            if (uri != null && uri.contains("/customers") && requestBody != null) {
                return extractCustomerDetails(requestBody, actionType);
            }
            
            // Table endpoints
            if (uri != null && uri.contains("/tables") && requestBody != null) {
                return extractTableDetails(requestBody, actionType);
            }
            
        } catch (Exception e) {
            log.debug("Failed to generate detailed description: {}", e.getMessage());
        }
        
        // Fallback to basic description
        return generateDescription(actionType, entityType, uri);
    }
    
    /**
     * Extract reservation booking details
     */
    private String extractReservationDetails(Object requestBody, String userRole) {
        try {
            // Extract all available fields
            Integer guestCount = safeInvokeMethod(requestBody, "getGuestCount", Integer.class);
            Object reservationDate = safeInvokeMethod(requestBody, "getReservationDate", Object.class);
            Object reservationTime = safeInvokeMethod(requestBody, "getReservationTime", Object.class);
            String customerName = safeInvokeMethod(requestBody, "getCustomerName", String.class);
            String phoneNumber = safeInvokeMethod(requestBody, "getPhoneNumber", String.class);
            String specialRequest = safeInvokeMethod(requestBody, "getSpecialRequest", String.class);
            
            StringBuilder description = new StringBuilder();
            
            // Role-based prefix
            if ("USER".equals(userRole)) {
                description.append("Khách hàng đặt bàn online");
            } else {
                description.append("Nhân viên tạo đặt bàn");
            }
            
            // Guest count
            if (guestCount != null && guestCount > 0) {
                description.append(" cho ").append(guestCount).append(" người");
            }
            
            // Date and time
            if (reservationTime != null && reservationDate != null) {
                description.append(" vào lúc ").append(reservationTime.toString())
                          .append(" ngày ").append(reservationDate.toString());
            }
            
            // Customer info
            if (customerName != null && !customerName.isEmpty()) {
                description.append(" - Tên: ").append(customerName);
            }
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                description.append(" - SĐT: ").append(phoneNumber);
            }
            
            // Special request
            if (specialRequest != null && !specialRequest.isEmpty()) {
                String truncated = specialRequest.length() > 50 
                    ? specialRequest.substring(0, 50) + "..." 
                    : specialRequest;
                description.append(" - Yêu cầu: ").append(truncated);
            }
            
            return description.toString();
        } catch (Exception e) {
            log.debug("Failed to extract reservation details: {}", e.getMessage());
            return "USER".equals(userRole) ? "Khách hàng đặt bàn online" : "Tạo mới đặt bàn";
        }
    }
    
    /**
     * Extract payment details
     */
    private String extractPaymentDetails(Object requestBody) {
        try {
            Integer invoiceId = safeInvokeMethod(requestBody, "getInvoiceId", Integer.class);
            Object totalAmount = safeInvokeMethod(requestBody, "getTotalAmount", Object.class);
            String paymentMethod = safeInvokeMethod(requestBody, "getPaymentMethod", String.class);
            Object discountAmount = safeInvokeMethod(requestBody, "getDiscountAmount", Object.class);
            
            StringBuilder description = new StringBuilder();
            description.append("Thanh toán hóa đơn");
            
            if (invoiceId != null) {
                description.append(" #").append(invoiceId);
            }
            
            if (totalAmount != null) {
                description.append(" - Tổng tiền: ").append(String.format("%,d", totalAmount)).append(" đ");
            }
            
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                String methodName = translatePaymentMethod(paymentMethod);
                description.append(" - Phương thức: ").append(methodName);
            }
            
            if (discountAmount != null) {
                long discount = ((Number) discountAmount).longValue();
                if (discount > 0) {
                    description.append(" - Giảm giá: ").append(String.format("%,d", discount)).append(" đ");
                }
            }
            
            return description.toString();
        } catch (Exception e) {
            log.debug("Failed to extract payment details: {}", e.getMessage());
            return "Thanh toán hóa đơn";
        }
    }
    
    /**
     * Translate payment method to Vietnamese
     */
    private String translatePaymentMethod(String method) {
        if (method == null) return "";
        switch (method.toUpperCase()) {
            case "CASH": return "Tiền mặt";
            case "BANK_TRANSFER": return "Chuyển khoản";
            case "CREDIT_CARD": return "Thẻ tín dụng";
            case "MOMO": return "MoMo";
            case "VNPAY": return "VNPay";
            default: return method;
        }
    }
    
    /**
     * Extract order details
     */
    private String extractOrderDetails(Object requestBody) {
        try {
            Integer tableId = safeInvokeMethod(requestBody, "getTableId", Integer.class);
            Object items = safeInvokeMethod(requestBody, "getItems", Object.class);
            String note = safeInvokeMethod(requestBody, "getNote", String.class);
            
            StringBuilder description = new StringBuilder();
            description.append("Tạo đơn hàng");
            
            if (tableId != null) {
                description.append(" cho bàn #").append(tableId);
            }
            
            // Count items if available
            if (items != null && items instanceof java.util.Collection) {
                int itemCount = ((java.util.Collection<?>) items).size();
                if (itemCount > 0) {
                    description.append(" - ").append(itemCount).append(" món");
                }
            }
            
            if (note != null && !note.isEmpty()) {
                String truncated = note.length() > 30 ? note.substring(0, 30) + "..." : note;
                description.append(" - Ghi chú: ").append(truncated);
            }
            
            return description.toString();
        } catch (Exception e) {
            log.debug("Failed to extract order details: {}", e.getMessage());
            return "Tạo đơn hàng";
        }
    }
    
    /**
     * Extract walk-in details
     */
    private String extractWalkInDetails(Object requestBody) {
        try {
            Integer guestCount = safeInvokeMethod(requestBody, "getGuestCount", Integer.class);
            String customerName = safeInvokeMethod(requestBody, "getCustomerName", String.class);
            String phoneNumber = safeInvokeMethod(requestBody, "getPhoneNumber", String.class);
            Integer tableId = safeInvokeMethod(requestBody, "getTableId", Integer.class);
            
            StringBuilder description = new StringBuilder();
            description.append("Tạo Walk-in");
            
            if (guestCount != null && guestCount > 0) {
                description.append(" cho ").append(guestCount).append(" người");
            }
            
            if (tableId != null) {
                description.append(" - Bàn #").append(tableId);
            }
            
            if (customerName != null && !customerName.isEmpty()) {
                description.append(" - Tên: ").append(customerName);
            }
            
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                description.append(" - SĐT: ").append(phoneNumber);
            }
            
            return description.toString();
        } catch (Exception e) {
            log.debug("Failed to extract walk-in details: {}", e.getMessage());
            return "Tạo mới Walk-in";
        }
    }
    
    /**
     * Safe method invocation helper
     */
    private <T> T safeInvokeMethod(Object obj, String methodName, Class<T> returnType) {
        try {
            Method method = obj.getClass().getMethod(methodName);
            Object result = method.invoke(obj);
            return returnType.cast(result);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extract product details
     */
    private String extractProductDetails(Object requestBody, String actionType) {
        try {
            String productName = safeInvokeMethod(requestBody, "getProductName", String.class);
            Object price = safeInvokeMethod(requestBody, "getPrice", Object.class);
            String category = safeInvokeMethod(requestBody, "getCategory", String.class);
            Boolean isActive = safeInvokeMethod(requestBody, "getIsActive", Boolean.class);
            
            StringBuilder description = new StringBuilder();
            
            if ("CREATE".equals(actionType)) {
                description.append("Tạo sản phẩm mới");
            } else if ("UPDATE".equals(actionType)) {
                description.append("Cập nhật sản phẩm");
            } else if ("DELETE".equals(actionType)) {
                description.append("Xóa sản phẩm");
            }
            
            if (productName != null && !productName.isEmpty()) {
                description.append(": ").append(productName);
            }
            
            if (price != null) {
                description.append(" - Giá: ").append(String.format("%,d", price)).append(" đ");
            }
            
            if (category != null && !category.isEmpty()) {
                description.append(" - Danh mục: ").append(category);
            }
            
            if (isActive != null && "UPDATE".equals(actionType)) {
                description.append(isActive ? " - Kích hoạt" : " - Vô hiệu hóa");
            }
            
            return description.toString();
        } catch (Exception e) {
            log.debug("Failed to extract product details: {}", e.getMessage());
            return actionType.equals("CREATE") ? "Tạo sản phẩm mới" : 
                   actionType.equals("UPDATE") ? "Cập nhật sản phẩm" : "Xóa sản phẩm";
        }
    }
    
    /**
     * Extract combo details
     */
    private String extractComboDetails(Object requestBody, String actionType) {
        try {
            String comboName = safeInvokeMethod(requestBody, "getComboName", String.class);
            Object price = safeInvokeMethod(requestBody, "getPrice", Object.class);
            Object discountPercent = safeInvokeMethod(requestBody, "getDiscountPercent", Object.class);
            Boolean isActive = safeInvokeMethod(requestBody, "getIsActive", Boolean.class);
            
            StringBuilder description = new StringBuilder();
            
            if ("CREATE".equals(actionType)) {
                description.append("Tạo combo mới");
            } else if ("UPDATE".equals(actionType)) {
                description.append("Cập nhật combo");
            } else if ("DELETE".equals(actionType)) {
                description.append("Xóa combo");
            }
            
            if (comboName != null && !comboName.isEmpty()) {
                description.append(": ").append(comboName);
            }
            
            if (price != null) {
                description.append(" - Giá: ").append(String.format("%,d", price)).append(" đ");
            }
            
            if (discountPercent != null) {
                description.append(" - Giảm giá: ").append(discountPercent).append("%");
            }
            
            if (isActive != null && "UPDATE".equals(actionType)) {
                description.append(isActive ? " - Kích hoạt" : " - Vô hiệu hóa");
            }
            
            return description.toString();
        } catch (Exception e) {
            log.debug("Failed to extract combo details: {}", e.getMessage());
            return actionType.equals("CREATE") ? "Tạo combo mới" : 
                   actionType.equals("UPDATE") ? "Cập nhật combo" : "Xóa combo";
        }
    }
    
    /**
     * Extract voucher details
     */
    private String extractVoucherDetails(Object requestBody, String actionType) {
        try {
            String voucherCode = safeInvokeMethod(requestBody, "getVoucherCode", String.class);
            String voucherName = safeInvokeMethod(requestBody, "getVoucherName", String.class);
            Object discountValue = safeInvokeMethod(requestBody, "getDiscountValue", Object.class);
            String discountType = safeInvokeMethod(requestBody, "getDiscountType", String.class);
            Object maxUsage = safeInvokeMethod(requestBody, "getMaxUsage", Object.class);
            Boolean isActive = safeInvokeMethod(requestBody, "getIsActive", Boolean.class);
            
            StringBuilder description = new StringBuilder();
            
            if ("CREATE".equals(actionType)) {
                description.append("Tạo voucher mới");
            } else if ("UPDATE".equals(actionType)) {
                description.append("Cập nhật voucher");
            } else if ("DELETE".equals(actionType)) {
                description.append("Xóa voucher");
            }
            
            if (voucherCode != null && !voucherCode.isEmpty()) {
                description.append(": ").append(voucherCode);
            }
            
            if (voucherName != null && !voucherName.isEmpty()) {
                description.append(" (").append(voucherName).append(")");
            }
            
            if (discountValue != null && discountType != null) {
                if ("PERCENTAGE".equals(discountType)) {
                    description.append(" - Giảm ").append(discountValue).append("%");
                } else {
                    description.append(" - Giảm ").append(String.format("%,d", discountValue)).append(" đ");
                }
            }
            
            if (maxUsage != null) {
                description.append(" - Giới hạn: ").append(maxUsage).append(" lần");
            }
            
            if (isActive != null && "UPDATE".equals(actionType)) {
                description.append(isActive ? " - Kích hoạt" : " - Vô hiệu hóa");
            }
            
            return description.toString();
        } catch (Exception e) {
            log.debug("Failed to extract voucher details: {}", e.getMessage());
            return actionType.equals("CREATE") ? "Tạo voucher mới" : 
                   actionType.equals("UPDATE") ? "Cập nhật voucher" : "Xóa voucher";
        }
    }
    
    /**
     * Extract employee details
     */
    private String extractEmployeeDetails(Object requestBody, String actionType) {
        try {
            String fullName = safeInvokeMethod(requestBody, "getFullName", String.class);
            String username = safeInvokeMethod(requestBody, "getUsername", String.class);
            String email = safeInvokeMethod(requestBody, "getEmail", String.class);
            String role = safeInvokeMethod(requestBody, "getRole", String.class);
            String phoneNumber = safeInvokeMethod(requestBody, "getPhoneNumber", String.class);
            Boolean isActive = safeInvokeMethod(requestBody, "getIsActive", Boolean.class);
            
            StringBuilder description = new StringBuilder();
            
            if ("CREATE".equals(actionType)) {
                description.append("Tạo nhân viên mới");
            } else if ("UPDATE".equals(actionType)) {
                description.append("Cập nhật nhân viên");
            } else if ("DELETE".equals(actionType)) {
                description.append("Xóa nhân viên");
            }
            
            if (fullName != null && !fullName.isEmpty()) {
                description.append(": ").append(fullName);
            } else if (username != null && !username.isEmpty()) {
                description.append(": ").append(username);
            }
            
            if (role != null && !role.isEmpty()) {
                String roleVN = translateRole(role);
                description.append(" - Vai trò: ").append(roleVN);
            }
            
            if (email != null && !email.isEmpty()) {
                description.append(" - Email: ").append(email);
            }
            
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                description.append(" - SĐT: ").append(phoneNumber);
            }
            
            if (isActive != null && "UPDATE".equals(actionType)) {
                description.append(isActive ? " - Kích hoạt" : " - Vô hiệu hóa");
            }
            
            return description.toString();
        } catch (Exception e) {
            log.debug("Failed to extract employee details: {}", e.getMessage());
            return actionType.equals("CREATE") ? "Tạo nhân viên mới" : 
                   actionType.equals("UPDATE") ? "Cập nhật nhân viên" : "Xóa nhân viên";
        }
    }
    
    /**
     * Extract customer details
     */
    private String extractCustomerDetails(Object requestBody, String actionType) {
        try {
            String fullName = safeInvokeMethod(requestBody, "getFullName", String.class);
            String email = safeInvokeMethod(requestBody, "getEmail", String.class);
            String phoneNumber = safeInvokeMethod(requestBody, "getPhoneNumber", String.class);
            String address = safeInvokeMethod(requestBody, "getAddress", String.class);
            
            StringBuilder description = new StringBuilder();
            
            if ("CREATE".equals(actionType)) {
                description.append("Tạo khách hàng mới");
            } else if ("UPDATE".equals(actionType)) {
                description.append("Cập nhật khách hàng");
            }
            
            if (fullName != null && !fullName.isEmpty()) {
                description.append(": ").append(fullName);
            }
            
            if (email != null && !email.isEmpty()) {
                description.append(" - Email: ").append(email);
            }
            
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                description.append(" - SĐT: ").append(phoneNumber);
            }
            
            if (address != null && !address.isEmpty()) {
                String truncated = address.length() > 30 ? address.substring(0, 30) + "..." : address;
                description.append(" - Địa chỉ: ").append(truncated);
            }
            
            return description.toString();
        } catch (Exception e) {
            log.debug("Failed to extract customer details: {}", e.getMessage());
            return actionType.equals("CREATE") ? "Tạo khách hàng mới" : "Cập nhật khách hàng";
        }
    }
    
    /**
     * Extract table details
     */
    private String extractTableDetails(Object requestBody, String actionType) {
        try {
            String tableName = safeInvokeMethod(requestBody, "getTableName", String.class);
            Integer capacity = safeInvokeMethod(requestBody, "getCapacity", Integer.class);
            String status = safeInvokeMethod(requestBody, "getStatus", String.class);
            String location = safeInvokeMethod(requestBody, "getLocation", String.class);
            
            StringBuilder description = new StringBuilder();
            
            if ("CREATE".equals(actionType)) {
                description.append("Tạo bàn ăn mới");
            } else if ("UPDATE".equals(actionType)) {
                description.append("Cập nhật bàn ăn");
            } else if ("DELETE".equals(actionType)) {
                description.append("Xóa bàn ăn");
            }
            
            if (tableName != null && !tableName.isEmpty()) {
                description.append(": ").append(tableName);
            }
            
            if (capacity != null) {
                description.append(" - Sức chứa: ").append(capacity).append(" người");
            }
            
            if (status != null && !status.isEmpty()) {
                String statusVN = translateTableStatus(status);
                description.append(" - Trạng thái: ").append(statusVN);
            }
            
            if (location != null && !location.isEmpty()) {
                description.append(" - Vị trí: ").append(location);
            }
            
            return description.toString();
        } catch (Exception e) {
            log.debug("Failed to extract table details: {}", e.getMessage());
            return actionType.equals("CREATE") ? "Tạo bàn ăn mới" : 
                   actionType.equals("UPDATE") ? "Cập nhật bàn ăn" : "Xóa bàn ăn";
        }
    }
    
    /**
     * Translate role to Vietnamese
     */
    private String translateRole(String role) {
        if (role == null) return "";
        switch (role.toUpperCase()) {
            case "ADMIN": return "Quản trị viên";
            case "STAFF": return "Nhân viên";
            case "RECEPTION": return "Lễ tân";
            case "KITCHEN": return "Bếp";
            case "USER": return "Khách hàng";
            default: return role;
        }
    }
    
    /**
     * Translate table status to Vietnamese
     */
    private String translateTableStatus(String status) {
        if (status == null) return "";
        switch (status.toUpperCase()) {
            case "AVAILABLE": return "Trống";
            case "OCCUPIED": return "Đang sử dụng";
            case "RESERVED": return "Đã đặt";
            case "CLEANING": return "Đang dọn dẹp";
            case "MAINTENANCE": return "Bảo trì";
            default: return status;
        }
    }

    /**
     * Special handling for login endpoints
     */
    @Around("execution(* com.example.datn_sd_29.auth.controller.LoginController.login(..))")
    public Object auditLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getCurrentHttpRequest();
        
        // Extract email from request body if possible
        String userEmail = "unknown";
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] != null) {
                // Try to extract email from LoginRequest
                Method getEmailMethod = args[0].getClass().getMethod("getEmail");
                userEmail = (String) getEmailMethod.invoke(args[0]);
            }
        } catch (Exception e) {
            log.debug("Could not extract email from login request");
        }
        
        AuditLogRequest.AuditLogRequestBuilder auditBuilder = AuditLogRequest.builder()
                .userEmail(userEmail)
                .userRole("UNKNOWN")
                .actionType("LOGIN")
                .entityType("")
                .actionDescription("Đăng nhập hệ thống")
                .severity("INFO");
        
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
            result = joinPoint.proceed();
            
            // Extract role and userId from successful login response
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                responseStatus = responseEntity.getStatusCode().value();
                responseMessage = "Đăng nhập thành công";
                
                // Extract role and userId from ApiResponse<LoginResponse>
                try {
                    Object body = responseEntity.getBody();
                    if (body != null) {
                        // Get data from ApiResponse
                        Method getDataMethod = body.getClass().getMethod("getData");
                        Object loginResponse = getDataMethod.invoke(body);
                        
                        if (loginResponse != null) {
                            Method getRoleMethod = loginResponse.getClass().getMethod("getRole");
                            Method getUserIdMethod = loginResponse.getClass().getMethod("getUserId");
                            
                            String role = (String) getRoleMethod.invoke(loginResponse);
                            Integer userId = (Integer) getUserIdMethod.invoke(loginResponse);
                            
                            auditBuilder.userRole(role != null ? role : "UNKNOWN");
                            auditBuilder.userId(userId);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not extract role/userId from login response: {}", e.getMessage());
                }
            } else {
                responseStatus = 200;
                responseMessage = "Đăng nhập thành công";
            }
            
        } catch (Exception e) {
            responseStatus = 401;
            responseMessage = "Đăng nhập thất bại: " + e.getMessage();
            auditBuilder
                    .severity("WARNING")
                    .actionType("LOGIN_FAILED");
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            auditBuilder
                    .responseStatus(responseStatus)
                    .responseMessage(responseMessage)
                    .executionTimeMs((int) executionTime);
            
            try {
                auditLogService.logAsync(auditBuilder.build());
            } catch (Exception e) {
                log.error("Failed to save login audit log: {}", e.getMessage());
            }
        }
        
        return result;
    }

    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractUserRole() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getAuthorities() != null) {
                return authentication.getAuthorities().stream()
                        .findFirst()
                        .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                        .orElse("UNKNOWN");
            }
            
            HttpServletRequest request = getCurrentHttpRequest();
            if (request != null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    return jwtService.extractRole(token);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract user role: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    private Integer extractUserId() {
        try {
            HttpServletRequest request = getCurrentHttpRequest();
            if (request != null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    return jwtService.extractCustomerId(token);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract user ID: {}", e.getMessage());
        }
        return null;
    }

    private String extractUserFullName() {
        try {
            // Full name is not stored in JWT, return null for now
            // Can be enhanced later to fetch from database if needed
            return null;
        } catch (Exception e) {
            log.debug("Failed to extract user full name: {}", e.getMessage());
        }
        return null;
    }

    private String determineActionType(Method method, HttpServletRequest request) {
        if (method.isAnnotationPresent(PostMapping.class)) {
            return "CREATE";
        } else if (method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(PatchMapping.class)) {
            return "UPDATE";
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            return "DELETE";
        }
        return request.getMethod();
    }

    private String determineEntityType(String uri) {
        // Extract entity type from URI
        // e.g., /api/products -> Product, /api/invoices -> Invoice
        if (uri == null) return "";
        
        String[] parts = uri.split("/");
        if (parts.length >= 3) {
            String entity = parts[2];
            // Convert plural to singular and capitalize
            if (entity.endsWith("ies")) {
                entity = entity.substring(0, entity.length() - 3) + "y";
            } else if (entity.endsWith("s")) {
                entity = entity.substring(0, entity.length() - 1);
            }
            return capitalize(entity);
        }
        return "";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String generateDescription(String actionType, String entityType, String uri) {
        // Special handling for specific endpoints
        if (uri != null) {
            // Walk-in endpoints
            if (uri.contains("/walk-in/cancel/")) {
                return "Hủy Walk-in";
            }
            if (uri.contains("/walk-in") && "CREATE".equals(actionType)) {
                return "Tạo mới Walk-in";
            }
            
            // Reservation endpoints
            if (uri.contains("/reservations/cancel/") || uri.contains("/reservation/cancel/")) {
                return "Hủy đặt bàn";
            }
            if (uri.contains("/reservations/confirm/") || uri.contains("/reservation/confirm/")) {
                return "Xác nhận đặt bàn";
            }
            if (uri.contains("/reservations/check-in/") || uri.contains("/reservation/check-in/")) {
                return "Check-in đặt bàn";
            }
            if (uri.contains("/reservations") && "CREATE".equals(actionType)) {
                return "Tạo mới đặt bàn";
            }
            if (uri.contains("/reservations") && "UPDATE".equals(actionType)) {
                return "Cập nhật đặt bàn";
            }
            
            // Invoice/Payment endpoints
            if (uri.contains("/invoices/checkout") || uri.contains("/payment/checkout")) {
                return "Thanh toán hóa đơn";
            }
            if (uri.contains("/invoices/cancel/") || uri.contains("/invoice/cancel/")) {
                return "Hủy hóa đơn";
            }
            
            // Order endpoints
            if (uri.contains("/orders") && "CREATE".equals(actionType)) {
                return "Tạo đơn hàng";
            }
            if (uri.contains("/orders/cancel/")) {
                return "Hủy đơn hàng";
            }
            
            // Product endpoints
            if (uri.contains("/products") && "CREATE".equals(actionType)) {
                return "Tạo mới sản phẩm";
            }
            if (uri.contains("/products") && "UPDATE".equals(actionType)) {
                return "Cập nhật sản phẩm";
            }
            if (uri.contains("/products") && "DELETE".equals(actionType)) {
                return "Xóa sản phẩm";
            }
            
            // Combo endpoints
            if (uri.contains("/combos") && "CREATE".equals(actionType)) {
                return "Tạo mới combo";
            }
            if (uri.contains("/combos") && "UPDATE".equals(actionType)) {
                return "Cập nhật combo";
            }
            if (uri.contains("/combos") && "DELETE".equals(actionType)) {
                return "Xóa combo";
            }
            
            // Voucher endpoints
            if (uri.contains("/vouchers") && "CREATE".equals(actionType)) {
                return "Tạo mới voucher";
            }
            if (uri.contains("/vouchers") && "UPDATE".equals(actionType)) {
                return "Cập nhật voucher";
            }
            if (uri.contains("/vouchers") && "DELETE".equals(actionType)) {
                return "Xóa voucher";
            }
            
            // Customer endpoints
            if (uri.contains("/customers") && "CREATE".equals(actionType)) {
                return "Tạo mới khách hàng";
            }
            if (uri.contains("/customers") && "UPDATE".equals(actionType)) {
                return "Cập nhật khách hàng";
            }
            
            // Employee endpoints
            if (uri.contains("/employees") && "CREATE".equals(actionType)) {
                return "Tạo mới nhân viên";
            }
            if (uri.contains("/employees") && "UPDATE".equals(actionType)) {
                return "Cập nhật nhân viên";
            }
            if (uri.contains("/employees") && "DELETE".equals(actionType)) {
                return "Xóa nhân viên";
            }
            
            // Table endpoints
            if (uri.contains("/tables") && "CREATE".equals(actionType)) {
                return "Tạo mới bàn ăn";
            }
            if (uri.contains("/tables") && "UPDATE".equals(actionType)) {
                return "Cập nhật bàn ăn";
            }
            if (uri.contains("/tables") && "DELETE".equals(actionType)) {
                return "Xóa bàn ăn";
            }
        }
        
        // Fallback to generic description
        String action = "";
        switch (actionType) {
            case "CREATE": action = "Tạo mới"; break;
            case "UPDATE": action = "Cập nhật"; break;
            case "DELETE": action = "Xóa"; break;
            default: action = actionType;
        }
        
        String entity = translateEntity(entityType);
        
        if (entity.isEmpty()) {
            return action + " - " + uri;
        }
        
        return action + " " + entity;
    }

    private String translateEntity(String entityType) {
        if (entityType == null || entityType.isEmpty()) return "";
        
        switch (entityType.toLowerCase()) {
            case "product": return "sản phẩm";
            case "combo": return "combo";
            case "invoice": return "hóa đơn";
            case "customer": return "khách hàng";
            case "employee": return "nhân viên";
            case "table": return "bàn ăn";
            case "reservation": return "đặt bàn";
            case "voucher": return "voucher";
            case "review": return "đánh giá";
            default: return entityType;
        }
    }

    private String determineSeverity(String actionType) {
        switch (actionType) {
            case "DELETE": return "WARNING";
            case "CREATE":
            case "UPDATE": return "INFO";
            default: return "INFO";
        }
    }
}
