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
        broadcastKitchenUpdate(action, itemId, itemName, null);
    }

    /**
     * Broadcast với lý do (dùng cho ITEM_CANCELLED — gửi lý do cho staff hiển thị).
     * reason có thể null nếu không áp dụng.
     */
    public void broadcastKitchenUpdate(String action, Integer itemId, String itemName, String reason) {
        try {
            log.info("📢 Broadcasting kitchen update: action={}, itemId={}, itemName={}, reason={}",
                    action, itemId, itemName, reason);

            Map<String, Object> message = new HashMap<>();
            message.put("action", action);
            message.put("itemId", itemId);
            message.put("itemName", itemName);
            if (reason != null && !reason.isBlank()) {
                message.put("reason", reason);
            }
            message.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/kitchen-updates", message);

            log.info("✅ Kitchen update broadcast sent successfully");
        } catch (Exception e) {
            log.error("❌ Failed to broadcast kitchen update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Broadcast khi bếp làm xong 1 món — kèm tên bàn để staff biết đi phục vụ.
     */
    public void broadcastItemDone(Integer itemId, String itemName, String tableName) {
        try {
            log.info("📢 Broadcasting ITEM_DONE: itemId={}, itemName={}, tableName={}",
                    itemId, itemName, tableName);

            Map<String, Object> message = new HashMap<>();
            message.put("action", "ITEM_DONE");
            message.put("itemId", itemId);
            message.put("itemName", itemName);
            if (tableName != null && !tableName.isBlank()) {
                message.put("tableName", tableName);
            }
            message.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/kitchen-updates", message);
            log.info("✅ ITEM_DONE broadcast sent");
        } catch (Exception e) {
            log.error("❌ Failed to broadcast ITEM_DONE: {}", e.getMessage(), e);
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
