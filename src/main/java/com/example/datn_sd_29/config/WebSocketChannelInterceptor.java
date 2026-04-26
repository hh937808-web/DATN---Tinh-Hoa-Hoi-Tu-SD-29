package com.example.datn_sd_29.config;

import com.example.datn_sd_29.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * STOMP Channel Interceptor — auth ở STOMP layer thay vì HandshakeInterceptor.
 *
 * Lý do: SockJS dùng nhiều transport (xhr_streaming, eventsource, ws...) và mỗi
 * transport tạo URL phụ không kế thừa query string. Auth qua URL handshake sẽ
 * fail. Thay vào đó, FE gửi token qua connectHeaders.Authorization của STOMP
 * CONNECT frame — interceptor này đọc và validate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Value("${security.api.enabled:true}")
    private boolean securityEnabled;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        // Security tắt — cho qua, gắn dev user
        if (!securityEnabled) {
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("email", "dev-user");
                accessor.getSessionAttributes().put("role", "ADMIN");
            }
            log.debug("STOMP CONNECT accepted (security disabled)");
            return message;
        }

        // Security bật — đọc Authorization từ STOMP CONNECT frame
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            log.warn("STOMP CONNECT rejected: missing Authorization header");
            throw new IllegalArgumentException("Missing Authorization header");
        }

        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("STOMP CONNECT rejected: Authorization not Bearer format");
            throw new IllegalArgumentException("Invalid Authorization format");
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            log.warn("STOMP CONNECT rejected: invalid/expired token");
            throw new IllegalArgumentException("Invalid token");
        }

        String email = jwtService.extractSubject(token);
        String role = jwtService.extractRole(token);

        if (role == null) {
            log.warn("STOMP CONNECT rejected: token thiếu role claim");
            throw new IllegalArgumentException("Token missing role");
        }

        // Cho phép STAFF, RECEPTION, KITCHEN, ADMIN, USER (USER có thể cần subscribe khi book)
        if (!"STAFF".equals(role) && !"RECEPTION".equals(role)
                && !"KITCHEN".equals(role) && !"ADMIN".equals(role)
                && !"USER".equals(role)) {
            log.warn("STOMP CONNECT rejected: role {} không được phép", role);
            throw new IllegalArgumentException("Role not authorized");
        }

        if (accessor.getSessionAttributes() != null) {
            accessor.getSessionAttributes().put("email", email);
            accessor.getSessionAttributes().put("role", role);
        }

        log.info("STOMP CONNECT accepted: user={}, role={}", email, role);
        return message;
    }
}
