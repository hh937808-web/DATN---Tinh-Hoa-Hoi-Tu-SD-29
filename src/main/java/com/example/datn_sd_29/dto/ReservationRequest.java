package com.example.datn_sd_29.dto;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReservationRequest {
    private String fullName;
    private String phoneNumber;
    private String email;

    private LocalDateTime reservedAt; // thời gian đến
    private List<Integer> diningTableIds; // danh sách bàn

}
