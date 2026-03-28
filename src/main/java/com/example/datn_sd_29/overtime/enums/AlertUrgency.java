package com.example.datn_sd_29.overtime.enums;

/**
 * Urgency levels for overtime alerts.
 * Requirements: 5.3
 */
public enum AlertUrgency {
    CRITICAL,  // Next reservation within 30 minutes - Immediate action required
    HIGH,      // Next reservation within 60 minutes - Action needed soon
    MEDIUM,    // Next reservation within 120 minutes - Monitor closely
    LOW        // No next reservation or > 120 minutes - Informational only
}
