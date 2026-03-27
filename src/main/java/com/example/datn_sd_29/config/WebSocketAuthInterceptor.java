package com.example.datn_sd_29.config;

import com.example.datn_sd_29.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket Handshake Interceptor for JWT authentication
 * Extracts and validates JWT token from query parameter during SockJS handshake
 * This is the industry-standard approach for SockJS authentication
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        
        log.info("WebSocket handshake initiated");
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String token = servletRequest.getServletRequest().getParameter("token");
            
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket handshake rejected: No token provided");
                return false; // Reject handshake
            }
            
            // Validate token
            try {
                if (!jwtService.isTokenValid(token)) {
                    log.warn("WebSocket handshake rejected: Invalid token");
                    return false;
                }
                
                // Extract user information
                String email = jwtService.extractSubject(token);
                String role = jwtService.extractRole(token);
                
                if (email == null || role == null) {
                    log.warn("WebSocket handshake rejected: Invalid token claims");
                    return false;
                }
                
                // Verify role
                if (!"STAFF".equals(role) && !"RECEPTION".equals(role) && !"ADMIN".equals(role)) {
                    log.warn("WebSocket handshake rejected: User {} with role {} not authorized", email, role);
                    return false;
                }
                
                // Store user info in session attributes for later use
                attributes.put("email", email);
                attributes.put("role", role);
                
                log.info("WebSocket handshake accepted: user={}, role={}", email, role);
                return true; // Accept handshake
                
            } catch (Exception e) {
                log.error("WebSocket handshake error: {}", e.getMessage());
                return false;
            }
        }
        
        log.warn("WebSocket handshake rejected: Invalid request type");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake failed", exception);
        }
    }
}
