package com.example.datn_sd_29.employee.entity;

import com.example.datn_sd_29.employee.enums.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Getter
@Setter
@Entity
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id", nullable = false)
    private Integer id;

    @Size(max = 150)
    @Nationalized
    @Column(name = "full_name", length = 150)
    private String fullName;

    @Size(max = 100)
    @Column(name = "username", length = 100)
    private String username;

    @Size(max = 150)
    @Column(name = "password", length = 150)
    private String password;

    @Column(name = "role")
    private String role;

    @Size(max = 10)
    @Column(name = "phone_number", length = 10)
    private String phoneNumber;

    @Size(max = 200)
    @Column(name = "email", length = 200)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @ColumnDefault("getdate()")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "address")
    private String address;

}