package com.example.datn_sd_29.voucher.enums;

public enum VoucherStatus {
    HOAT_DONG("Hoạt động"),
    DA_DUNG("Đã dùng"),
    HET_HAN("Hết hạn"),
    KHONG_HOAT_DONG("Không hoạt động");

    private final String displayName;

    VoucherStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static VoucherStatus fromDisplayName(String displayName) {
        for (VoucherStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        // Fallback cho các giá trị cũ tiếng Anh
        return switch (displayName) {
            case "ACTIVE" -> HOAT_DONG;
            case "USED" -> DA_DUNG;
            case "EXPIRED" -> HET_HAN;
            case "INACTIVE" -> KHONG_HOAT_DONG;
            default -> throw new IllegalArgumentException("Unknown voucher status: " + displayName);
        };
    }
}
