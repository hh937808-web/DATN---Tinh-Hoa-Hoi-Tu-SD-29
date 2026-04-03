package com.example.datn_sd_29.customer.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.customer.dto.CustomerListResponse;
import com.example.datn_sd_29.customer.dto.CustomerProfileResponse;
import com.example.datn_sd_29.customer.dto.CustomerResponse;
import com.example.datn_sd_29.customer.dto.UpdateProfileRequest;
import com.example.datn_sd_29.customer.dto.UpdateProfileResponse;
import com.example.datn_sd_29.customer.entity.Gender;
import com.example.datn_sd_29.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin("*")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ========================
    // ADMIN: GET ALL CUSTOMERS (SIMPLE LIST)
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
    // ADMIN: SEARCH CUSTOMERS
    // ========================
    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return ResponseEntity.ok(
                customerService.search(keyword, isActive, gender, startDate, endDate)
        );
    }

    // ========================
    // ADMIN: SORT CUSTOMERS
    // ========================
    @GetMapping("/sort")
    public ResponseEntity<List<CustomerResponse>> sort(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return ResponseEntity.ok(customerService.getAllSorted(sortBy, direction));
    }

    // ========================
    // ADMIN: UPDATE CUSTOMER STATUS
    // ========================
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean isActive = body.get("isActive");
        customerService.updateStatus(id, isActive);
        return ResponseEntity.ok().build();
    }

    // ========================
    // CUSTOMER: GET PROFILE
    // ========================
    @GetMapping("/profile")
    public CustomerProfileResponse getProfile(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        return customerService.getProfile(token);
    }

    // ========================
    // CUSTOMER: UPDATE PROFILE
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
