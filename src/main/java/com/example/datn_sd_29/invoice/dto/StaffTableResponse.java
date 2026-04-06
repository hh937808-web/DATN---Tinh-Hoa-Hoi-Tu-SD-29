package com.example.datn_sd_29.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffTableResponse {
    private Integer tableId;
    private String tableName;
    private Integer floor;
    private Integer seatingCapacity;
    private String tableStatus;
}
