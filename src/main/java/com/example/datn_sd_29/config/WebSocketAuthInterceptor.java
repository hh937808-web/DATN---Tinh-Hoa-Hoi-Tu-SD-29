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
 * WebSocket handshake interceptor for JWT authentication
 * Validates JWT token and extracts user information before establishing WebSocket connection
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) throws Exception {
        
        if (request instanceof ServletServerHttpRequest servletRequest) {
            // Extract JWT token from query parameter (WebSocket doesn't support headers in browser)
            String token = servletRequest.getServletRequest().getParameter("token");
            
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket handshake rejected: No token provided");
                return false;
            }

            // Validate token
            if (!jwtService.isTokenValid(token)) {
                log.warn("WebSocket handshake rejected: Invalid token");
                return false;
            }

            // Extract user information from token
            String email = jwtService.extractSubject(token);
            String role = jwtService.extractRole(token);

            // Verify user has STAFF or RECEPTION role
            if (!"STAFF".equals(role) && !"RECEPTION".equals(role)) {
                log.warn("WebSocket handshake rejected: User {} with role {} is not authorized", email, role);
                return false;
            }

            // Add user information to WebSocket session attributes
            attributes.put("email", email);
            attributes.put("role", role);
            
            log.info("WebSocket handshake successful for user: {} with role: {}", email, role);
            return true;
        }

        log.warn("WebSocket handshake rejected: Invalid request type");
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // No action needed after handshake
    }
}
