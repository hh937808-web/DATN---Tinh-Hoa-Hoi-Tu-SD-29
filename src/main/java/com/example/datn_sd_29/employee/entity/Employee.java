package com.example.datn_sd_29.employee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Size(max = 50)
    @Nationalized
    @Column(name = "role", length = 50)
    private String role;

    @Size(max = 10)
    @Column(name = "phone_number", length = 10)
    private String phoneNumber;

    @Size(max = 200)
    @Column(name = "email", length = 200)
    private String email;

    @ColumnDefault("getdate()")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "is_active")
    private Boolean isActive;

}