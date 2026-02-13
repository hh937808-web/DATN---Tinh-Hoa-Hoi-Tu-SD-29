package com.example.datn_sd_29.auth.service;

import com.example.datn_sd_29.auth.dto.LoginRequest;
import com.example.datn_sd_29.auth.dto.LoginResponse;
import com.example.datn_sd_29.auth.dto.RegisterRequest;
import com.example.datn_sd_29.auth.dto.RegisterResponse;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;

@Service
public class RegisterService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService
    ) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (customerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists!");
        }

        Customer customer = new Customer();
        customer.setFullName(request.getFullName().trim());
        customer.setEmail(normalizedEmail);
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setLoyaltyPoints(0);
        customer.setIsActive(true);
        customer.setCreatedAt(Instant.now());

        try {
            Customer saved = customerRepository.save(customer);
            return new RegisterResponse(saved.getId(), saved.getEmail());
        } catch (DataIntegrityViolationException ex) {
            Throwable cause = ex.getMostSpecificCause();
            if (cause instanceof SQLException sqlEx) {
                int errorCode = sqlEx.getErrorCode();
                if (errorCode == 2601 || errorCode == 2627) {
                    throw new IllegalArgumentException("Email already exists!");
                }
            }

            String rootMsg = cause != null ? cause.getMessage() : "";
            String msgLower = rootMsg == null ? "" : rootMsg.toLowerCase();
            if (msgLower.contains("email")) {
                throw new IllegalArgumentException("Email already exists!");
            }
            throw new IllegalArgumentException("Data violates constraint!");
        }
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Customer customer = customerRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

        if (Boolean.FALSE.equals(customer.getIsActive())) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa");
        }

        String encodedPassword = customer.getPassword();
        if (encodedPassword == null || !passwordEncoder.matches(request.getPassword(), encodedPassword)) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
        }

        String token = jwtService.generateToken(customer.getEmail());
        return new LoginResponse(token, customer.getEmail());
    }
}
