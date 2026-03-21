package com.example.datn_sd_29.auth.dto;

public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private String email;
    private String role;

    public LoginResponse(String accessToken, String email, String role) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.email = email;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
