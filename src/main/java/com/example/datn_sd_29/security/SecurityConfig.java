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
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    
    @Value("${security.api.enabled:true}")
    private boolean securityEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Development mode - tất cả APIs public
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
            return http.build();
        }
        
        // Production mode - role-based security
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
                                "/error",                 // Error page
                                "/api/kitchen/**"         // Kitchen staff update status
                        ).permitAll()
                        
                        // Public GET endpoints - Khách xem menu (không cần đăng nhập)
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-combos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-combo-vouchers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-vouchers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/customer-vouchers/**").permitAll()
                        
                        // ========================================
                        // ADMIN + RECEPTION - Dashboard & Reports
                        // ========================================
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "RECEPTION")
                        
                        // ========================================
                        // ADMIN ONLY
                        // ========================================
                        .requestMatchers("/api/debug/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")
                        
                        // Product Management
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/product-combos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/product-combos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/product-combos/**").hasRole("ADMIN")
                        .requestMatchers("/api/product-combo-items/**").hasRole("ADMIN")
                        
                        // Voucher Management
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
                        // RECEPTION ONLY
                        // ========================================
                        .requestMatchers("/api/reception/payment/**").hasRole("RECEPTION")
                        .requestMatchers("/api/walk-in/**").hasRole("RECEPTION")
                        .requestMatchers("/api/reservation/all").hasRole("RECEPTION")  // Xem tất cả đặt bàn
                        
                        // ========================================
                        // STAFF + RECEPTION - Overtime Alerts
                        // ========================================
                        .requestMatchers("/api/overtime/alerts/**").hasAnyRole("STAFF", "RECEPTION")
                        
                        // ========================================
                        // STAFF ONLY
                        // ========================================
                        .requestMatchers("/api/tables/**").hasRole("STAFF")
                        
                        // ========================================
                        // USER (Customer) - Authenticated users
                        // ========================================
                        .requestMatchers("/api/reservation/**").authenticated()  // Đặt bàn (cần đăng nhập)
                        
                        // ========================================
                        // Tất cả các request khác cần authentication
                        // ========================================
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
