package com.example.datn_sd_29.payment.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller xử lý notification thanh toán QR code
 * Endpoint này là public (không cần JWT) để điện thoại khách có thể gọi
 */
@RestController
public class SimpleNotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleNotificationController.class);
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/payment-notifications/";
    private static final long NOTIFICATION_TIMEOUT_MS = 30000; // 30 giây
    
    public SimpleNotificationController() {
        File dir = new File(TEMP_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            logger.info("Created notification temp directory: {} - Success: {}", TEMP_DIR, created);
        }
    }
    
    /**
     * Endpoint để điện thoại gửi notification khi chuyển khoản thành công
     * Public endpoint - không cần authentication
     */
    @PostMapping({"/public/notification/send/{tableId}", "/api/public/notification/send/{tableId}"})
    public ResponseEntity<Map<String, Object>> sendNotification(
            @PathVariable Long tableId, 
            @RequestBody Map<String, Object> data) {
        
        logger.info("Received payment notification for table {}: {}", tableId, data);
        
        try {
            File file = new File(TEMP_DIR + "table_" + tableId + ".txt");
            
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(data.get("amount").toString() + "\n");
                writer.write(System.currentTimeMillis() + "");
            }
            
            logger.info("Notification saved successfully for table {}", tableId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification sent");
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            logger.error("Error saving notification for table {}: {}", tableId, e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to save notification");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Endpoint để máy tính polling kiểm tra notification
     * Public endpoint - không cần authentication
     */
    @GetMapping({"/public/notification/check/{tableId}", "/api/public/notification/check/{tableId}"})
    public ResponseEntity<Map<String, Object>> checkNotification(@PathVariable Long tableId) {
        
        try {
            File file = new File(TEMP_DIR + "table_" + tableId + ".txt");
            
            if (file.exists()) {
                String content = new String(Files.readAllBytes(file.toPath()));
                String[] lines = content.split("\n");
                
                if (lines.length < 2) {
                    logger.warn("Invalid notification file format for table {}", tableId);
                    file.delete();
                    return createNoNotificationResponse();
                }
                
                long timestamp = Long.parseLong(lines[1]);
                long now = System.currentTimeMillis();
                
                // Chỉ trả về nếu notification mới (trong vòng 30 giây)
                if (now - timestamp < NOTIFICATION_TIMEOUT_MS) {
                    // Xóa file sau khi đọc để tránh xử lý lại
                    file.delete();
                    
                    logger.info("Found valid notification for table {}, amount: {}", tableId, lines[0]);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("hasNotification", true);
                    response.put("amount", Double.parseDouble(lines[0]));
                    response.put("tableId", tableId);
                    return ResponseEntity.ok(response);
                } else {
                    // Xóa file cũ đã hết hạn
                    logger.info("Notification expired for table {}, deleting file", tableId);
                    file.delete();
                }
            }
            
            return createNoNotificationResponse();
            
        } catch (Exception e) {
            logger.error("Error checking notification for table {}: {}", tableId, e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error checking notification");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private ResponseEntity<Map<String, Object>> createNoNotificationResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("hasNotification", false);
        return ResponseEntity.ok(response);
    }
}
