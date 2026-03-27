package com.example.datn_sd_29.overtime.service.impl;

import com.example.datn_sd_29.overtime.dto.OvertimeAlert;
import com.example.datn_sd_29.overtime.enums.AlertUrgency;
import com.example.datn_sd_29.overtime.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of AlertService for managing and broadcasting overtime alerts.
 * Requirements: 2.1, 2.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, OvertimeAlert> activeAlerts = new ConcurrentHashMap<>();
    
    // Track broadcast metrics (Requirement 10.2)
    private long totalBroadcasts = 0;
    private long successfulBroadcasts = 0;
    private long failedBroadcasts = 0;
    
    /**
     * Broadcasts an overtime alert to all connected staff sessions.
     * Requirements: 2.3, 2.4, 2.5, 8.1, 8.2, 8.3, 8.4, 8.5
     */
    @Override
    public void broadcastOvertimeAlert(OvertimeAlert alert) {
        totalBroadcasts++;
        
        try {
            // Calculate urgency based on next reservation proximity
            AlertUrgency urgency = calculateUrgency(alert.getNextReservationTime());
            alert.setUrgency(urgency);
            
            // Add to active alerts cache
            activeAlerts.put(alert.getId(), alert);
            
            // Broadcast to WebSocket topic
            messagingTemplate.convertAndSend("/topic/overtime-alerts", alert);
            
            successfulBroadcasts++;
            
            log.info("Alert broadcast successful: {} - Tables: {}, Duration: {} minutes, Next reservation: {}, Urgency: {} (Success rate: {}/{})", 
                    alert.getId(), alert.getTableNames(), alert.getDiningDuration(), 
                    alert.getNextReservationTime(), urgency, successfulBroadcasts, totalBroadcasts);
        } catch (Exception e) {
            failedBroadcasts++;
            
            log.error("Failed to broadcast alert: {} - Tables: {} (Success rate: {}/{}, Failures: {})", 
                    alert.getId(), alert.getTableNames(), successfulBroadcasts, totalBroadcasts, failedBroadcasts, e);
        }
    }
    
    /**
     * Retrieves current active alerts sorted by urgency.
     * Requirements: 5.3
     */
    @Override
    public List<OvertimeAlert> getActiveAlerts() {
        return activeAlerts.values().stream()
                .sorted(Comparator.comparing(OvertimeAlert::getUrgency))
                .toList();
    }
    
    /**
     * Marks an alert as acknowledged and removes it from active alerts.
     * Requirements: 5.4
     */
    @Override
    public void acknowledgeAlert(String alertId, Integer staffId) {
        OvertimeAlert removed = activeAlerts.remove(alertId);
        if (removed != null) {
            log.info("Alert acknowledged: {} by staff {} at {}", 
                    alertId, staffId, Instant.now());
        } else {
            log.warn("Attempted to acknowledge non-existent alert: {} by staff {}", 
                    alertId, staffId);
        }
    }
    
    /**
     * Calculates urgency based on next reservation proximity.
     * CRITICAL: < 30 minutes - Immediate action required
     * HIGH: < 60 minutes - Action needed soon
     * MEDIUM: < 120 minutes - Monitor closely
     * LOW: No next reservation or > 120 minutes - Informational only
     */
    private AlertUrgency calculateUrgency(LocalDateTime nextReservationTime) {
        if (nextReservationTime == null) {
            return AlertUrgency.LOW; // No next reservation
        }
        
        Instant nextReservationInstant = nextReservationTime.atZone(ZoneId.systemDefault()).toInstant();
        long minutesUntilReservation = Duration.between(Instant.now(), nextReservationInstant).toMinutes();
        
        if (minutesUntilReservation < 30) {
            return AlertUrgency.CRITICAL;
        } else if (minutesUntilReservation < 60) {
            return AlertUrgency.HIGH;
        } else if (minutesUntilReservation < 120) {
            return AlertUrgency.MEDIUM;
        } else {
            return AlertUrgency.LOW; // > 120 minutes
        }
    }
    
    /**
     * Returns broadcast success rate metrics
     * Requirement 10.2
     */
    public double getBroadcastSuccessRate() {
        if (totalBroadcasts == 0) {
            return 100.0;
        }
        return (double) successfulBroadcasts / totalBroadcasts * 100.0;
    }
    
    /**
     * Returns total broadcast count
     * Requirement 10.2
     */
    public long getTotalBroadcasts() {
        return totalBroadcasts;
    }
    
    /**
     * Returns failed broadcast count
     * Requirement 10.2
     */
    public long getFailedBroadcasts() {
        return failedBroadcasts;
    }
}
