package com.example.datn_sd_29.walkin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedTablesResponse {
    private List<TableInfo> suggestedTables;
    private Integer totalCapacity;
    private String message;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableInfo {
        private Integer id;
        private String tableName;
        private Integer seatingCapacity;
        private String area;
        private Integer floor;
    }
}
