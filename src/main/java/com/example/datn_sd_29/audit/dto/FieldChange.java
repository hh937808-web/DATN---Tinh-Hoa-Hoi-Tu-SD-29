package com.example.datn_sd_29.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mô tả 1 thay đổi field khi UPDATE — dùng để audit diff.
 * VD: { field: "price", fieldLabel: "Giá", oldValue: "100000", newValue: "150000" }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldChange {
    /** Tên field kỹ thuật (VD: "price") */
    private String field;

    /** Nhãn tiếng Việt hiển thị cho admin (VD: "Giá bán") */
    private String fieldLabel;

    /** Giá trị cũ (đã toString) — null nếu là thêm mới field */
    private String oldValue;

    /** Giá trị mới (đã toString) — null nếu là xóa field */
    private String newValue;
}
