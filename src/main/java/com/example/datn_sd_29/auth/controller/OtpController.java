package com.example.datn_sd_29.auth.controller;

import com.example.datn_sd_29.auth.dto.SendOtpRequest;
import com.example.datn_sd_29.auth.dto.SendOtpResponse;
import com.example.datn_sd_29.auth.dto.VerifyOtpRequest;
import com.example.datn_sd_29.auth.dto.RegisterResponse;
import com.example.datn_sd_29.auth.service.OtpService;
import com.example.datn_sd_29.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendOtpResponse>> sendOtp(
            @Valid @RequestBody SendOtpRequest request, HttpServletRequest httpRequest
            ) {
        SendOtpResponse response = otpService.sendRegisterOtp(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", response));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<RegisterResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest
            ) {
        RegisterResponse response = otpService.verifyRegisterOtp(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Email verified. Register completed.", response));
    }
}
