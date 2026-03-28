package com.example.datn_sd_29.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket notification for no-show events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoShowNotification {
    
    /**
     * Reservation code
     */
    private String reservationCode;
    
    /**
     * Customer name
     */
    private String customerName;
    
    /**
     * Customer phone number
     */
    private String phoneNumber;
    
    /**
     * Reserved time
     */
    private LocalDateTime reservedAt;
    
    /**
     * Table names
     */
    private String tableNames;
    
    /**
     * Guest count
     */
    private Integer guestCount;
    
    /**
     * Detection time
     */
    private LocalDateTime detectedAt;
    
    /**
     * Notification type: "NO_SHOW_DETECTED" or "NO_SHOW_AUTO_CANCELLED"
     */
    private String notificationType;
    
    /**
     * Additional message
     */
    private String message;
}
