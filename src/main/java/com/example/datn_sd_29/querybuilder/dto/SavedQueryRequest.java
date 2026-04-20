package com.example.datn_sd_29.querybuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedQueryRequest {
    private String name;
    private String description;
    private QueryRequest query;
    private String visualizationType; // TABLE, BAR, LINE, PIE, DOUGHNUT
}
