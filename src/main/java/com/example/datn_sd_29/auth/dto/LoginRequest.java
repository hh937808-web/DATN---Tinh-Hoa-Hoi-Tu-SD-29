package com.example.datn_sd_29.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Email/Username is required")
    @Size(max = 200, message = "Email/Username max 200 characters")
    private String email; // Can be email or username

    @NotBlank(message = "Password is required")
    @Size(max = 150, message = "Password max 150 characters")
    private String password;

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
