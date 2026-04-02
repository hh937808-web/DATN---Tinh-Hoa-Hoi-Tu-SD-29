package com.example.datn_sd_29.dining_table.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;

@Data
public class DiningTableResponse {
    private Integer id;
    private String tableName;
    private Integer seatingCapacity;
    private String tableStatus;
    private String area;
    private Integer floor;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant createdAt;
}