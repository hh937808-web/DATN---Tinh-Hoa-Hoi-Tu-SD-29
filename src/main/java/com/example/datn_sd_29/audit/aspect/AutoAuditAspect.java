package com.example.datn_sd_29.audit.aspect;

import com.example.datn_sd_29.audit.context.AuditContext;
import com.example.datn_sd_29.audit.dto.AuditLogRequest;
import com.example.datn_sd_29.audit.service.AuditLogService;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.repository.EmployeeRepository;
import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product.repository.ProductRepository;
import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import com.example.datn_sd_29.product_combo.repository.ProductComboRepository;
import com.example.datn_sd_29.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.math.BigDecimal;

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
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductComboRepository productComboRepository;

    @Value("${security.api.enabled:true}")
    private boolean securityEnabled;

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
        
        // Định danh user — cùng pattern với PaymentService/WalkInService/StaffOrderService:
        //   security.api.enabled = true  → đọc từ JWT
        //   security.api.enabled = false → đọc header X-Employee-Username / X-Customer-Email rồi lookup DB để lấy role THẬT
        String userEmail = null;
        String userRole = null;
        Integer userId = null;
        String userFullName = null;

        if (securityEnabled) {
            try {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    userEmail = jwtService.extractEmail(token); // username với employee, email với customer
                    userRole = jwtService.extractRole(token);
                    userId = jwtService.extractCustomerId(token);
                }
            } catch (Exception e) {
                log.debug("Failed to extract user info from JWT: {}", e.getMessage());
            }
        } else {
            // Security tắt — lookup DB để lấy role + fullName chính xác
            String empUsername = request.getHeader("X-Employee-Username");
            if (empUsername != null && !empUsername.trim().isEmpty()) {
                Employee emp = employeeRepository.findByUsernameIgnoreCase(empUsername.trim()).orElse(null);
                if (emp != null) {
                    userEmail = emp.getUsername();
                    userRole = emp.getRole();           // ADMIN / STAFF / RECEPTION / KITCHEN — LẤY TỪ DB
                    userId = emp.getId();
                    userFullName = emp.getFullName();
                }
            }

            if (userEmail == null) {
                String custEmail = request.getHeader("X-Customer-Email");
                if (custEmail != null && !custEmail.trim().isEmpty()) {
                    Customer cust = customerRepository.findByEmailIgnoreCase(custEmail.trim()).orElse(null);
                    if (cust != null) {
                        userEmail = cust.getEmail();
                        userRole = "USER";
                        userId = cust.getId();
                        userFullName = cust.getFullName();
                    }
                }
            }
        }

        // Không định danh được → log với anonymous để vẫn có dấu vết (không skip)
        if (userEmail == null || userEmail.isEmpty() || "anonymousUser".equals(userEmail)) {
            userEmail = "anonymous";
            userRole = "ANONYMOUS";
        }
        
        // Determine action type and entity from method and request
        String actionType = determineActionType(method, request);
        String entityType = determineEntityType(request.getRequestURI());
        
        // Extract request body for detailed description
        Object requestBody = extractRequestBody(joinPoint);
        String description = generateDetailedDescription(actionType, entityType, request.getRequestURI(), requestBody, userRole);
        String severity = determineSeverity(actionType);
        
        // Build audit log request
        String entityId = extractEntityIdFromUri(request.getRequestURI());
        AuditLogRequest.AuditLogRequestBuilder auditBuilder = AuditLogRequest.builder()
                .userEmail(userEmail)
                .userRole(userRole)
                .userId(userId)
                .userFullName(userFullName)
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
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

            // Đọc diff từ AuditContext — service nghiệp vụ set qua ThreadLocal trước khi method return
            java.util.List<com.example.datn_sd_29.audit.dto.FieldChange> changes = AuditContext.getChanges();

            // Complete audit log
            auditBuilder
                    .responseStatus(responseStatus)
                    .responseMessage(responseMessage)
                    .executionTimeMs((int) executionTime)
                    .changes(changes);

            // Save audit log asynchronously
            try {
                auditLogService.logAsync(auditBuilder.build());
            } catch (Exception e) {
                log.error("Failed to save audit log: {}", e.getMessage());
            }

            // CRITICAL: luôn clear context để tránh rò rỉ giữa các request trên cùng thread
            AuditContext.clear();
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

            // Blog endpoints (xử lý TRƯỚC fallback — kể cả khi body null)
            if (uri != null && uri.startsWith("/api/blog/admin")) {
                return extractBlogDetails(actionType, uri, requestBody);
            }

            // Kitchen item actions (URL-driven, không có body)
            if (actionType != null && actionType.startsWith("KITCHEN_ITEM_")) {
                return extractKitchenActionDetails(actionType, uri);
            }

            // Review / feedback admin actions
            if (actionType != null && actionType.startsWith("REVIEW_")) {
                return extractReviewActionDetails(actionType, uri, requestBody);
            }

            // Payment-specific actions
            if (actionType != null && (actionType.startsWith("PAYMENT_") || actionType.startsWith("INVOICE_ITEM_"))) {
                return extractPaymentActionDetails(actionType, uri, requestBody);
            }

            // Reservation lifecycle actions beyond CREATE
            if (actionType != null && actionType.startsWith("RESERVATION_") && !"RESERVATION_CREATE".equals(actionType)) {
                return extractReservationActionDetails(actionType, uri);
            }

            // Walk-in lifecycle actions
            if (actionType != null && actionType.startsWith("WALKIN_")) {
                return extractWalkinActionDetails(actionType, uri, requestBody);
            }

            // Auth actions
            if (actionType != null && (actionType.equals("LOGOUT") || actionType.startsWith("REGISTER_"))) {
                return extractAuthActionDetails(actionType, requestBody);
            }

            // Customer specific actions
            if (actionType != null && actionType.startsWith("CUSTOMER_") && !"CUSTOMER_PROFILE_UPDATE".equals(actionType)) {
                return extractCustomerActionDetails(actionType, uri, requestBody);
            }

            // Query Builder
            if (actionType != null && (actionType.startsWith("QUERY_") || actionType.startsWith("SAVED_QUERY_") || actionType.startsWith("DASHBOARD_"))) {
                return extractQueryBuilderDetails(actionType, uri, requestBody);
            }

        } catch (Exception e) {
            log.debug("Failed to generate detailed description: {}", e.getMessage());
        }

        // Fallback to basic description
        return generateDescription(actionType, entityType, uri);
    }

    /**
     * Mô tả chi tiết cho các thao tác trên bài viết (blog).
     * Format: "<Tên action>: \"<tiêu đề>\" [<danh mục>] - <thông tin bổ sung>"
     * Đọc vào hội đồng hiểu ngay ai làm gì với bài nào.
     */
    private String extractBlogDetails(String actionType, String uri, Object requestBody) {
        String postId = extractEntityIdFromUri(uri);
        String title = safeInvokeMethod(requestBody, "getTitle", String.class);
        String category = safeInvokeMethod(requestBody, "getCategory", String.class);
        Boolean isPublished = safeInvokeMethod(requestBody, "getIsPublished", Boolean.class);
        Object scheduledAt = safeInvokeMethod(requestBody, "getScheduledPublishAt", Object.class);
        Object expiresAt = safeInvokeMethod(requestBody, "getExpiresAt", Object.class);

        StringBuilder sb = new StringBuilder();
        switch (actionType) {
            case "BLOG_CREATE":
                sb.append("Tạo mới bài viết");
                if (title != null && !title.isEmpty()) {
                    sb.append(": \"").append(truncate(title, 80)).append("\"");
                }
                if (category != null && !category.isEmpty()) {
                    sb.append(" [").append(category).append("]");
                }
                if (scheduledAt != null) {
                    sb.append(" - Lên lịch đăng: ").append(scheduledAt);
                } else if (Boolean.TRUE.equals(isPublished)) {
                    sb.append(" - Xuất bản ngay");
                } else {
                    sb.append(" - Lưu bản nháp");
                }
                if (expiresAt != null) {
                    sb.append(" - Hết hạn: ").append(expiresAt);
                }
                return sb.toString();

            case "BLOG_UPDATE":
                sb.append("Cập nhật bài viết");
                if (postId != null) sb.append(" #").append(postId);
                if (title != null && !title.isEmpty()) {
                    sb.append(": \"").append(truncate(title, 80)).append("\"");
                }
                if (category != null && !category.isEmpty()) {
                    sb.append(" [").append(category).append("]");
                }
                if (scheduledAt != null) {
                    sb.append(" - Lên lịch đăng: ").append(scheduledAt);
                } else if (Boolean.TRUE.equals(isPublished)) {
                    sb.append(" - Đánh dấu xuất bản");
                }
                return sb.toString();

            case "BLOG_DISABLE":
                sb.append("Vô hiệu hóa bài viết");
                if (postId != null) sb.append(" #").append(postId);
                sb.append(" (chuyển sang danh sách vô hiệu, dữ liệu vẫn lưu trong DB)");
                return sb.toString();

            case "BLOG_RESTORE":
                sb.append("Kích hoạt lại bài viết");
                if (postId != null) sb.append(" #").append(postId);
                sb.append(" (từ danh sách vô hiệu về bản nháp)");
                return sb.toString();

            case "BLOG_HARD_DELETE":
                sb.append("Xóa vĩnh viễn bài viết");
                if (postId != null) sb.append(" #").append(postId);
                sb.append(" (xóa cứng, không thể khôi phục)");
                return sb.toString();

            case "BLOG_AI_GENERATE":
                sb.append("Yêu cầu AI viết nội dung bài");
                if (title != null && !title.isEmpty()) {
                    sb.append(" - Tiêu đề: \"").append(truncate(title, 80)).append("\"");
                }
                if (category != null && !category.isEmpty()) {
                    sb.append(" [").append(category).append("]");
                }
                return sb.toString();

            default:
                return "Thao tác trên bài viết" + (postId != null ? " #" + postId : "");
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    // ===== KITCHEN =====
    private String extractKitchenActionDetails(String actionType, String uri) {
        String itemId = extractEntityIdFromUri(uri);
        String idSuffix = itemId != null ? " #" + itemId : "";
        switch (actionType) {
            case "KITCHEN_ITEM_START":    return "Bắt đầu nấu món" + idSuffix;
            case "KITCHEN_ITEM_DONE":     return "Hoàn tất nấu món" + idSuffix;
            case "KITCHEN_ITEM_SERVE":    return "Phục vụ món" + idSuffix + " đến bàn";
            case "KITCHEN_ITEM_CANCEL":   return "Hủy món" + idSuffix + " trong đơn";
            case "KITCHEN_ITEM_ACTIVATE": return "Kích hoạt dessert" + idSuffix;
            default: return "Thao tác bếp" + idSuffix;
        }
    }

    // ===== REVIEW / FEEDBACK =====
    private String extractReviewActionDetails(String actionType, String uri, Object requestBody) {
        String reviewId = extractEntityIdFromUri(uri);
        String idSuffix = reviewId != null ? " #" + reviewId : "";
        Integer rating = safeInvokeMethod(requestBody, "getRating", Integer.class);
        String content = safeInvokeMethod(requestBody, "getContent", String.class);

        switch (actionType) {
            case "REVIEW_CREATE": {
                StringBuilder sb = new StringBuilder("Gửi đánh giá mới");
                if (rating != null) sb.append(" - ").append(rating).append(" sao");
                if (content != null && !content.isEmpty()) sb.append(" - Nội dung: \"").append(truncate(content, 80)).append("\"");
                return sb.toString();
            }
            case "REVIEW_APPROVE": return "Phê duyệt đánh giá" + idSuffix + " (hiển thị công khai)";
            case "REVIEW_REJECT":  return "Từ chối đánh giá" + idSuffix + " (ẩn khỏi hiển thị)";
            case "REVIEW_DELETE":  return "Xóa đánh giá" + idSuffix;
            default: return "Thao tác đánh giá" + idSuffix;
        }
    }

    // ===== PAYMENT / INVOICE =====
    private String extractPaymentActionDetails(String actionType, String uri, Object requestBody) {
        Integer invoiceId = safeInvokeMethod(requestBody, "getInvoiceId", Integer.class);
        BigDecimal totalAmount = safeInvokeMethod(requestBody, "getTotalAmount", BigDecimal.class);
        String paymentMethod = safeInvokeMethod(requestBody, "getPaymentMethod", String.class);
        BigDecimal discountAmount = safeInvokeMethod(requestBody, "getDiscountAmount", BigDecimal.class);
        Integer itemId = extractEntityIdFromUri(uri) != null ? Integer.valueOf(extractEntityIdFromUri(uri)) : null;
        Integer quantity = safeInvokeMethod(requestBody, "getQuantity", Integer.class);
        Integer productId = safeInvokeMethod(requestBody, "getProductId", Integer.class);
        Integer comboId = safeInvokeMethod(requestBody, "getComboId", Integer.class);

        StringBuilder sb = new StringBuilder();
        switch (actionType) {
            case "PAYMENT_CHECKOUT":
                sb.append("Thanh toán hóa đơn");
                if (invoiceId != null) sb.append(" #").append(invoiceId);
                if (totalAmount != null) sb.append(" - Tổng: ").append(formatMoney(totalAmount));
                if (paymentMethod != null) sb.append(" - Phương thức: ").append(translatePaymentMethod(paymentMethod));
                if (discountAmount != null && discountAmount.signum() > 0) sb.append(" - Giảm giá: ").append(formatMoney(discountAmount));
                return sb.toString();
            case "PAYMENT_CANCEL":
                sb.append("Hủy thanh toán");
                if (invoiceId != null) sb.append(" hóa đơn #").append(invoiceId);
                return sb.toString();
            case "INVOICE_ITEM_ADD": {
                // Lookup tên sản phẩm/combo để audit log có đủ thông tin truy vết tranh chấp
                String itemName = lookupProductOrComboName(productId, comboId);
                sb.append("Thêm món");
                if (itemName != null) sb.append(" '").append(itemName).append("'");
                if (invoiceId != null) sb.append(" vào hóa đơn #").append(invoiceId);
                if (quantity != null) sb.append(" - SL: ").append(quantity);
                return sb.toString();
            }
            case "INVOICE_ORDER_ADD_ITEMS": {
                // Request body là TableOrderRequest với list items — gom tên từng món
                String itemsList = summarizeOrderItems(requestBody);
                sb.append("Gọi thêm món cho bàn");
                if (itemsList != null && !itemsList.isEmpty()) sb.append(": ").append(itemsList);
                return sb.toString();
            }
            case "INVOICE_ITEM_UPDATE":
                sb.append("Cập nhật món #").append(itemId != null ? itemId : "?");
                if (quantity != null) sb.append(" - SL mới: ").append(quantity);
                return sb.toString();
            case "INVOICE_ITEM_REMOVE":
                sb.append("Xóa món #").append(itemId != null ? itemId : "?").append(" khỏi hóa đơn");
                return sb.toString();
            default:
                return "Thao tác hóa đơn";
        }
    }

    /**
     * Lookup tên Product hoặc ProductCombo từ ID — trả null nếu không tìm được.
     */
    private String lookupProductOrComboName(Integer productId, Integer comboId) {
        try {
            if (productId != null) {
                Product p = productRepository.findById(productId).orElse(null);
                if (p != null) {
                    // Thử nhiều tên field có thể dùng
                    String name = safeInvokeMethod(p, "getProductName", String.class);
                    if (name == null) name = safeInvokeMethod(p, "getName", String.class);
                    return name;
                }
            }
            if (comboId != null) {
                ProductCombo c = productComboRepository.findById(comboId).orElse(null);
                if (c != null) {
                    String name = safeInvokeMethod(c, "getComboName", String.class);
                    if (name == null) name = safeInvokeMethod(c, "getName", String.class);
                    return name;
                }
            }
        } catch (Exception e) {
            log.debug("Không lookup được tên sản phẩm/combo: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Gom tên các items trong TableOrderRequest → chuỗi dạng "Lẩu bò x2, Tôm hùm x1, Rau muống x3"
     */
    @SuppressWarnings("unchecked")
    private String summarizeOrderItems(Object requestBody) {
        try {
            Object itemsObj = safeInvokeMethod(requestBody, "getItems", Object.class);
            if (!(itemsObj instanceof java.util.List)) return null;
            java.util.List<Object> items = (java.util.List<Object>) itemsObj;
            StringBuilder sb = new StringBuilder();
            int max = Math.min(items.size(), 5); // tránh log quá dài
            for (int i = 0; i < max; i++) {
                Object item = items.get(i);
                Integer pId = safeInvokeMethod(item, "getProductId", Integer.class);
                Integer cId = safeInvokeMethod(item, "getProductComboId", Integer.class);
                Integer qty = safeInvokeMethod(item, "getQuantity", Integer.class);
                String name = lookupProductOrComboName(pId, cId);
                if (name == null) name = pId != null ? "SP#" + pId : (cId != null ? "Combo#" + cId : "?");
                if (sb.length() > 0) sb.append(", ");
                sb.append(name);
                if (qty != null) sb.append(" x").append(qty);
            }
            if (items.size() > max) sb.append(", …+").append(items.size() - max).append(" món khác");
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "";
        return String.format("%,.0fđ", amount.doubleValue());
    }

    private String translatePaymentMethod(String m) {
        if (m == null) return "";
        switch (m.toUpperCase()) {
            case "CASH": return "Tiền mặt";
            case "BANK_TRANSFER": return "Chuyển khoản";
            default: return m;
        }
    }

    // ===== RESERVATION lifecycle (sau khi đã tạo) =====
    private String extractReservationActionDetails(String actionType, String uri) {
        // ID trong URL có thể là reservationCode (string) hoặc numeric
        String code = null;
        if (uri != null) {
            String[] parts = uri.split("/");
            // Pattern: /api/reservation/{code}/action → code là part[3]
            if (parts.length >= 4) code = parts[3];
        }
        String suffix = code != null && !code.isEmpty() ? " #" + code : "";
        switch (actionType) {
            case "RESERVATION_CONFIRM":          return "Xác nhận đặt bàn" + suffix;
            case "RESERVATION_CHECKIN":          return "Check-in đặt bàn" + suffix;
            case "RESERVATION_CANCEL":           return "Hủy đặt bàn" + suffix;
            case "RESERVATION_EMAIL_SEND":       return "Gửi email xác nhận đặt bàn" + suffix;
            case "RESERVATION_TABLE_REASSIGN":   return "Chuyển bàn cho đặt bàn" + suffix;
            default: return "Thao tác đặt bàn" + suffix;
        }
    }

    // ===== WALK-IN =====
    private String extractWalkinActionDetails(String actionType, String uri, Object requestBody) {
        String code = extractEntityIdFromUri(uri);
        String suffix = code != null ? " #" + code : "";
        Integer guestCount = safeInvokeMethod(requestBody, "getGuestCount", Integer.class);
        String customerName = safeInvokeMethod(requestBody, "getCustomerName", String.class);
        switch (actionType) {
            case "WALKIN_CHECKIN": {
                StringBuilder sb = new StringBuilder("Check-in khách walk-in");
                if (guestCount != null) sb.append(" cho ").append(guestCount).append(" người");
                if (customerName != null && !customerName.isEmpty()) sb.append(" - KH: ").append(customerName);
                return sb.toString();
            }
            case "WALKIN_CANCEL": return "Hủy bàn walk-in" + suffix;
            default: return "Thao tác walk-in" + suffix;
        }
    }

    // ===== AUTH =====
    private String extractAuthActionDetails(String actionType, Object requestBody) {
        String email = safeInvokeMethod(requestBody, "getEmail", String.class);
        String emailSuffix = email != null && !email.isEmpty() ? " - " + email : "";
        switch (actionType) {
            case "REGISTER_START":    return "Bắt đầu đăng ký tài khoản" + emailSuffix;
            case "REGISTER_OTP_SEND": return "Gửi mã OTP xác nhận" + emailSuffix;
            case "REGISTER_VERIFY":   return "Hoàn tất đăng ký (xác nhận OTP)" + emailSuffix;
            case "LOGOUT":            return "Đăng xuất khỏi hệ thống";
            default: return "Thao tác xác thực";
        }
    }

    // ===== CUSTOMER specific =====
    private String extractCustomerActionDetails(String actionType, String uri, Object requestBody) {
        String customerId = extractEntityIdFromUri(uri);
        String suffix = customerId != null ? " #" + customerId : "";
        Boolean isActive = safeInvokeMethod(requestBody, "getIsActive", Boolean.class);
        switch (actionType) {
            case "CUSTOMER_STATUS_CHANGE":
                return "Thay đổi trạng thái khách hàng" + suffix
                        + (isActive != null ? (isActive ? " - Kích hoạt" : " - Vô hiệu hóa") : "");
            case "CUSTOMER_PASSWORD_CHANGE":
                return "Đổi mật khẩu" + suffix;
            default:
                return "Thao tác khách hàng" + suffix;
        }
    }

    // ===== QUERY BUILDER =====
    private String extractQueryBuilderDetails(String actionType, String uri, Object requestBody) {
        String id = extractEntityIdFromUri(uri);
        String name = safeInvokeMethod(requestBody, "getName", String.class);
        String question = safeInvokeMethod(requestBody, "getQuestion", String.class);
        String sqlQuery = safeInvokeMethod(requestBody, "getSqlQuery", String.class);

        switch (actionType) {
            case "QUERY_EXECUTE": {
                StringBuilder sb = new StringBuilder("Thực thi custom query");
                if (sqlQuery != null) sb.append(": \"").append(truncate(sqlQuery, 120)).append("\"");
                return sb.toString();
            }
            case "QUERY_AI_GENERATE":
                return "Sinh SQL từ ngôn ngữ tự nhiên" + (question != null ? " - Câu hỏi: \"" + truncate(question, 100) + "\"" : "");
            case "SAVED_QUERY_CREATE":
                return "Lưu query mới" + (name != null ? ": \"" + truncate(name, 80) + "\"" : "");
            case "SAVED_QUERY_UPDATE":
                return "Cập nhật saved query" + (id != null ? " #" + id : "") + (name != null ? ": \"" + truncate(name, 80) + "\"" : "");
            case "SAVED_QUERY_DELETE":
                return "Xóa saved query" + (id != null ? " #" + id : "");
            case "DASHBOARD_CREATE":
                return "Tạo dashboard" + (name != null ? ": \"" + truncate(name, 80) + "\"" : "");
            case "DASHBOARD_UPDATE":
                return "Cập nhật dashboard" + (id != null ? " #" + id : "");
            case "DASHBOARD_DELETE":
                return "Xóa dashboard" + (id != null ? " #" + id : "");
            case "DASHBOARD_LAYOUT_SAVE":
                return "Lưu layout dashboard" + (id != null ? " #" + id : "");
            case "DASHBOARD_WIDGET_REMOVE":
                return "Xóa widget khỏi dashboard" + (id != null ? " #" + id : "");
            default:
                return "Thao tác query builder";
        }
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
        String uri = request.getRequestURI();
        if (uri == null) return httpVerbFallback(method, request);

        // ===== Blog =====
        if (uri.startsWith("/api/blog/admin")) {
            if (uri.endsWith("/ai-generate")) return "BLOG_AI_GENERATE";
            if (uri.endsWith("/disable")) return "BLOG_DISABLE";
            if (uri.endsWith("/restore")) return "BLOG_RESTORE";
            if (uri.endsWith("/permanent")) return "BLOG_HARD_DELETE";
            if (method.isAnnotationPresent(PostMapping.class)) return "BLOG_CREATE";
            if (method.isAnnotationPresent(PutMapping.class)) return "BLOG_UPDATE";
        }

        // ===== Auth =====
        if (uri.startsWith("/api/auth/")) {
            if (uri.contains("/login")) return "LOGIN";
            if (uri.contains("/logout")) return "LOGOUT";
            if (uri.contains("/register")) return "REGISTER_START";
            if (uri.contains("/otp/send")) return "REGISTER_OTP_SEND";
            if (uri.contains("/otp/verify")) return "REGISTER_VERIFY";
        }

        // ===== Customer =====
        if (uri.startsWith("/api/customers")) {
            if (uri.contains("/status")) return "CUSTOMER_STATUS_CHANGE";
            if (uri.contains("/change-password")) return "CUSTOMER_PASSWORD_CHANGE";
            if (uri.contains("/profile")) return "CUSTOMER_PROFILE_UPDATE";
        }

        // ===== Employee =====
        if (uri.startsWith("/api/employees")) {
            if (method.isAnnotationPresent(PostMapping.class)) return "EMPLOYEE_CREATE";
            if (method.isAnnotationPresent(PutMapping.class)) return "EMPLOYEE_UPDATE";
            if (method.isAnnotationPresent(DeleteMapping.class)) return "EMPLOYEE_DISABLE";
        }

        // ===== Reservation =====
        if (uri.startsWith("/api/reservation")) {
            if (uri.contains("/check-in")) return "RESERVATION_CHECKIN";
            if (uri.contains("/send-email")) return "RESERVATION_EMAIL_SEND";
            if (uri.contains("/cancel")) return "RESERVATION_CANCEL";
            if (uri.contains("/confirm")) return "RESERVATION_CONFIRM";
            if (uri.contains("/reassign-tables")) return "RESERVATION_TABLE_REASSIGN";
            if (method.isAnnotationPresent(PostMapping.class)) return "RESERVATION_CREATE";
        }

        // ===== Walk-in =====
        if (uri.startsWith("/api/walk-in")) {
            if (uri.contains("/check-in")) return "WALKIN_CHECKIN";
            if (uri.contains("/cancel")) return "WALKIN_CANCEL";
        }

        // ===== Invoice / Payment =====
        if (uri.startsWith("/api/reception/payment")) {
            if (uri.contains("/checkout")) return "PAYMENT_CHECKOUT";
            if (uri.contains("/cancel")) return "PAYMENT_CANCEL";
            if (uri.contains("/add-item")) return "INVOICE_ITEM_ADD";
            if (uri.contains("/items/") && method.isAnnotationPresent(PatchMapping.class)) return "INVOICE_ITEM_UPDATE";
            if (uri.contains("/items/") && method.isAnnotationPresent(DeleteMapping.class)) return "INVOICE_ITEM_REMOVE";
        }
        if (uri.startsWith("/public/notification/send/")) return "PAYMENT_QR_NOTIFY";

        // ===== Kitchen =====
        if (uri.startsWith("/api/kitchen/items/")) {
            if (uri.endsWith("/start")) return "KITCHEN_ITEM_START";
            if (uri.endsWith("/done")) return "KITCHEN_ITEM_DONE";
            if (uri.endsWith("/serve")) return "KITCHEN_ITEM_SERVE";
            if (uri.endsWith("/cancel")) return "KITCHEN_ITEM_CANCEL";
        }
        if (uri.startsWith("/api/invoices/items/") && uri.endsWith("/activate")) return "KITCHEN_ITEM_ACTIVATE";
        if ((uri.matches("/api/invoices/\\d+/order-items") || uri.matches("/api/tables/\\d+/order-items"))
                && method.isAnnotationPresent(PostMapping.class)) return "INVOICE_ORDER_ADD_ITEMS";

        // ===== Product =====
        if (uri.startsWith("/api/products")) {
            if (method.isAnnotationPresent(PostMapping.class)) return "PRODUCT_CREATE";
            if (method.isAnnotationPresent(PutMapping.class)) return "PRODUCT_UPDATE";
            if (method.isAnnotationPresent(DeleteMapping.class)) return "PRODUCT_DISABLE";
        }

        // ===== Combo =====
        if (uri.startsWith("/api/product-combos")) {
            if (method.isAnnotationPresent(PostMapping.class)) return "COMBO_CREATE";
            if (method.isAnnotationPresent(PutMapping.class)) return "COMBO_UPDATE";
            if (method.isAnnotationPresent(DeleteMapping.class)) return "COMBO_DISABLE";
        }
        if (uri.startsWith("/api/product-combo-items")) {
            if (method.isAnnotationPresent(PostMapping.class)) return "COMBO_ITEM_ADD";
            if (method.isAnnotationPresent(DeleteMapping.class)) return "COMBO_ITEMS_CLEAR";
        }

        // ===== Voucher (3 types) =====
        if (uri.startsWith("/api/product-vouchers")) {
            if (method.isAnnotationPresent(PostMapping.class)) return "PRODUCT_VOUCHER_CREATE";
            if (method.isAnnotationPresent(PutMapping.class)) return "PRODUCT_VOUCHER_UPDATE";
            if (method.isAnnotationPresent(DeleteMapping.class)) return "PRODUCT_VOUCHER_DISABLE";
        }
        if (uri.startsWith("/api/product-combo-vouchers")) {
            if (method.isAnnotationPresent(PostMapping.class)) return "COMBO_VOUCHER_CREATE";
            if (method.isAnnotationPresent(PutMapping.class)) return "COMBO_VOUCHER_UPDATE";
            if (method.isAnnotationPresent(DeleteMapping.class)) return "COMBO_VOUCHER_DISABLE";
        }
        if (uri.startsWith("/api/customer-vouchers")) {
            if (method.isAnnotationPresent(PostMapping.class)) return "CUSTOMER_VOUCHER_CREATE";
            if (method.isAnnotationPresent(PutMapping.class)) return "CUSTOMER_VOUCHER_UPDATE";
            if (method.isAnnotationPresent(DeleteMapping.class)) return "CUSTOMER_VOUCHER_DELETE";
        }

        // ===== Dining Table =====
        if (uri.startsWith("/api/tables") && !uri.contains("/order-items")) {
            if (method.isAnnotationPresent(PostMapping.class)) return "TABLE_CREATE";
            if (method.isAnnotationPresent(PutMapping.class)) return "TABLE_UPDATE";
            if (method.isAnnotationPresent(DeleteMapping.class)) return "TABLE_DELETE";
        }

        // ===== Review / Feedback =====
        if (uri.startsWith("/api/reviews")) {
            if (uri.contains("/approve")) return "REVIEW_APPROVE";
            if (uri.contains("/reject")) return "REVIEW_REJECT";
            if (method.isAnnotationPresent(DeleteMapping.class)) return "REVIEW_DELETE";
            if (method.isAnnotationPresent(PostMapping.class)) return "REVIEW_CREATE";
        }

        // ===== Query Builder =====
        if (uri.startsWith("/api/query-builder")) {
            if (uri.endsWith("/execute")) return "QUERY_EXECUTE";
            if (uri.contains("/ai/generate-sql")) return "QUERY_AI_GENERATE";
            if (uri.contains("/saved-queries")) {
                if (method.isAnnotationPresent(PostMapping.class)) return "SAVED_QUERY_CREATE";
                if (method.isAnnotationPresent(PutMapping.class)) return "SAVED_QUERY_UPDATE";
                if (method.isAnnotationPresent(DeleteMapping.class)) return "SAVED_QUERY_DELETE";
            }
            if (uri.contains("/dashboards/") && uri.contains("/layouts/")
                    && method.isAnnotationPresent(DeleteMapping.class)) return "DASHBOARD_WIDGET_REMOVE";
            if (uri.contains("/dashboards/") && uri.contains("/layouts")) return "DASHBOARD_LAYOUT_SAVE";
            if (uri.contains("/dashboards")) {
                if (method.isAnnotationPresent(PostMapping.class)) return "DASHBOARD_CREATE";
                if (method.isAnnotationPresent(PutMapping.class)) return "DASHBOARD_UPDATE";
                if (method.isAnnotationPresent(DeleteMapping.class)) return "DASHBOARD_DELETE";
            }
        }

        return httpVerbFallback(method, request);
    }

    private String httpVerbFallback(Method method, HttpServletRequest request) {
        if (method.isAnnotationPresent(PostMapping.class)) return "CREATE";
        if (method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(PatchMapping.class)) return "UPDATE";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        return request.getMethod();
    }

    /**
     * Extract entity ID from URI — tìm segment số trong path.
     * VD: /api/blog/admin/123/disable → "123"
     */
    private String extractEntityIdFromUri(String uri) {
        if (uri == null) return null;
        String[] parts = uri.split("/");
        for (String p : parts) {
            if (p.matches("\\d+")) return p;
        }
        return null;
    }

    private String determineEntityType(String uri) {
        if (uri == null) return "";

        // Override: đồng nhất entity type — quan trọng cho tra cứu truy vết tranh chấp
        // Các URL khác nhau nhưng cùng tác động lên 1 loại đối tượng → gom về 1 entityType chung
        if (uri.startsWith("/api/reception/payment")) return "Invoice";
        if (uri.startsWith("/api/invoices")) return "Invoice";
        if (uri.startsWith("/api/kitchen")) return "Invoice";
        if (uri.matches("/api/tables/\\d+/order-items.*")) return "Invoice";
        if (uri.startsWith("/public/notification/send")) return "Invoice";

        if (uri.startsWith("/api/reservation")) return "Reservation";
        if (uri.startsWith("/api/walk-in")) return "WalkIn";
        if (uri.startsWith("/api/blog")) return "Blog";
        if (uri.startsWith("/api/auth")) return "Auth";

        // Default: parse từ URI path (vd /api/products → Product)
        String[] parts = uri.split("/");
        if (parts.length >= 3) {
            String entity = parts[2];
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
            case "product-combo":
            case "combo": return "combo";
            case "invoice": return "hóa đơn";
            case "customer": return "khách hàng";
            case "employee": return "nhân viên";
            case "table": return "bàn ăn";
            case "reservation": return "đặt bàn";
            case "walk-in":
            case "walkin": return "walk-in";
            case "voucher":
            case "product-voucher":
            case "customer-voucher":
            case "product-combo-voucher": return "voucher";
            case "review": return "đánh giá";
            case "blog": return "bài viết";
            case "query-builder": return "query builder";
            case "kitchen": return "bếp";
            case "auth": return "tài khoản";
            default: return entityType;
        }
    }

    /**
     * Map action type → severity. Quy tắc:
     * - CRITICAL: thanh toán, hủy thanh toán, xóa cứng, đổi mật khẩu/role
     * - WARNING:  cancel, disable, delete soft, reject — tác động dữ liệu có thể khôi phục
     * - ERROR:    LOGIN_FAILED, exception (set bởi catch block)
     * - INFO:     create, update, state transition bình thường
     */
    private String determineSeverity(String actionType) {
        if (actionType == null) return "INFO";
        switch (actionType) {
            // CRITICAL — tiền bạc & bảo mật
            case "PAYMENT_CHECKOUT":
            case "PAYMENT_CANCEL":
            case "BLOG_HARD_DELETE":
            case "CUSTOMER_PASSWORD_CHANGE":
            case "PASSWORD_CHANGE":
            case "CUSTOMER_STATUS_CHANGE":
            case "EMPLOYEE_DISABLE":
                return "CRITICAL";

            // ERROR
            case "LOGIN_FAILED":
                return "ERROR";

            // WARNING — tác động dữ liệu có thể khôi phục
            case "DELETE":
            case "RESERVATION_CANCEL":
            case "WALKIN_CANCEL":
            case "KITCHEN_ITEM_CANCEL":
            case "INVOICE_ITEM_REMOVE":
            case "BLOG_DISABLE":
            case "PRODUCT_DISABLE":
            case "COMBO_DISABLE":
            case "PRODUCT_VOUCHER_DISABLE":
            case "COMBO_VOUCHER_DISABLE":
            case "CUSTOMER_VOUCHER_DELETE":
            case "TABLE_DELETE":
            case "REVIEW_REJECT":
            case "REVIEW_DELETE":
            case "SAVED_QUERY_DELETE":
            case "DASHBOARD_DELETE":
            case "DASHBOARD_WIDGET_REMOVE":
            case "COMBO_ITEMS_CLEAR":
            case "QUERY_EXECUTE":
                return "WARNING";

            // INFO — bình thường
            default:
                return "INFO";
        }
    }
}
