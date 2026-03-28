package com.example.datn_sd_29.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * STOMP Channel Interceptor for passing authenticated user info to STOMP session
 * User is already authenticated at handshake level by WebSocketAuthInterceptor
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        // Pass user info from handshake attributes to STOMP session
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                String email = (String) sessionAttributes.get("email");
                String role = (String) sessionAttributes.get("role");
                
                if (email != null && role != null) {
                    log.info("STOMP CONNECT: user={}, role={}", email, role);
                } else {
                    log.warn("STOMP CONNECT: No user info in session (authentication may have failed)");
                }
            }
        }
        
        return message;
    }
}
