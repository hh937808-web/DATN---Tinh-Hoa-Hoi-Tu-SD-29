package com.example.datn_sd_29.querybuilder.service;

import com.example.datn_sd_29.querybuilder.dto.QueryRequest;
import com.example.datn_sd_29.querybuilder.dto.QueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryExecutionService {
    
    private final DataSource dataSource;
    private final DatabaseMetadataService metadataService;
    
    private static final int MAX_ROWS = 10000;
    private static final int QUERY_TIMEOUT_SECONDS = 30;
    
    // Dangerous SQL keywords that are not allowed
    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
        // Data Modification
        "INSERT", "UPDATE", "DELETE", "MERGE", "TRUNCATE",
        // Schema Modification
        "CREATE", "ALTER", "DROP", "RENAME",
        // Permissions
        "GRANT", "REVOKE", "DENY",
        // Execution
        "EXEC", "EXECUTE", "SP_EXECUTESQL", "XP_CMDSHELL",
        // Transactions (có thể gây lock) - removed BEGIN to avoid false positives
        "COMMIT", "ROLLBACK", "SAVEPOINT",
        // Backup/Restore
        "BACKUP", "RESTORE",
        // System Commands
        "SHUTDOWN", "RECONFIGURE", "DBCC",
        // Bulk Operations
        "BULK", "OPENROWSET", "OPENDATASOURCE"
    );
    
    private static final Pattern COMMENT_PATTERN = Pattern.compile("--.*|/\\*.*?\\*/", Pattern.DOTALL);
    
    public QueryResponse executeQuery(QueryRequest request) {
        long startTime = System.currentTimeMillis();
        
        String sql;
        if ("VISUAL".equals(request.getQueryType())) {
            sql = buildSqlFromVisualQuery(request.getVisualQuery());
        } else if ("SQL".equals(request.getQueryType())) {
            sql = request.getSqlQuery();
            validateSqlQuery(sql);
        } else {
            throw new IllegalArgumentException("Loại truy vấn không hợp lệ: " + request.getQueryType());
        }
        
        log.info("Executing query: {}", sql);
        
        QueryResponse response = executeSql(sql);
        response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        response.setGeneratedSql(sql);
        
        return response;
    }
    
    private String buildSqlFromVisualQuery(QueryRequest.VisualQuery visualQuery) {
        if (visualQuery == null || visualQuery.getTableName() == null) {
            throw new IllegalArgumentException("Truy vấn trực quan phải có tên bảng");
        }
        
        StringBuilder sql = new StringBuilder("SELECT ");
        
        // Build SELECT clause
        if (visualQuery.getColumns() == null || visualQuery.getColumns().isEmpty()) {
            sql.append("*");
        } else {
            List<String> selectParts = new ArrayList<>();
            for (QueryRequest.VisualQuery.ColumnSelection col : visualQuery.getColumns()) {
                String columnExpr = buildColumnExpression(col);
                selectParts.add(columnExpr);
            }
            sql.append(String.join(", ", selectParts));
        }
        
        // FROM clause
        sql.append(" FROM ").append(visualQuery.getTableName());
        
        // JOIN clauses
        if (visualQuery.getJoins() != null && !visualQuery.getJoins().isEmpty()) {
            for (QueryRequest.VisualQuery.JoinClause join : visualQuery.getJoins()) {
                sql.append(" ").append(join.getJoinType()).append(" JOIN ")
                   .append(join.getTableName())
                   .append(" ON ")
                   .append(join.getWithTable()).append(".").append(join.getWithColumn())
                   .append(" = ")
                   .append(join.getTableName()).append(".").append(join.getOnColumn());
            }
        }
        
        // WHERE clause
        if (visualQuery.getFilters() != null && !visualQuery.getFilters().isEmpty()) {
            sql.append(" WHERE ");
            List<String> filterParts = new ArrayList<>();
            for (int i = 0; i < visualQuery.getFilters().size(); i++) {
                QueryRequest.VisualQuery.FilterCondition filter = visualQuery.getFilters().get(i);
                String filterExpr = buildFilterExpression(filter);
                filterParts.add(filterExpr);
                
                if (i < visualQuery.getFilters().size() - 1) {
                    String logicalOp = filter.getLogicalOperator() != null ? 
                        filter.getLogicalOperator() : "AND";
                    filterParts.add(logicalOp);
                }
            }
            sql.append(String.join(" ", filterParts));
        }
        
        // GROUP BY clause
        if (visualQuery.getGroupBy() != null && !visualQuery.getGroupBy().isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", visualQuery.getGroupBy()));
        }
        
        // ORDER BY clause
        if (visualQuery.getOrderBy() != null && !visualQuery.getOrderBy().isEmpty()) {
            sql.append(" ORDER BY ");
            List<String> orderParts = new ArrayList<>();
            for (QueryRequest.VisualQuery.OrderByClause order : visualQuery.getOrderBy()) {
                String orderExpr = order.getTableName() != null ?
                    order.getTableName() + "." + order.getColumnName() :
                    order.getColumnName();
                orderExpr += " " + (order.getDirection() != null ? order.getDirection() : "ASC");
                orderParts.add(orderExpr);
            }
            sql.append(String.join(", ", orderParts));
        }
        
        // LIMIT clause
        int limit = visualQuery.getLimit() != null ? 
            Math.min(visualQuery.getLimit(), MAX_ROWS) : MAX_ROWS;
        sql.append(" LIMIT ").append(limit);
        
        return sql.toString();
    }
    
    private String buildColumnExpression(QueryRequest.VisualQuery.ColumnSelection col) {
        String columnName = col.getTableName() != null ?
            col.getTableName() + "." + col.getColumnName() :
            col.getColumnName();
        
        if (col.getAggregation() != null && !col.getAggregation().isEmpty()) {
            columnName = col.getAggregation() + "(" + columnName + ")";
        }
        
        if (col.getAlias() != null && !col.getAlias().isEmpty()) {
            columnName += " AS " + col.getAlias();
        }
        
        return columnName;
    }
    
    private String buildFilterExpression(QueryRequest.VisualQuery.FilterCondition filter) {
        String columnName = filter.getTableName() != null ?
            filter.getTableName() + "." + filter.getColumnName() :
            filter.getColumnName();
        
        String operator = filter.getOperator();
        Object value = filter.getValue();
        
        switch (operator.toUpperCase()) {
            case "IN":
                if (value instanceof List) {
                    List<?> values = (List<?>) value;
                    String inValues = values.stream()
                        .map(v -> "'" + escapeSqlString(v.toString()) + "'")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                    return columnName + " IN (" + inValues + ")";
                }
                break;
            case "BETWEEN":
                return columnName + " BETWEEN '" + escapeSqlString(value.toString()) + 
                       "' AND '" + escapeSqlString(filter.getValue2().toString()) + "'";
            case "LIKE":
                return columnName + " LIKE '%" + escapeSqlString(value.toString()) + "%'";
            case "IS NULL":
                return columnName + " IS NULL";
            case "IS NOT NULL":
                return columnName + " IS NOT NULL";
            default:
                return columnName + " " + operator + " '" + escapeSqlString(value.toString()) + "'";
        }
        
        return columnName + " = '" + escapeSqlString(value.toString()) + "'";
    }
    
    private void validateSqlQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("Câu lệnh SQL không được để trống");
        }
        
        // Remove comments
        String cleanSql = COMMENT_PATTERN.matcher(sql).replaceAll(" ");
        String upperSql = cleanSql.toUpperCase();
        
        // Remove extra whitespace
        upperSql = upperSql.replaceAll("\\s+", " ").trim();
        
        // Check for forbidden keywords using word boundaries
        // This prevents false positives like "created_at" matching "CREATE"
        for (String keyword : FORBIDDEN_KEYWORDS) {
            // Use regex with word boundaries to match only standalone keywords
            Pattern keywordPattern = Pattern.compile("\\b" + keyword + "\\b");
            if (keywordPattern.matcher(upperSql).find()) {
                throw new SecurityException("Không được phép sử dụng lệnh " + keyword + ". Bạn chỉ được phép xem dữ liệu bằng SELECT, không được thêm/sửa/xóa dữ liệu hoặc thay đổi cấu trúc database.");
            }
        }
        
        // Must start with SELECT or WITH (for CTEs)
        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("WITH")) {
            throw new SecurityException("CHỈ ĐƯỢC PHÉP TRUY VẤN DỮ LIỆU!\n\n" +
                "Bạn chỉ được dùng lệnh SELECT để xem dữ liệu.\n" +
                "Không được phép thêm, sửa, xóa dữ liệu hoặc thay đổi cấu trúc database.");
        }
        
        // If starts with WITH, must contain SELECT
        if (upperSql.startsWith("WITH") && !upperSql.contains("SELECT")) {
            throw new SecurityException("Câu lệnh WITH phải đi kèm với SELECT để truy vấn dữ liệu.");
        }
        
        // Check for multiple statements (semicolon in the middle)
        String trimmedSql = cleanSql.trim();
        if (trimmedSql.contains(";")) {
            // Allow only one semicolon at the end
            int semicolonIndex = trimmedSql.indexOf(";");
            if (semicolonIndex != trimmedSql.length() - 1) {
                throw new SecurityException("Không được phép chạy nhiều câu lệnh SQL cùng lúc. Chỉ được phép 1 câu SELECT.");
            }
        }
        
        // Additional security: Check for SQL injection patterns
        if (upperSql.contains("--") || upperSql.contains("/*") || upperSql.contains("*/")) {
            // Comments were already removed, if they still exist, it's suspicious
            throw new SecurityException("Không được phép sử dụng comment (-- hoặc /* */) trong SQL vì lý do bảo mật.");
        }
        
        // Check for xp_ system procedures (SQL Server specific)
        if (upperSql.contains("XP_")) {
            throw new SecurityException("Không được phép gọi system procedures (xp_cmdshell, etc.) vì lý do bảo mật.");
        }
        
        // Check for OPENROWSET, OPENDATASOURCE (can be used to access external data)
        if (upperSql.contains("OPENROWSET") || upperSql.contains("OPENDATASOURCE")) {
            throw new SecurityException("Không được phép truy cập dữ liệu từ nguồn bên ngoài (OPENROWSET, OPENDATASOURCE).");
        }
    }
    
    private QueryResponse executeSql(String sql) {
        List<QueryResponse.ColumnInfo> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            stmt.setMaxRows(MAX_ROWS);
            
            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                // Build column info
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    String columnLabel = metaData.getColumnLabel(i);
                    String dataType = metaData.getColumnTypeName(i);
                    
                    columns.add(new QueryResponse.ColumnInfo(
                        columnName, columnLabel, dataType
                    ));
                }
                
                // Build rows
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    rows.add(row);
                }
            }
            
        } catch (SQLException e) {
            log.error("Error executing query: " + sql, e);
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        }
        
        QueryResponse response = new QueryResponse();
        response.setColumns(columns);
        response.setRows(rows);
        response.setTotalRows(rows.size());
        
        return response;
    }
    
    private String escapeSqlString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }
}
