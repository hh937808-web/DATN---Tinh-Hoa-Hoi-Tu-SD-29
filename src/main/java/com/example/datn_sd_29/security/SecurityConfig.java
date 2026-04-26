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

/**
 * Security config — phân quyền đầy đủ cho mọi endpoint trong dự án.
 *
 * security.api.enabled = false (DEV / hiện tại):
 *   → permitAll, JwtAuthFilter KHÔNG được add → mọi request public.
 *   → Audit aspect dựa vào header X-Employee-Username / X-Customer-Email
 *     để định danh (đã có sẵn trong AutoAuditAspect).
 *
 * security.api.enabled = true (PROD):
 *   → JwtAuthFilter được add → request không có Bearer hợp lệ sẽ 401.
 *   → Mọi rule role-based bên dưới có hiệu lực.
 *
 * THỨ TỰ KHỚP RULE:
 *   Spring Security duyệt từ trên xuống, dùng MATCH ĐẦU TIÊN. Vì vậy:
 *     - Rule cụ thể (path/method hẹp) phải đặt TRƯỚC rule rộng.
 *     - permitAll cho path con phải đặt TRƯỚC requestMatcher cha hạn chế.
 *     - * khớp 1 segment, ** khớp nhiều segment.
 */
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
        // ============================================================
        // DEV MODE — security.api.enabled=false
        // ============================================================
        if (!securityEnabled) {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(cors -> cors.configure(http))
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            // KHÔNG add jwtAuthFilter — giữ FE đang gửi header X-Employee-Username
            // hoạt động như bình thường, audit aspect tự đọc header.
            return http.build();
        }

        // ============================================================
        // PROD MODE — phân quyền đầy đủ theo từng controller/endpoint
        // ============================================================
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configure(http))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // ====================================================
                        // 0. ADMIN-only paths có nguy cơ conflict với public wildcard
                        //    → đặt LÊN ĐẦU để được match trước.
                        //    VD: /api/blog/admin (1 segment) trùng pattern /api/blog/* public
                        //    nếu để sau sẽ bị permitAll nuốt.
                        // ====================================================
                        .requestMatchers("/api/blog/admin/**").hasRole("ADMIN")

                        // ====================================================
                        // 1. PUBLIC — không cần đăng nhập
                        // ====================================================
                        // Auth (login/register/otp) + websocket + error page
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**", "/public/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Chatbot — cho phép cả khách chưa đăng nhập
                        .requestMatchers("/api/chatbot/**").permitAll()

                        // Menu công khai (sản phẩm + combo)
                        .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-combos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-combos/active").permitAll()

                        // Ảnh sản phẩm/combo/blog — read công khai
                        .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()

                        // Voucher — list công khai (CRUD chặn ở block ADMIN bên dưới)
                        .requestMatchers(HttpMethod.GET, "/api/customer-vouchers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/customer-vouchers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-vouchers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-vouchers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-combo-vouchers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-combo-vouchers/**").permitAll()

                        // Blog public (đọc bài đã publish)
                        .requestMatchers(HttpMethod.GET, "/api/blog/published").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blog/published/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blog/*").permitAll() // /api/blog/{id}

                        // Reviews đã duyệt — đọc công khai
                        .requestMatchers(HttpMethod.GET, "/api/reviews").permitAll()

                        // Reservation — flow public (xem bàn trống, đặt, tra cứu mã)
                        .requestMatchers(HttpMethod.GET, "/api/reservation/available").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reservation").permitAll()
                        // /api/reservation/{code} GET — public lookup
                        // CHÚ Ý: rule "/api/reservation/all|pending|search" phải đặt TRƯỚC rule này
                        // (đặt ở block ADMIN+RECEPTION xử lý trước nên OK).

                        // ====================================================
                        // 2. ADMIN ONLY
                        // ====================================================
                        // Audit log
                        .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")

                        // Debug + test endpoints
                        .requestMatchers("/api/debug/**").hasRole("ADMIN")
                        .requestMatchers("/api/test/**").hasRole("ADMIN")
                        .requestMatchers("/api/dashboard/test/**").hasRole("ADMIN")

                        // Báo cáo doanh thu/sản phẩm
                        .requestMatchers("/api/reports/**").hasRole("ADMIN")

                        // Query builder (chạy SQL tùy chỉnh — nhạy cảm)
                        .requestMatchers("/api/query-builder/**").hasRole("ADMIN")

                        // Quản lý nhân viên
                        .requestMatchers("/api/employees/**").hasRole("ADMIN")

                        // Blog admin đã được match ở block 0 (đặt trên cùng để tránh conflict với /api/blog/* public)

                        // Customer admin operations (search/sort/list/lock)
                        .requestMatchers(HttpMethod.GET, "/api/customers").hasRole("ADMIN")
                        .requestMatchers("/api/customers/search").hasRole("ADMIN")
                        .requestMatchers("/api/customers/sort").hasRole("ADMIN")
                        .requestMatchers("/api/customers/*/status").hasRole("ADMIN")

                        // Product CRUD + tools admin
                        .requestMatchers("/api/products/search").hasRole("ADMIN")
                        .requestMatchers("/api/products/sort").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // Product combo CRUD + combo items
                        .requestMatchers(HttpMethod.POST, "/api/product-combos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/product-combos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/product-combos/**").hasRole("ADMIN")
                        .requestMatchers("/api/product-combo-items/**").hasRole("ADMIN")

                        // Voucher CRUD
                        .requestMatchers(HttpMethod.POST, "/api/customer-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/customer-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/customer-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/product-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/product-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/product-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/product-combo-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/product-combo-vouchers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/product-combo-vouchers/**").hasRole("ADMIN")

                        // Image upload/delete (admin quản lý nội dung)
                        .requestMatchers(HttpMethod.POST, "/api/images/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/images/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/images/**").hasRole("ADMIN")

                        // Review moderation
                        .requestMatchers("/api/reviews/admin/**").hasRole("ADMIN")

                        // Dining table CRUD admin (read sẽ mở cho nhiều role bên dưới)
                        .requestMatchers("/api/tables/search").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tables").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tables/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/tables/**").hasRole("ADMIN")

                        // ====================================================
                        // 3. ADMIN + RECEPTION
                        // ====================================================
                        // Dashboard (đặt SAU /api/dashboard/test/** ADMIN-only ở trên)
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "RECEPTION")

                        // Reception flow (payment + walk-in)
                        .requestMatchers("/api/reception/**").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers("/api/walk-in/**").hasAnyRole("ADMIN", "RECEPTION")

                        // Reservation management (lễ tân duyệt/xếp bàn/hủy)
                        // Đặt TRƯỚC rule public /api/reservation/* GET để khớp đúng.
                        .requestMatchers("/api/reservation/all").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers("/api/reservation/pending").hasAnyRole("ADMIN", "RECEPTION")
                        // Search — staff cũng cần tra cứu reservation theo SĐT khi khách đến quầy
                        .requestMatchers("/api/reservation/search").hasAnyRole("ADMIN", "RECEPTION", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/reservation/*/confirm").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers(HttpMethod.POST, "/api/reservation/*/check-in").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers(HttpMethod.GET, "/api/reservation/*/alternative-tables").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers(HttpMethod.GET, "/api/reservation/*/recommended-tables").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers(HttpMethod.POST, "/api/reservation/*/reassign-tables").hasAnyRole("ADMIN", "RECEPTION")

                        // ====================================================
                        // 4. KITCHEN + ADMIN — vận hành bếp
                        // ====================================================
                        // Action "Phục vụ" (chuyển DONE → SERVED) là việc của staff bưng đồ ra bàn,
                        // không phải bếp. Đặt rule này TRƯỚC /api/kitchen/** để được match đúng.
                        .requestMatchers(HttpMethod.PUT, "/api/kitchen/items/*/serve")
                                .hasAnyRole("STAFF", "RECEPTION", "ADMIN")

                        // GET /api/kitchen/tables — staff cần xem trạng thái món để biết món nào
                        // sẵn sàng phục vụ. Cho phép cả KITCHEN, STAFF, RECEPTION, ADMIN đọc.
                        // Mở rộng path pattern cho an toàn (match cả có/không trailing).
                        .requestMatchers(HttpMethod.GET, "/api/kitchen/tables", "/api/kitchen/tables/**")
                                .hasAnyRole("KITCHEN", "STAFF", "RECEPTION", "ADMIN")

                        // Còn lại: start/done/cancel/list bếp — chỉ bếp + admin
                        .requestMatchers("/api/kitchen/**").hasAnyRole("KITCHEN", "ADMIN")

                        // ====================================================
                        // 5. STAFF + RECEPTION + ADMIN — order món + invoice vận hành
                        // ====================================================
                        .requestMatchers("/api/invoices/**").hasAnyRole("STAFF", "RECEPTION", "ADMIN")
                        .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "RECEPTION", "ADMIN")

                        // Tables endpoints dành cho staff vận hành
                        .requestMatchers("/api/tables/serving").hasAnyRole("STAFF", "RECEPTION", "ADMIN")
                        .requestMatchers("/api/tables/staff/**").hasAnyRole("STAFF", "RECEPTION", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tables/*/order-items").hasAnyRole("STAFF", "RECEPTION", "ADMIN")

                        // ====================================================
                        // 6. STAFF + RECEPTION + ADMIN + KITCHEN — read bàn
                        // ====================================================
                        // Tất cả role nhân viên cần xem danh sách bàn (kitchen cũng cần để biết bàn nào đang phục vụ)
                        .requestMatchers(HttpMethod.GET, "/api/tables").hasAnyRole("STAFF", "RECEPTION", "ADMIN", "KITCHEN")
                        .requestMatchers(HttpMethod.GET, "/api/tables/*").hasAnyRole("STAFF", "RECEPTION", "ADMIN", "KITCHEN")

                        // ====================================================
                        // 7. AUTHENTICATED (USER khách + bất kỳ role nhân viên đăng nhập)
                        // ====================================================
                        // Customer profile riêng
                        .requestMatchers("/api/customers/profile").authenticated()
                        .requestMatchers("/api/customers/profile/**").authenticated()

                        // Hóa đơn của customer (xem hóa đơn chính họ)
                        .requestMatchers("/api/invoice/**").authenticated()

                        // Voucher cá nhân của customer
                        .requestMatchers("/api/personal-vouchers/**").authenticated()

                        // Đăng review (customer đã đăng nhập)
                        .requestMatchers(HttpMethod.POST, "/api/reviews").authenticated()

                        // Hủy đặt bàn — cả customer và lễ tân/admin đều có thể hủy
                        // (BE phân biệt qua query param ?cancelledByStaff=true)
                        .requestMatchers(HttpMethod.POST, "/api/reservation/*/cancel").authenticated()

                        // Gửi lại email xác nhận đặt bàn — customer tự bấm gửi lại từ trang booking,
                        // hoặc lễ tân/staff gửi sau khi xếp bàn xong. Bất kỳ user đăng nhập đều OK.
                        .requestMatchers(HttpMethod.POST, "/api/reservation/*/send-email").authenticated()

                        // Reservation public lookup theo mã (đặt cuối cùng trong group reservation
                        // để các rule cụ thể hơn ở trên đã được match trước)
                        .requestMatchers(HttpMethod.GET, "/api/reservation/*").permitAll()

                        // ====================================================
                        // 8. DEFAULT — bất kỳ request khác phải đăng nhập
                        // ====================================================
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
