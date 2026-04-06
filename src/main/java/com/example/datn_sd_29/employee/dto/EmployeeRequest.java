package com.example.datn_sd_29.employee.dto;

import com.example.datn_sd_29.employee.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeRequest {

    @NotBlank(message = "Tên không được để trống")
    @Size(max = 150)
    private String fullName;

    @NotBlank(message = "Username không được để trống")
    @Size(max = 100)
    private String username;

    // Password is only required when creating new employee, not when updating
    @Size(max = 150)
    private String password;

    private String role;

    private Gender gender;

    @Size(max = 10)
    private String phoneNumber;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String address;

    private Boolean isActive;
}