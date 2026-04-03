package com.example.datn_sd_29.customer.service;

import com.example.datn_sd_29.customer.dto.CustomerListResponse;
import com.example.datn_sd_29.customer.dto.CustomerProfileResponse;
import com.example.datn_sd_29.customer.dto.UpdateProfileRequest;
import com.example.datn_sd_29.customer.dto.UpdateProfileResponse;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final JwtService jwtService;

    // ========================
    // GET ALL CUSTOMERS (FOR ADMIN)
    // ========================
    public List<CustomerListResponse> getAll() {
        return customerRepository.findAll()
                .stream()
                .map(CustomerListResponse::new)
                .toList();
    }

    // ========================
    // GET PROFILE
    // ========================
    public CustomerProfileResponse getProfile(String token) {

        Integer customerId = jwtService.extractCustomerId(token);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return CustomerProfileResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .dateOfBirth(customer.getDateOfBirth())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .createdAt(customer.getCreatedAt())
                .isActive(customer.getIsActive())
                .build();
    }

    // ========================
    // UPDATE PROFILE
    // ========================
    public UpdateProfileResponse updateProfile(String token, UpdateProfileRequest request) {

        Integer customerId = jwtService.extractCustomerId(token);
        String oldEmail = jwtService.extractEmail(token);
        String role = jwtService.extractRole(token);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isEmailChanged = false;

        // ========================
        // UPDATE EMAIL
        // ========================
        if (request.getEmail() != null && !request.getEmail().equals(oldEmail)) {

            boolean exists = customerRepository.existsByEmail(request.getEmail());
            if (exists) {
                throw new RuntimeException("Email already exists");
            }

            customer.setEmail(request.getEmail());
            isEmailChanged = true;
        }

        // ========================
        // UPDATE OTHER FIELDS
        // ========================
        if (request.getFullName() != null) {
            customer.setFullName(request.getFullName());
        }

        if (request.getPhoneNumber() != null) {
            customer.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getDateOfBirth() != null) {
            customer.setDateOfBirth(request.getDateOfBirth());
        }

        customerRepository.save(customer);

        String newToken = null;

        // ========================
        // 🔥 EMAIL ĐỔI → TOKEN MỚI
        // ========================
        if (isEmailChanged) {
            newToken = jwtService.generateToken(
                    customer.getEmail(),
                    customer.getId(),
                    role
            );
        }

        return UpdateProfileResponse.builder()
                .message("Update successful")
                .accessToken(newToken)
                .build();
    }
}