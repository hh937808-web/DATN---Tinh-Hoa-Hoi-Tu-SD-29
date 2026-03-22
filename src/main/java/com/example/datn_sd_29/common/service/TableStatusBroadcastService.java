package com.example.datn_sd_29.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized service for broadcasting table status changes via WebSocket.
 * All table status updates should use this service for consistency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableStatusBroadcastService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Maps database table status to display status for frontend
     * IN_USE -> OCCUPIED (for consistency with DashboardService)
     */
    private String mapStatusForDisplay(String dbStatus) {
        if ("IN_USE".equals(dbStatus)) {
            return "OCCUPIED";
        }
        // AVAILABLE, RESERVED, CLEANING, etc. remain the same
        return dbStatus;
    }
    
    /**
     * Broadcasts table status change to all connected clients via WebSocket
     * @param tableIds List of table IDs that changed status
     * @param newStatus The new status of the tables (database status)
     */
    public void broadcastTableStatusChange(List<Integer> tableIds, String newStatus) {
        if (tableIds == null || tableIds.isEmpty()) {
            log.warn("broadcastTableStatusChange called with empty tableIds");
            return;
        }
        
        try {
            String displayStatus = mapStatusForDisplay(newStatus);
            Map<String, Object> message = new HashMap<>();
            message.put("tableIds", tableIds);
            message.put("status", displayStatus);
            message.put("timestamp", Instant.now().toString());
            
            log.info("🔔 Broadcasting table status change: {} tables -> {} (DB: {}, Display: {})", 
                tableIds.size(), displayStatus, newStatus, displayStatus);
            log.info("📋 Table IDs: {}", tableIds);
            
            messagingTemplate.convertAndSend("/topic/table-status", message);
            
            log.info("✅ Broadcast sent successfully to /topic/table-status");
            
        } catch (Exception e) {
            log.error("❌ Failed to broadcast table status change: {}", e.getMessage(), e);
        }
    }
}
