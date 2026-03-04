package com.example.datn_sd_29.auth.controller;

import com.example.datn_sd_29.auth.dto.RegisterRequest;
import com.example.datn_sd_29.auth.dto.SendOtpResponse;
import com.example.datn_sd_29.auth.service.OtpService;
import com.example.datn_sd_29.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegisterController {

    private final OtpService otpService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<SendOtpResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        SendOtpResponse response = otpService.startRegister(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Register info received. OTP sent to email.", response));
    }
}
