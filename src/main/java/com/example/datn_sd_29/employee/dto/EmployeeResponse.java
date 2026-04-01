package com.example.datn_sd_29.employee.dto;

import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.enums.Gender;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.Instant;

@Getter
public class EmployeeResponse {

    private Integer id;
    private String fullName;
    private String username;
    private String role;
    private Gender gender;
    private String phoneNumber;
    private String email;
    private String address;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant createdAt;

    private Boolean isActive;

    public EmployeeResponse(Employee e) {
        this.id = e.getId();
        this.fullName = e.getFullName();
        this.username = e.getUsername();
        this.role = e.getRole();
        this.gender = e.getGender();
        this.phoneNumber = e.getPhoneNumber();
        this.email = e.getEmail();
        this.address = e.getAddress();
        this.createdAt = e.getCreatedAt();
        this.isActive = e.getIsActive();
    }
}