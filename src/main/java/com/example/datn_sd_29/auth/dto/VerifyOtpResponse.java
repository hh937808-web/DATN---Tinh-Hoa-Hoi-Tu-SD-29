package com.example.datn_sd_29.auth.dto;

import java.time.Instant;

public class VerifyOtpResponse {

    private final String email;
    private final Instant verifiedAt;
    private final Instant pendingExpiresAt;

    public VerifyOtpResponse(String email, Instant verifiedAt, Instant pendingExpiresAt) {
        this.email = email;
        this.verifiedAt = verifiedAt;
        this.pendingExpiresAt = pendingExpiresAt;
    }

    public String getEmail() {
        return email;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getPendingExpiresAt() {
        return pendingExpiresAt;
    }
}
