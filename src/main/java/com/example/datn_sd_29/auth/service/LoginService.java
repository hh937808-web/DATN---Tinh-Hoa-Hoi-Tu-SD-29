package com.example.datn_sd_29.auth.service;

import com.example.datn_sd_29.auth.dto.LoginRequest;
import com.example.datn_sd_29.auth.dto.LoginResponse;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Customer customer = customerRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

        if (Boolean.FALSE.equals(customer.getIsActive())) {
            throw new IllegalArgumentException("Tài khoản này đã bị khóa");
        }

        String encodePassword = passwordEncoder.encode(request.getPassword());
        if (encodePassword == null || !passwordEncoder.matches(request.getPassword(), encodePassword)) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");

        }

        String token = jwtService.generateToken(customer.getEmail());
        return new LoginResponse(token, customer.getEmail());
    }
}
