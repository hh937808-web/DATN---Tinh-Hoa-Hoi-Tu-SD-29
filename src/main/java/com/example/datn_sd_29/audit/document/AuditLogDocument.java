package com.example.datn_sd_29.audit.document;

import com.example.datn_sd_29.audit.dto.FieldChange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
@CompoundIndexes({
    // Index theo access pattern phổ biến — tối ưu cho query admin hay dùng
    @CompoundIndex(name = "idx_user_time",     def = "{'userEmail': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_role_time",     def = "{'userRole': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_entity_time",   def = "{'entityType': 1, 'entityId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_severity_time", def = "{'severity': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_action_time",   def = "{'actionType': 1, 'createdAt': -1}")
})
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
    
    // Timestamp — thường index (không phải TTL), TTL dùng expiresAt để mỗi log có retention riêng
    @Indexed
    private Instant createdAt;

    // TTL-enforcement — MongoDB tự xóa document khi expiresAt < now.
    // Giá trị được set trong AuditLogService tùy theo loại actionType:
    //   - PAYMENT_*, INVOICE_*  → 10 năm (compliance tài chính VN)
    //   - LOGIN_FAILED, PASSWORD_*, *_DISABLE, *_HARD_DELETE → 1 năm (bảo mật)
    //   - Còn lại               → 90 ngày (hoạt động bình thường)
    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;

    // Additional Context
    @Indexed
    private String severity;

    // Performance Metrics
    private Integer executionTimeMs;

    // Diff snapshot cho UPDATE — ghi rõ field nào đổi từ giá trị gì sang giá trị gì
    private List<FieldChange> changes;
}
