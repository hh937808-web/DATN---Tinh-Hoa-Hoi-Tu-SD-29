package com.example.datn_sd_29.dining_table.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class DiningTableResponse {
    private Integer id;
    private String tableName;
    private Integer seatingCapacity;
    private String tableStatus;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant createdAt;



}