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
     * Broadcasts table status change to all connected clients via WebSocket
     * @param tableIds List of table IDs that changed status
     * @param newStatus The new status of the tables
     */
    public void broadcastTableStatusChange(List<Integer> tableIds, String newStatus) {
        if (tableIds == null || tableIds.isEmpty()) {
            return;
        }
        
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("tableIds", tableIds);
            message.put("status", newStatus);
            message.put("timestamp", Instant.now().toString());
            
            messagingTemplate.convertAndSend("/topic/table-status", message);
            
            log.debug("Broadcasted table status change: {} tables -> {}", tableIds.size(), newStatus);
            
        } catch (Exception e) {
            log.error("Failed to broadcast table status change: {}", e.getMessage(), e);
        }
    }
}
