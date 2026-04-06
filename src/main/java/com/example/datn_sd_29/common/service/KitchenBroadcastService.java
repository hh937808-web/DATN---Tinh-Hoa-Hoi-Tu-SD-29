package com.example.datn_sd_29.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for broadcasting kitchen updates via WebSocket
 * Notifies kitchen staff when items are ordered, cancelled, or status changed
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KitchenBroadcastService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Broadcast kitchen update when items are added/cancelled/status changed
     * 
     * @param action Action type: "ITEM_ORDERED", "ITEM_CANCELLED", "STATUS_CHANGED"
     * @param itemId Invoice item ID
     * @param itemName Item name
     */
    public void broadcastKitchenUpdate(String action, Integer itemId, String itemName) {
        try {
            log.info("📢 Broadcasting kitchen update: action={}, itemId={}, itemName={}", 
                    action, itemId, itemName);
            
            Map<String, Object> message = new HashMap<>();
            message.put("action", action);
            message.put("itemId", itemId);
            message.put("itemName", itemName);
            message.put("timestamp", LocalDateTime.now().toString());
            
            messagingTemplate.convertAndSend("/topic/kitchen-updates", message);
            
            log.info("✅ Kitchen update broadcast sent successfully");
        } catch (Exception e) {
            log.error("❌ Failed to broadcast kitchen update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Broadcast bulk kitchen update (for multiple items ordered at once)
     */
    public void broadcastBulkKitchenUpdate(String action, int itemCount) {
        try {
            log.info("📢 Broadcasting bulk kitchen update: action={}, itemCount={}", 
                    action, itemCount);
            
            Map<String, Object> message = new HashMap<>();
            message.put("action", action);
            message.put("itemCount", itemCount);
            message.put("timestamp", LocalDateTime.now().toString());
            
            messagingTemplate.convertAndSend("/topic/kitchen-updates", message);
            
            log.info("✅ Bulk kitchen update broadcast sent successfully");
        } catch (Exception e) {
            log.error("❌ Failed to broadcast bulk kitchen update: {}", e.getMessage(), e);
        }
    }
}
