package com.example.datn_sd_29.controller;

import com.example.datn_sd_29.dto.ApiResponse;
import com.example.datn_sd_29.dto.RegisterRequest;
import com.example.datn_sd_29.dto.RegisterResponse;
import com.example.datn_sd_29.service.RegisterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RegisterController {

    private final RegisterService registerService;
    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registerService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Register successfully!", response));
    }
}
