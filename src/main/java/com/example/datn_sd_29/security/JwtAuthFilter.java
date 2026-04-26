package com.example.datn_sd_29.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    
    @org.springframework.beans.factory.annotation.Value("${security.api.enabled:true}")
    private boolean securityEnabled;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Nếu security bị tắt, bỏ qua hoàn toàn JWT authentication
        if (!securityEnabled) {
            log.debug("Security DISABLED - Allowing request: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        log.info("[JWT-FILTER] {} {}", request.getMethod(), path);

        // Bỏ qua authentication cho các endpoint công khai
        if (isPublicPath(path)) {
            log.info("[JWT-FILTER] → public path, skip auth");
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            log.warn("[JWT-FILTER] → MISSING Authorization header (request will be anonymous)");
            filterChain.doFilter(request, response);
            return;
        }
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("[JWT-FILTER] → BAD Authorization format (expected 'Bearer xxx', got '{}')",
                    authHeader.length() > 30 ? authHeader.substring(0, 30) + "..." : authHeader);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            log.warn("[JWT-FILTER] → INVALID token (signature mismatch hoặc expired). Token len={}", token.length());
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.extractSubject(token);
        String role = jwtService.extractRole(token);  // Extract role from JWT

        if (role == null || role.isBlank()) {
            log.warn("[JWT-FILTER] → token VALID nhưng KHÔNG có role claim (subject={}). " +
                    "Có thể token cũ trước khi thêm role claim. User cần logout/login lại.", email);
            filterChain.doFilter(request, response);
            return;
        }

        // Create authority from role for Spring Security
        List<org.springframework.security.core.GrantedAuthority> authorities = List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role)
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, authorities);

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("[JWT-FILTER] → AUTH OK: subject={}, role=ROLE_{}", email, role);
        filterChain.doFilter(request, response);
    }
    
    /**
     * Kiểm tra xem path có phải là public endpoint không
     * Chỉ bỏ qua authentication cho các endpoint thực sự public
     * Các endpoint khác sẽ được xử lý bởi SecurityConfig
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/public/") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/api/auth/") ||
               path.startsWith("/ws/");
    }
}
