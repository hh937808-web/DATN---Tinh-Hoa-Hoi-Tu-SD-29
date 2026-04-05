package com.example.datn_sd_29.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ConfirmReservationRequest {
    
    @NotBlank(message = "Preferred area is required")
    @Pattern(regexp = "^[A-F]$", message = "Area must be A, B, C, D, E, or F")
    private String preferredArea;
}
