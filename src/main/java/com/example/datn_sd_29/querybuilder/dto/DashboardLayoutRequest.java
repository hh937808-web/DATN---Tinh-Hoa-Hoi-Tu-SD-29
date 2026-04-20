package com.example.datn_sd_29.querybuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardLayoutRequest {
    private Long savedQueryId;
    private Integer x;
    private Integer y;
    private Integer w;
    private Integer h;
}
