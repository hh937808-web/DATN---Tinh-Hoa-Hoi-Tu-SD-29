package com.example.datn_sd_29.customer.dto;

import com.example.datn_sd_29.customer.entity.Customer;

public class CustomerListResponse {

    private Integer id;
    private String fullName;
    private String phoneNumber;
    private String email;

    public CustomerListResponse(Customer customer) {
        this.id = customer.getId();
        this.fullName = customer.getFullName();
        this.phoneNumber = customer.getPhoneNumber();
        this.email = customer.getEmail();
    }

    public Integer getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }
}
