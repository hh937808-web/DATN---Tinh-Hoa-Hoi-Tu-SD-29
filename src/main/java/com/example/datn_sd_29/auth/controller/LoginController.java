package com.example.datn_sd_29.auth.controller;

import com.example.datn_sd_29.auth.dto.LoginRequest;
import com.example.datn_sd_29.auth.dto.LoginResponse;
import com.example.datn_sd_29.auth.service.LoginService;
import com.example.datn_sd_29.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;

    /**
     * Customer login endpoint - ONLY for customers using EMAIL
     */
    @PostMapping("/login/customer")
    public ResponseEntity<ApiResponse<LoginResponse>> customerLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginService.customerLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công!", response));
    }

    /**
     * Employee login endpoint - ONLY for employees using USERNAME
     */
    @PostMapping("/login/employee")
    public ResponseEntity<ApiResponse<LoginResponse>> employeeLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginService.employeeLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công!", response));
    }

    /**
     * @deprecated Use /login/customer or /login/employee instead
     * Legacy endpoint for backward compatibility
     */
    @Deprecated
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successfully!", response));
    }
}
