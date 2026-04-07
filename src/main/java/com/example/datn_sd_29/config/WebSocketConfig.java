package com.example.datn_sd_29.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket configuration for real-time updates
 * Implements STOMP protocol over WebSocket with SockJS fallback
 * Uses STOMP CONNECT header authentication and topic-level authorization
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketChannelInterceptor channelInterceptor;
    private final WebSocketSubscriptionInterceptor subscriptionInterceptor;
    private final WebSocketAuthInterceptor authInterceptor;
    
    // Track active WebSocket connections (Requirement 10.2)
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    @Value("${spring.websocket.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for broadcasting messages to subscribed clients
        config.enableSimpleBroker("/topic");
        
        // Set prefix for messages from clients to server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register interceptors for STOMP CONNECT authentication and SUBSCRIBE authorization
        registration.interceptors(channelInterceptor, subscriptionInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register general WebSocket endpoint for admin dashboard
        // Authentication happens at handshake level via query parameter
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.split(","))
                .addInterceptors(authInterceptor)
                .withSockJS();
    }
    
    /**
     * Handles WebSocket connection events
     * Logs connection establishment and tracks active connection count
     * Requirement 10.2
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        int currentConnections = activeConnections.incrementAndGet();
        String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        log.info("WebSocket connection established - Session: {}, Active connections: {}", 
                sessionId, currentConnections);
    }
    
    /**
     * Handles WebSocket disconnection events
     * Logs disconnection and tracks active connection count
     * Requirement 10.2
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        int currentConnections = activeConnections.decrementAndGet();
        String sessionId = event.getSessionId();
        log.info("WebSocket connection closed - Session: {}, Active connections: {}", 
                sessionId, currentConnections);
        
        // Log warning if connection count drops significantly
        if (currentConnections == 0) {
            log.warn("All WebSocket connections have been closed - no active staff sessions");
        }
    }
    
    /**
     * Returns the current count of active WebSocket connections
     * Requirement 10.2
     */
    public int getActiveConnectionCount() {
        return activeConnections.get();
    }
}

