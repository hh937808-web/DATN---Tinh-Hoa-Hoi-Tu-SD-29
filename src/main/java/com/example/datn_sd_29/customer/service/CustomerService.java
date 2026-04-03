package com.example.datn_sd_29.customer.service;

import com.example.datn_sd_29.customer.dto.CustomerListResponse;
import com.example.datn_sd_29.customer.dto.CustomerProfileResponse;
import com.example.datn_sd_29.customer.dto.CustomerResponse;
import com.example.datn_sd_29.customer.dto.UpdateProfileRequest;
import com.example.datn_sd_29.customer.dto.UpdateProfileResponse;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.entity.Gender;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final JwtService jwtService;

    // ========================
    // ADMIN: GET ALL CUSTOMERS WITH SORT
    // ========================
    public List<CustomerResponse> getAllSorted(String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return customerRepository.findAll(sort)
                .stream()
                .map(CustomerResponse::new)
                .toList();
    }

    // ========================
    // ADMIN: SEARCH CUSTOMERS
    // ========================
    public List<CustomerResponse> search(String keyword, Boolean isActive, Gender gender,
                                         String startDate, String endDate) {

        // fix keyword rỗng
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        Instant start = null;
        Instant end = null;

        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDate.parse(startDate)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
        }

        if (endDate != null && !endDate.isEmpty()) {
            end = LocalDate.parse(endDate)
                    .plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
        }

        return java.util.Optional
                .ofNullable(customerRepository.search(keyword, isActive, gender, start, end))
                .orElse(List.of())
                .stream()
                .filter(java.util.Objects::nonNull)
                .map(CustomerResponse::new)
                .toList();
    }

    // ========================
    // ADMIN: UPDATE CUSTOMER STATUS
    // ========================
    public void updateStatus(Integer id, Boolean isActive) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setIsActive(isActive);
        customerRepository.save(customer);
    }

    // ========================
    // CUSTOMER: GET ALL (SIMPLE LIST FOR ADMIN)
    // ========================
    public List<CustomerListResponse> getAll() {
        return customerRepository.findAll()
                .stream()
                .map(CustomerListResponse::new)
                .toList();
    }

    // ========================
    // CUSTOMER: GET PROFILE
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
    // CUSTOMER: UPDATE PROFILE
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
