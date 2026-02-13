package com.example.datn_sd_29.reservation.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReservationRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone is number must be 10 digits")
    private String phoneNumber;

    @NotNull(message = "Reserved time is required")
    private LocalDateTime reservedAt; // thời gian đến

    @NotEmpty(message = "Dining table list is required")
    private List<Integer> diningTableIds; // danh sách bàn

}
