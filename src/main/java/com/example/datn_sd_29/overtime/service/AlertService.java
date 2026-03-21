package com.example.datn_sd_29.overtime.service;

import com.example.datn_sd_29.overtime.dto.OvertimeAlert;

import java.util.List;

/**
 * Service for managing and broadcasting overtime alerts.
 * Requirements: 2.1, 2.4
 * 
 * Note: This interface will be fully implemented in task 5.
 */
public interface AlertService {
    
    /**
     * Broadcasts an overtime alert to all connected staff sessions.
     * Requirements: 2.3, 2.4, 2.5, 8.1, 8.2, 8.3, 8.4, 8.5
     * 
     * @param alert The alert to broadcast
     */
    void broadcastOvertimeAlert(OvertimeAlert alert);
    
    /**
     * Retrieves current active alerts.
     * Requirements: 5.3
     * 
     * @return List of active alerts
     */
    List<OvertimeAlert> getActiveAlerts();
    
    /**
     * Marks an alert as acknowledged.
     * Requirements: 5.4
     * 
     * @param alertId The alert ID
     * @param staffId The staff member who acknowledged
     */
    void acknowledgeAlert(String alertId, Integer staffId);
}
