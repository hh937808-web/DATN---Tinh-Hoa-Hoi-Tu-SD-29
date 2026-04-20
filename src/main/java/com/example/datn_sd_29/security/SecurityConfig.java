package com.example.datn_sd_29.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = false)
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    
    @Value("${security.api.enabled:true}")
    private boolean securityEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Development mode - tất cả APIs public, KHÔNG add JWT filter
        if (!securityEnabled) {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(cors -> cors.configure(http))
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll()
                    );
            // KHÔNG add jwtAuthFilter khi security tắt
            return http.build();
        }
        
        // Production mode - role-based security với JWT filter
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configure(http))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // ========================================
                        // PUBLIC ENDPOINTS - Không cần authentication
                        // ========================================
                        .requestMatchers(
                                "/api/auth/**",           // Authentication endpoints
                                "/api/public/**",         // Public notification endpoints
                                "/public/**",             // Direct public access
                                "/ws/**",                 // WebSocket endpoints (authentication handled by WebSocketAuthInterceptor)
                                "/error"                  // Error page
                        ).permitAll()
                        
                        // ========================================
                        // CUSTOMER ENDPOINTS - Phân quyền rõ ràng
                        // ========================================
                        // ADMIN only - Customer management
                        .requestMatchers("/api/customers/search").hasRole("ADMIN")
                        .requestMatchers("/api/customers/sort").hasRole("ADMIN")
                        .requestMatchers("/api/customers/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/customers").hasRole("ADMIN")
                        
                        // Authenticated USER - Customer profile
                        .requestMatchers("/api/customers/profile").authenticated()
                        
                        // ========================================
                        // PUBLIC GET ENDPOINTS - Khách xem menu (không cần đăng nhập)
                        // ========================================
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-combos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-combo-vouchers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-vouchers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/customer-vouchers/**").permitAll()
                        
                        // ========================================
                        // ADMIN ONLY - Management Endpoints (ĐẶT TRƯỚC để ưu tiên)
                        // ========================================
                        .requestMatchers("/api/debug/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")
                        
                        // Employee Management - ADMIN ONLY
                        .requestMatchers("/api/employees/**").hasRole("ADMIN")
                        
                        // Product Management - ADMIN ONLY
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/product-combos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/product-combos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/product-combos/**").hasRole("ADMIN")
                        .requestMatchers("/api/product-combo-items/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/images/combo/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/images/product/**").hasRole("ADMIN")
                        
                        // Voucher Management - ADMIN ONLY
                        .requestMatchers(HttpMethod.POST, "/api/product-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/product-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/product-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/product-combo-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/product-combo-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/product-combo-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/customer-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/customer-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/customer-vouchers/**").hasRole("ADMIN")
                        
                        // ========================================
                        // ADMIN + RECEPTION - Dashboard & Reports
                        // ========================================
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers("/api/query-builder/**").hasAnyRole("ADMIN", "RECEPTION")
                        
                        // ========================================
                        // RECEPTION + ADMIN - Reception Specific Endpoints
                        // ========================================
                        .requestMatchers("/api/reception/**").hasAnyRole("RECEPTION", "ADMIN")
                        .requestMatchers("/api/walk-in/**").hasAnyRole("RECEPTION", "ADMIN")
                        
                        // Reservation Management - RECEPTION + ADMIN
                        .requestMatchers("/api/reservation/all").hasAnyRole("RECEPTION", "ADMIN")
                        .requestMatchers("/api/reservation/pending").hasAnyRole("RECEPTION", "ADMIN")
                        .requestMatchers("/api/reservation/*/confirm").hasAnyRole("RECEPTION", "ADMIN")
                        .requestMatchers("/api/reservation/*/alternative-tables").hasAnyRole("RECEPTION", "ADMIN")
                        .requestMatchers("/api/reservation/*/recommended-tables").hasAnyRole("RECEPTION", "ADMIN")
                        .requestMatchers("/api/reservation/*/reassign-tables").hasAnyRole("RECEPTION", "ADMIN")
                        .requestMatchers("/api/reservation/*/check-in").hasAnyRole("RECEPTION", "ADMIN")
                        .requestMatchers("/api/reservation/*/cancel").hasAnyRole("RECEPTION", "ADMIN")
                        .requestMatchers("/api/reservation/search").hasAnyRole("RECEPTION", "ADMIN")
                        
                        // ========================================
                        // KITCHEN + ADMIN - Kitchen Management
                        // ========================================
                        .requestMatchers("/api/kitchen/**").hasAnyRole("KITCHEN", "ADMIN")
                        
                        // ========================================
                        // STAFF + RECEPTION + ADMIN - Invoice & Table Management
                        // ĐẶT SAU các rule cụ thể để tránh conflict
                        // ========================================
                        .requestMatchers("/api/invoices/**").hasAnyRole("STAFF", "RECEPTION", "ADMIN")
                        .requestMatchers("/api/tables/**").hasAnyRole("STAFF", "RECEPTION", "ADMIN")
                        
                        // ========================================
                        // USER (Customer) - Authenticated users
                        // ========================================
                        .requestMatchers("/api/reservation/**").authenticated()  // Đặt bàn (cần đăng nhập)
                        
                        // ========================================
                        // Tất cả các request khác cần authentication
                        // ========================================
                        .anyRequest().authenticated()
                )
                // CHỈ add JWT filter khi security được bật
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
