package com.example.datn_sd_29.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * STOMP Subscription Interceptor for topic-level authorization
 * Enforces role-based access control for different WebSocket topics
 * User is already authenticated at handshake level
 */
@Slf4j
@Component
public class WebSocketSubscriptionInterceptor implements ChannelInterceptor {

    // Admin-only topics
    private static final Set<String> ADMIN_ONLY_TOPICS = Set.of(
        "/topic/dashboard-stats",
        "/topic/invoice-updates"
    );
    
    // Topics accessible by ADMIN, RECEPTION, and STAFF
    private static final Set<String> STAFF_ACCESSIBLE_TOPICS = Set.of(
        "/topic/table-status",
        "/topic/overtime-alerts"
    );

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            
            if (destination == null || sessionAttributes == null) {
                log.warn("WebSocket SUBSCRIBE rejected: Missing destination or session attributes");
                throw new IllegalArgumentException("Invalid subscription request");
            }
            
            String email = (String) sessionAttributes.get("email");
            String role = (String) sessionAttributes.get("role");
            
            if (email == null || role == null) {
                log.warn("WebSocket SUBSCRIBE rejected: User not authenticated");
                throw new IllegalArgumentException("User not authenticated");
            }
            
            // Check authorization for admin-only topics
            if (ADMIN_ONLY_TOPICS.contains(destination)) {
                if (!"ADMIN".equals(role)) {
                    log.warn("WebSocket SUBSCRIBE rejected: User {} with role {} attempted to access admin-only topic {}", 
                            email, role, destination);
                    throw new IllegalArgumentException("Access denied: Admin role required");
                }
            }
            // Check authorization for staff-accessible topics
            else if (STAFF_ACCESSIBLE_TOPICS.contains(destination)) {
                if (!"ADMIN".equals(role) && !"RECEPTION".equals(role) && !"STAFF".equals(role)) {
                    log.warn("WebSocket SUBSCRIBE rejected: User {} with role {} attempted to access staff topic {}", 
                            email, role, destination);
                    throw new IllegalArgumentException("Access denied: Staff role required");
                }
            }
            
            log.info("WebSocket SUBSCRIBE authorized: user={}, role={}, topic={}", email, role, destination);
        }
        
        return message;
    }
}
