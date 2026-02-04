package com.example.datn_sd_29.service;

import com.example.datn_sd_29.dto.RegisterRequest;
import com.example.datn_sd_29.dto.RegisterResponse;
import com.example.datn_sd_29.entity.Customer;
import com.example.datn_sd_29.repository.CustomerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;

@Service
public class RegisterService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
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
}
