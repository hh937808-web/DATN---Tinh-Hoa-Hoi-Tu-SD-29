package com.example.datn_sd_29.querybuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableMetadataResponse {
    private String tableName;
    private String displayName;
    private List<ColumnMetadata> columns;
    private List<RelationshipMetadata> relationships;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnMetadata {
        private String columnName;
        private String displayName;
        private String dataType;
        private boolean nullable;
        private boolean primaryKey;
        private boolean foreignKey;
        private String referencedTable;
        private String referencedColumn;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationshipMetadata {
        private String fromTable;
        private String fromColumn;
        private String toTable;
        private String toColumn;
        private String relationshipType; // ONE_TO_MANY, MANY_TO_ONE, ONE_TO_ONE
    }
}
