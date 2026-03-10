package com.example.datn_sd_29.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvailableTableResponse {
    private Integer id;
    private String tableCode;
    private String tableName;
    private Integer seatingCapacity;
}
