package com.example.datn_sd_29.reservation.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber;

    @NotNull(message = "Guest count is required")
    @Min(value = 1, message = "Guest count must be greater than 0")
    private Integer guestCount;

    @NotNull(message = "Reserved time is required")
    private LocalDateTime reservedAt; // thời gian đến

    private String note;

    private String foodNote;

    private String guestName;   // Tên người thực sự đến ăn (optional)
    private String guestPhone;  // SĐT người thực sự đến ăn (optional)

}
