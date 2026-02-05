package com.example.datn_sd_29.dto;

public class RegisterResponse {
    private Integer customerId;
    private String email;

    public RegisterResponse(Integer customerId, String email) {
        this.customerId = customerId;
        this.email = email;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public String getEmail() {
        return email;
    }
}
