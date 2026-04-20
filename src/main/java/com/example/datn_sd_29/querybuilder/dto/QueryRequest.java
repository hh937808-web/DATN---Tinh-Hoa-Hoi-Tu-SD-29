package com.example.datn_sd_29.querybuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {
    private String queryType; // VISUAL or SQL
    private VisualQuery visualQuery;
    private String sqlQuery;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisualQuery {
        private String tableName;
        private List<ColumnSelection> columns;
        private List<FilterCondition> filters;
        private List<JoinClause> joins;
        private List<String> groupBy;
        private List<OrderByClause> orderBy;
        private Integer limit;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ColumnSelection {
            private String tableName;
            private String columnName;
            private String aggregation; // null, SUM, AVG, COUNT, MIN, MAX
            private String alias;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FilterCondition {
            private String tableName;
            private String columnName;
            private String operator; // =, !=, >, <, >=, <=, LIKE, IN, BETWEEN
            private Object value;
            private Object value2; // for BETWEEN
            private String logicalOperator; // AND, OR (for next condition)
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class JoinClause {
            private String joinType; // INNER, LEFT, RIGHT
            private String tableName;
            private String onColumn;
            private String withTable;
            private String withColumn;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OrderByClause {
            private String tableName;
            private String columnName;
            private String direction; // ASC, DESC
        }
    }
}
