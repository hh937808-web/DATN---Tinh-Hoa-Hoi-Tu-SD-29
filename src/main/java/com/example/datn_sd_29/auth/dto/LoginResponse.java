package com.example.datn_sd_29.auth.dto;

public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private String email;
    private String role;
    private Integer userId;
    private String fullName;
    private String username;

    public LoginResponse(String accessToken, String email, String role, Integer userId, String fullName, String username) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.email = email;
        this.role = role;
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
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

    public Integer getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }
}
