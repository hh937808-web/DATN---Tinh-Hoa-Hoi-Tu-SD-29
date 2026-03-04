package com.example.datn_sd_29.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "PendingRegister")
public class PendingRegister {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pending_register_id", nullable = false)
    private Integer id;

    @Column(name = "email", nullable = false, length = 200)
    private String email;

    @Column(name = "otp_challenge_id", nullable = false)
    private Integer otpChallengeId;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "phone_number", length = 10)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "otp_verified_at", nullable = false)
    private Instant otpVerifiedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
