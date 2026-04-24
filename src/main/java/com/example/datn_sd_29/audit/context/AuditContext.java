package com.example.datn_sd_29.audit.context;

import com.example.datn_sd_29.audit.dto.FieldChange;

import java.util.List;

/**
 * ThreadLocal context để các service tầng nghiệp vụ đẩy thông tin audit bổ sung lên aspect —
 * tránh phải thay đổi signature mọi method (kiểu truyền qua tham số).
 *
 * Luồng sử dụng:
 *  1. Service tầng nghiệp vụ (VD: BlogPostService.updatePost) snapshot state trước/sau
 *     rồi gọi AuditContext.setChanges(diff)
 *  2. AutoAuditAspect sau khi execute method xong đọc context → thêm vào audit log
 *  3. Aspect luôn gọi clear() trong finally để tránh rò rỉ giữa các request trên cùng thread
 *
 * Lưu ý: phải CLEAR sau mỗi request vì thread pool tái sử dụng thread.
 */
public final class AuditContext {

    private static final ThreadLocal<List<FieldChange>> CHANGES = new ThreadLocal<>();

    private AuditContext() {}

    public static void setChanges(List<FieldChange> changes) {
        if (changes != null && !changes.isEmpty()) {
            CHANGES.set(changes);
        }
    }

    public static List<FieldChange> getChanges() {
        return CHANGES.get();
    }

    public static void clear() {
        CHANGES.remove();
    }
}
