package com.example.datn_sd_29.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TableReassignmentRequest {
    @NotEmpty(message = "Table IDs cannot be empty")
    private List<Integer> tableIds;
}
