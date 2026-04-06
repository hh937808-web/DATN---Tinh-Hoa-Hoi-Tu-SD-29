package com.example.datn_sd_29.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceBroadcastService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Broadcast invoice status change to WebSocket clients
     */
    public void broadcastInvoiceUpdate(Integer invoiceId, String invoiceCode, String status) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("invoiceId", invoiceId);
            message.put("invoiceCode", invoiceCode);
            message.put("status", status);
            message.put("timestamp", Instant.now().toString());
            
            log.info("📢 Broadcasting invoice update: invoiceId={}, status={}", invoiceId, status);
            
            messagingTemplate.convertAndSend("/topic/invoice-updates", message);
            
            log.info("✅ Invoice update broadcast sent successfully");
        } catch (Exception e) {
            log.error("❌ Failed to broadcast invoice update: {}", e.getMessage(), e);
        }
    }
}
