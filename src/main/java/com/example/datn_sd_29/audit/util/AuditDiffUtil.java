package com.example.datn_sd_29.audit.util;

import com.example.datn_sd_29.audit.dto.FieldChange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tiện ích tính diff giữa state cũ và state mới của entity để audit UPDATE.
 *
 * Usage:
 *   Map&lt;String,Object&gt; before = AuditDiffUtil.snapshot(post,
 *       "title","summary","content","category","isPublished","expiresAt");
 *   // ... modify & save ...
 *   Map&lt;String,Object&gt; after = AuditDiffUtil.snapshot(post, ...same fields);
 *   List&lt;FieldChange&gt; changes = AuditDiffUtil.diff(before, after, FIELD_LABELS);
 *   AuditContext.setChanges(changes);
 */
public final class AuditDiffUtil {

    private AuditDiffUtil() {}

    /**
     * Snapshot 1 entity thành Map&lt;fieldName, value&gt; qua reflection getter.
     * Chỉ capture các field được list để tránh log field không quan trọng (id, timestamp...).
     */
    public static Map<String, Object> snapshot(Object bean, String... fields) {
        Map<String, Object> snap = new LinkedHashMap<>();
        if (bean == null || fields == null) return snap;
        for (String field : fields) {
            Object value = readField(bean, field);
            snap.put(field, value);
        }
        return snap;
    }

    /**
     * So sánh 2 snapshot → trả về danh sách FieldChange cho các field có thay đổi.
     *
     * @param before  state trước khi modify
     * @param after   state sau khi save
     * @param labels  map fieldName → fieldLabel (tiếng Việt hiển thị cho admin)
     */
    public static List<FieldChange> diff(Map<String, Object> before, Map<String, Object> after,
                                         Map<String, String> labels) {
        List<FieldChange> result = new ArrayList<>();
        if (before == null || after == null) return result;

        for (Map.Entry<String, Object> entry : after.entrySet()) {
            String field = entry.getKey();
            Object newVal = entry.getValue();
            Object oldVal = before.get(field);

            if (!Objects.equals(normalize(oldVal), normalize(newVal))) {
                result.add(FieldChange.builder()
                        .field(field)
                        .fieldLabel(labels != null && labels.containsKey(field) ? labels.get(field) : field)
                        .oldValue(toDisplayString(oldVal))
                        .newValue(toDisplayString(newVal))
                        .build());
            }
        }
        return result;
    }

    /**
     * Chuẩn hoá để so sánh: null và empty string coi như nhau, BigDecimal dùng compareTo...
     */
    private static Object normalize(Object v) {
        if (v == null) return null;
        if (v instanceof String s && s.isEmpty()) return null;
        if (v instanceof java.math.BigDecimal bd) return bd.stripTrailingZeros().toPlainString();
        return v;
    }

    /**
     * Chuyển giá trị về string để hiển thị cho admin đọc.
     */
    private static String toDisplayString(Object v) {
        if (v == null) return "(trống)";
        if (v instanceof Boolean b) return b ? "Có" : "Không";
        if (v instanceof java.time.Instant i) return i.toString();
        if (v instanceof java.math.BigDecimal bd) return bd.stripTrailingZeros().toPlainString();
        String s = v.toString();
        // Truncate nếu quá dài (tránh audit log phình to, VD: content blog 10000 ký tự)
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }

    private static Object readField(Object bean, String field) {
        try {
            String methodName = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            return bean.getClass().getMethod(methodName).invoke(bean);
        } catch (Exception e) {
            // thử getter "is..." cho boolean
            try {
                String methodName = "is" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
                return bean.getClass().getMethod(methodName).invoke(bean);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
