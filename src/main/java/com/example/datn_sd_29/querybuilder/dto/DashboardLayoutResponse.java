package com.example.datn_sd_29.querybuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardLayoutResponse {
    private Long id;
    private Long savedQueryId;
    private Integer x;
    private Integer y;
    private Integer w;
    private Integer h;
    private String i; // unique identifier for grid-layout (savedQueryId as string)
    
    public DashboardLayoutResponse(Long id, Long savedQueryId, Integer x, Integer y, Integer w, Integer h) {
        this.id = id;
        this.savedQueryId = savedQueryId;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.i = String.valueOf(savedQueryId);
    }
}
