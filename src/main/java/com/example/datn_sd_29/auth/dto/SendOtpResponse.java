package com.example.datn_sd_29.auth.dto;

import java.time.Instant;

public class SendOtpResponse {

    private final String email;
    private final Instant expiresAt;
    private final long resendAfterSeconds;

    public SendOtpResponse(String email, Instant expiresAt, long resendAfterSeconds) {
        this.email = email;
        this.expiresAt = expiresAt;
        this.resendAfterSeconds = resendAfterSeconds;
    }

    public String getEmail() {
        return email;
    }

    public long getResendAfterSeconds() {
        return resendAfterSeconds;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
