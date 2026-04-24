package com.example.datn_sd_29.customer.service;

import com.example.datn_sd_29.customer.dto.*;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.entity.Gender;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

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

        // AUDIT DIFF — snapshot BEFORE modification
        java.util.Map<String, Object> before = com.example.datn_sd_29.audit.util.AuditDiffUtil.snapshot(
                customer, "fullName", "email", "phoneNumber", "dateOfBirth"
        );

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

        Customer saved = customerRepository.save(customer);

        // AUDIT DIFF — diff trước/sau
        java.util.Map<String, Object> after = com.example.datn_sd_29.audit.util.AuditDiffUtil.snapshot(
                saved, "fullName", "email", "phoneNumber", "dateOfBirth"
        );
        com.example.datn_sd_29.audit.context.AuditContext.setChanges(
                com.example.datn_sd_29.audit.util.AuditDiffUtil.diff(before, after, CUSTOMER_FIELD_LABELS)
        );

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

    // ========================
    // CUSTOMER: CHANGE PASSWORD
    // ========================
    // ========================
// CUSTOMER: CHANGE PASSWORD
// ========================
    public ChangePasswordResponse changePassword(String token, ChangePasswordRequest request) {

        Integer customerId = jwtService.extractCustomerId(token);
        String email = jwtService.extractEmail(token);
        String role = jwtService.extractRole(token);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Check nhập đủ
        if (request.getOldPassword() == null || request.getOldPassword().isBlank()
                || request.getNewPassword() == null || request.getNewPassword().isBlank()
                || request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
            throw new RuntimeException("Vui lòng nhập đầy đủ thông tin");
        }

        // 2. Check mật khẩu cũ
        if (!passwordEncoder.matches(request.getOldPassword(), customer.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        // 3. Check xác nhận
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        // 4. Check độ dài
        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        // 5. Không cho trùng mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), customer.getPassword())) {
            throw new RuntimeException("Mật khẩu mới không được trùng mật khẩu cũ");
        }

        // 6. Encode password mới
        customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        customerRepository.save(customer);

        // 7. Tạo token mới để user không bị out login
        String newToken = jwtService.generateToken(
                email,
                customer.getId(),
                role
        );

        return ChangePasswordResponse.builder()
                .message("Đổi mật khẩu thành công")
                .accessToken(newToken)
                .build();
    }

    private static final java.util.Map<String, String> CUSTOMER_FIELD_LABELS = java.util.Map.of(
            "fullName", "Họ tên",
            "email", "Email",
            "phoneNumber", "Số điện thoại",
            "dateOfBirth", "Ngày sinh"
    );
}


