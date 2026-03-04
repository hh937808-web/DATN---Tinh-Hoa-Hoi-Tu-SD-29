package com.example.datn_sd_29.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SendOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    @Size(max = 200, message = "Email max 200 characters")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
