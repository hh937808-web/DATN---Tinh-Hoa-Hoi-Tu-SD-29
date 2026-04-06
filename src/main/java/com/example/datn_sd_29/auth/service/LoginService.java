package com.example.datn_sd_29.auth.service;

import com.example.datn_sd_29.auth.dto.LoginRequest;
import com.example.datn_sd_29.auth.dto.LoginResponse;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.repository.EmployeeRepository;
import com.example.datn_sd_29.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {
    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Customer login - ONLY accepts EMAIL
     * Employees CANNOT login through this endpoint
     */
    public LoginResponse customerLogin(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("🔐 Customer login attempt for email: {}", email);

        // Find customer by email
        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

        if (Boolean.FALSE.equals(customer.getIsActive())) {
            log.warn("🚫 Customer account is locked");
            throw new IllegalArgumentException("Tài khoản này đã bị khóa");
        }

        String encodePassword = customer.getPassword();
        if (encodePassword == null || !passwordEncoder.matches(request.getPassword(), encodePassword)) {
            log.warn("❌ Customer password mismatch");
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
        }

        // Generate JWT with USER role for customer
        String token = jwtService.generateTokenWithRole(customer.getEmail(), "USER", customer.getId());
        log.info("✅ Customer login successful");
        return new LoginResponse(
            token, 
            customer.getEmail(), 
            "USER", 
            customer.getId(), 
            customer.getFullName(), 
            customer.getEmail()
        );
    }

    /**
     * Employee login - ONLY accepts USERNAME
     * Customers CANNOT login through this endpoint
     */
    public LoginResponse employeeLogin(LoginRequest request) {
        String username = request.getEmail().trim().toLowerCase(); // Field name is 'email' but we use it as username
        log.info("🔐 Employee login attempt for username: {}", username);

        // Find employee by username
        Employee employee = employeeRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Username hoặc mật khẩu không đúng"));

        log.info("👤 Found employee: id={}, username={}, email={}, role={}", 
                employee.getId(), employee.getUsername(), employee.getEmail(), employee.getRole());
        
        if (Boolean.FALSE.equals(employee.getIsActive())) {
            log.warn("🚫 Employee account is locked");
            throw new IllegalArgumentException("Tài khoản này đã bị khóa");
        }

        String encodePassword = employee.getPassword();
        log.info("🔑 Password in DB: {} (length: {})", 
                encodePassword != null ? encodePassword.substring(0, Math.min(20, encodePassword.length())) + "..." : "NULL",
                encodePassword != null ? encodePassword.length() : 0);
        log.info("🔑 Input password: {} (length: {})", request.getPassword(), request.getPassword().length());
        
        boolean passwordMatches = encodePassword != null && passwordEncoder.matches(request.getPassword(), encodePassword);
        log.info("✅ Password matches: {}", passwordMatches);
        
        if (!passwordMatches) {
            log.warn("❌ Employee password mismatch");
            throw new IllegalArgumentException("Username hoặc mật khẩu không đúng");
        }

        // Generate JWT with role for employee
        String token = jwtService.generateTokenWithRole(employee.getEmail(), employee.getRole(), employee.getId());
        log.info("✅ Employee login successful, token generated");
        return new LoginResponse(
            token, 
            employee.getEmail(), 
            employee.getRole(), 
            employee.getId(), 
            employee.getFullName(), 
            employee.getUsername()
        );
    }

    /**
     * @deprecated Use customerLogin() or employeeLogin() instead
     * Legacy method for backward compatibility
     */
    @Deprecated
    public LoginResponse login(LoginRequest request) {
        // Try employee first, then customer
        String normalizedInput = request.getEmail().trim().toLowerCase();
        
        Optional<Employee> employeeOpt = employeeRepository.findByUsernameIgnoreCase(normalizedInput);
        if (employeeOpt.isPresent()) {
            return employeeLogin(request);
        }
        
        return customerLogin(request);
    }
}
