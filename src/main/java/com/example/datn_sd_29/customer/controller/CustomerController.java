package com.example.datn_sd_29.customer.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.customer.dto.CustomerListResponse;
import com.example.datn_sd_29.customer.dto.CustomerProfileResponse;
import com.example.datn_sd_29.customer.dto.UpdateProfileRequest;
import com.example.datn_sd_29.customer.dto.UpdateProfileResponse;
import com.example.datn_sd_29.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ========================
    // GET ALL CUSTOMERS (FOR ADMIN)
    // ========================
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerListResponse>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get customer list successfully!",
                        customerService.getAll()
                )
        );
    }

    // ========================
    // GET PROFILE
    // ========================
    @GetMapping("/profile")
    public CustomerProfileResponse getProfile(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        return customerService.getProfile(token);
    }

    // ========================
    // UPDATE PROFILE
    // ========================
    @PutMapping("/profile")
    public UpdateProfileResponse updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid UpdateProfileRequest request
    ) {
        String token = extractToken(authHeader);
        return customerService.updateProfile(token, request);
    }

    // ========================
    // HELPER: Extract JWT
    // ========================
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization header");
        }
        return authHeader.substring(7);
    }
}