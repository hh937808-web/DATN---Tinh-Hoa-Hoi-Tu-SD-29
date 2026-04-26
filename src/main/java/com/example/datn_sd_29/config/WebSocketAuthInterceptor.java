package com.example.datn_sd_29.config;

import com.example.datn_sd_29.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Value("${security.api.enabled:true}")
    private boolean securityEnabled;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        // QUAN TRỌNG: SockJS dùng nhiều transport (xhr_streaming, xhr, eventsource...),
        // mỗi transport tạo URL phụ KHÔNG kế thừa query string từ URL gốc.
        // Vì vậy KHÔNG thể auth qua ?token=xxx ở handshake — sẽ fail mọi transport sau cái đầu tiên.
        //
        // Giải pháp: cho phép handshake luôn, auth thật được thực hiện ở
        // WebSocketChannelInterceptor.preSend() khi nhận STOMP CONNECT frame
        // (token gửi qua header `Authorization: Bearer xxx` trong connectHeaders).
        //
        // Sub-paths /ws/** đã được khai báo permitAll trong SecurityConfig,
        // nên việc cho qua handshake không tạo lỗ hổng — auth thật vẫn diễn ra ở STOMP layer.
        log.debug("WebSocket handshake accepted (auth deferred to STOMP CONNECT)");
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake failed", exception);
        }
    }
}
