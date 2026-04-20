package com.example.datn_sd_29.querybuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {
    private List<ColumnInfo> columns;
    private List<Map<String, Object>> rows;
    private Integer totalRows;
    private Long executionTimeMs;
    private String generatedSql;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnInfo {
        private String name;
        private String displayName;
        private String dataType;
    }
}
