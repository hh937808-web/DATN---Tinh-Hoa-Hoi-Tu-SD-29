package com.example.datn_sd_29.dto;

public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private String email;

    public LoginResponse(String accessToken, String email) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.email = email;
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
}
