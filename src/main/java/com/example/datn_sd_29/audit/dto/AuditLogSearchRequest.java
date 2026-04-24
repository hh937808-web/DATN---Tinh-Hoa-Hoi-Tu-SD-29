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
    /**
     * Tìm kiếm toàn văn — regex match trong actionDescription, userEmail, entityId, userFullName.
     * Dùng khi admin chỉ nhớ từ khóa (tên khách, SĐT, tên món, username nhân viên...)
     */
    private String keyword;
    /**
     * Category filter — match nhiều actionType bằng substring.
     * VD: "CREATE" → match CREATE, BLOG_CREATE, PRODUCT_CREATE, RESERVATION_CREATE, ...
     *     "CANCEL" → match RESERVATION_CANCEL, WALKIN_CANCEL, KITCHEN_ITEM_CANCEL, ...
     *     "DELETE" → match DELETE, BLOG_HARD_DELETE, CUSTOMER_VOUCHER_DELETE, ...
     */
    private String actionCategory;
    private String entityType;
    private String entityId;
    private String severity;
    private Instant startDate;
    private Instant endDate;
    
    // Pagination
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;
    @Builder.Default
    private String sortBy = "createdAt";
    @Builder.Default
    private String sortDirection = "DESC";
}
