package com.example.datn_sd_29.querybuilder.service;

import com.example.datn_sd_29.querybuilder.dto.TableMetadataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseMetadataService {
    
    private final DataSource dataSource;
    
    // Whitelist of tables that are safe to query
    private static final Set<String> ALLOWED_TABLES = Set.of(
        "Invoice", "InvoiceItem", "Product", "ProductCombo",
        "Customer", "Employee", "DiningTable", "InvoiceDiningTable",
        "CustomerVoucher", "ProductVoucher", "InvoiceVoucher"
    );
    
    // Display names for tables (Vietnamese)
    private static final Map<String, String> TABLE_DISPLAY_NAMES = Map.ofEntries(
        Map.entry("Invoice", "Hóa đơn"),
        Map.entry("InvoiceItem", "Chi tiết hóa đơn"),
        Map.entry("Product", "Sản phẩm"),
        Map.entry("ProductCombo", "Combo"),
        Map.entry("Customer", "Khách hàng"),
        Map.entry("Employee", "Nhân viên"),
        Map.entry("DiningTable", "Bàn ăn"),
        Map.entry("InvoiceDiningTable", "Bàn trong hóa đơn"),
        Map.entry("CustomerVoucher", "Voucher khách hàng"),
        Map.entry("ProductVoucher", "Voucher sản phẩm"),
        Map.entry("InvoiceVoucher", "Voucher hóa đơn")
    );
    
    // Display names for common columns (Vietnamese)
    private static final Map<String, String> COLUMN_DISPLAY_NAMES = Map.ofEntries(
        Map.entry("id", "ID"),
        Map.entry("name", "Tên"),
        Map.entry("total_amount", "Tổng tiền"),
        Map.entry("payment_method", "Phương thức thanh toán"),
        Map.entry("payment_status", "Trạng thái thanh toán"),
        Map.entry("created_at", "Ngày tạo"),
        Map.entry("updated_at", "Ngày cập nhật"),
        Map.entry("quantity", "Số lượng"),
        Map.entry("price", "Giá"),
        Map.entry("discount", "Giảm giá"),
        Map.entry("status", "Trạng thái"),
        Map.entry("phone_number", "Số điện thoại"),
        Map.entry("email", "Email"),
        Map.entry("address", "Địa chỉ"),
        Map.entry("channel", "Kênh"),
        Map.entry("table_number", "Số bàn"),
        Map.entry("capacity", "Sức chứa")
    );
    
    public List<TableMetadataResponse> getAllTableMetadata() {
        List<TableMetadataResponse> tables = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            for (String tableName : ALLOWED_TABLES) {
                TableMetadataResponse tableMetadata = getTableMetadata(tableName, metaData);
                if (tableMetadata != null) {
                    tables.add(tableMetadata);
                }
            }
            
        } catch (SQLException e) {
            log.error("Error getting database metadata", e);
            throw new RuntimeException("Failed to retrieve database metadata", e);
        }
        
        return tables;
    }
    
    public TableMetadataResponse getTableMetadata(String tableName) {
        if (!ALLOWED_TABLES.contains(tableName.toLowerCase())) {
            throw new IllegalArgumentException("Table not allowed: " + tableName);
        }
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            return getTableMetadata(tableName, metaData);
        } catch (SQLException e) {
            log.error("Error getting table metadata for: " + tableName, e);
            throw new RuntimeException("Failed to retrieve table metadata", e);
        }
    }
    
    private TableMetadataResponse getTableMetadata(String tableName, DatabaseMetaData metaData) throws SQLException {
        List<TableMetadataResponse.ColumnMetadata> columns = new ArrayList<>();
        Set<String> primaryKeys = getPrimaryKeys(tableName, metaData);
        Map<String, ForeignKeyInfo> foreignKeys = getForeignKeys(tableName, metaData);
        
        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("TYPE_NAME");
                boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                boolean isPrimaryKey = primaryKeys.contains(columnName);
                
                ForeignKeyInfo fkInfo = foreignKeys.get(columnName);
                boolean isForeignKey = fkInfo != null;
                String referencedTable = isForeignKey ? fkInfo.referencedTable : null;
                String referencedColumn = isForeignKey ? fkInfo.referencedColumn : null;
                
                String displayName = COLUMN_DISPLAY_NAMES.getOrDefault(columnName, 
                    formatColumnName(columnName));
                
                columns.add(new TableMetadataResponse.ColumnMetadata(
                    columnName, displayName, dataType, nullable, 
                    isPrimaryKey, isForeignKey, referencedTable, referencedColumn
                ));
            }
        }
        
        if (columns.isEmpty()) {
            return null;
        }
        
        List<TableMetadataResponse.RelationshipMetadata> relationships = 
            buildRelationships(tableName, foreignKeys);
        
        String displayName = TABLE_DISPLAY_NAMES.getOrDefault(tableName, 
            formatTableName(tableName));
        
        return new TableMetadataResponse(tableName, displayName, columns, relationships);
    }
    
    private Set<String> getPrimaryKeys(String tableName, DatabaseMetaData metaData) throws SQLException {
        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet rs = metaData.getPrimaryKeys(null, null, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        return primaryKeys;
    }
    
    private Map<String, ForeignKeyInfo> getForeignKeys(String tableName, DatabaseMetaData metaData) throws SQLException {
        Map<String, ForeignKeyInfo> foreignKeys = new HashMap<>();
        try (ResultSet rs = metaData.getImportedKeys(null, null, tableName)) {
            while (rs.next()) {
                String fkColumnName = rs.getString("FKCOLUMN_NAME");
                String pkTableName = rs.getString("PKTABLE_NAME");
                String pkColumnName = rs.getString("PKCOLUMN_NAME");
                foreignKeys.put(fkColumnName, new ForeignKeyInfo(pkTableName, pkColumnName));
            }
        }
        return foreignKeys;
    }
    
    private List<TableMetadataResponse.RelationshipMetadata> buildRelationships(
            String tableName, Map<String, ForeignKeyInfo> foreignKeys) {
        List<TableMetadataResponse.RelationshipMetadata> relationships = new ArrayList<>();
        
        for (Map.Entry<String, ForeignKeyInfo> entry : foreignKeys.entrySet()) {
            String fromColumn = entry.getKey();
            ForeignKeyInfo fkInfo = entry.getValue();
            
            relationships.add(new TableMetadataResponse.RelationshipMetadata(
                tableName, fromColumn, fkInfo.referencedTable, fkInfo.referencedColumn, "MANY_TO_ONE"
            ));
        }
        
        return relationships;
    }
    
    private String formatTableName(String tableName) {
        return Arrays.stream(tableName.split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .reduce((a, b) -> a + " " + b)
            .orElse(tableName);
    }
    
    private String formatColumnName(String columnName) {
        return Arrays.stream(columnName.split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .reduce((a, b) -> a + " " + b)
            .orElse(columnName);
    }
    
    private static class ForeignKeyInfo {
        String referencedTable;
        String referencedColumn;
        
        ForeignKeyInfo(String referencedTable, String referencedColumn) {
            this.referencedTable = referencedTable;
            this.referencedColumn = referencedColumn;
        }
    }
}
