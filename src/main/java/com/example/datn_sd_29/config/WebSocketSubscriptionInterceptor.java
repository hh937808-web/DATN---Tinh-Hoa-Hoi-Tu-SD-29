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
 * STOMP Subscription Interceptor — phân quyền per-topic.
 *
 * Map roles cho từng topic dựa trên thực tế FE subscribe:
 *   /topic/dashboard-stats   → ADMIN, RECEPTION (admin xem dashboard, lễ tân xem tổng quan)
 *   /topic/invoice-updates   → ADMIN, RECEPTION, STAFF (vận hành đều cần biết hóa đơn thay đổi)
 *   /topic/table-status      → ADMIN, RECEPTION, STAFF, KITCHEN (ai cũng cần biết bàn nào trống/đầy)
 *   /topic/noshow            → ADMIN, RECEPTION, STAFF (đặt bàn no-show + tự hủy)
 *   /topic/kitchen-updates   → ADMIN, RECEPTION, STAFF, KITCHEN (món order/done/cancel cần broadcast cho staff bưng)
 *
 * USER (customer) hiện chưa subscribe topic nào — nếu sau này có sẽ thêm.
 */
@Slf4j
@Component
public class WebSocketSubscriptionInterceptor implements ChannelInterceptor {

    private static final Map<String, Set<String>> TOPIC_ROLES = Map.of(
            "/topic/dashboard-stats", Set.of("ADMIN", "RECEPTION"),
            "/topic/invoice-updates", Set.of("ADMIN", "RECEPTION", "STAFF"),
            "/topic/table-status",    Set.of("ADMIN", "RECEPTION", "STAFF", "KITCHEN"),
            "/topic/noshow",          Set.of("ADMIN", "RECEPTION", "STAFF"),
            "/topic/kitchen-updates", Set.of("ADMIN", "RECEPTION", "STAFF", "KITCHEN")
    );

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (destination == null || sessionAttributes == null) {
            log.warn("WebSocket SUBSCRIBE rejected: missing destination or session attributes");
            throw new IllegalArgumentException("Invalid subscription request");
        }

        String email = (String) sessionAttributes.get("email");
        String role = (String) sessionAttributes.get("role");

        if (email == null || role == null) {
            log.warn("WebSocket SUBSCRIBE rejected: user not authenticated (no email/role in session)");
            throw new IllegalArgumentException("User not authenticated");
        }

        Set<String> allowedRoles = TOPIC_ROLES.get(destination);
        if (allowedRoles != null && !allowedRoles.contains(role)) {
            log.warn("WebSocket SUBSCRIBE rejected: user={} role={} không có quyền với topic {}",
                    email, role, destination);
            throw new IllegalArgumentException("Access denied for topic " + destination);
        }
        // Topic không nằm trong map → cho phép (topic tự do, dành cho mở rộng tương lai)

        log.info("WebSocket SUBSCRIBE authorized: user={}, role={}, topic={}", email, role, destination);
        return message;
    }
}
