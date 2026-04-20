package com.example.datn_sd_29.querybuilder.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.querybuilder.dto.AiQueryRequest;
import com.example.datn_sd_29.querybuilder.dto.AiQueryResponse;
import com.example.datn_sd_29.querybuilder.dto.DashboardLayoutRequest;
import com.example.datn_sd_29.querybuilder.dto.DashboardLayoutResponse;
import com.example.datn_sd_29.querybuilder.dto.DashboardRequest;
import com.example.datn_sd_29.querybuilder.dto.DashboardResponse;
import com.example.datn_sd_29.querybuilder.dto.QueryRequest;
import com.example.datn_sd_29.querybuilder.dto.QueryResponse;
import com.example.datn_sd_29.querybuilder.dto.SavedQueryRequest;
import com.example.datn_sd_29.querybuilder.dto.TableMetadataResponse;
import com.example.datn_sd_29.querybuilder.entity.SavedQuery;
import com.example.datn_sd_29.querybuilder.service.AiQueryService;
import com.example.datn_sd_29.querybuilder.service.CustomDashboardService;
import com.example.datn_sd_29.querybuilder.service.DashboardLayoutService;
import com.example.datn_sd_29.querybuilder.service.DatabaseMetadataService;
import com.example.datn_sd_29.querybuilder.service.QueryExecutionService;
import com.example.datn_sd_29.querybuilder.service.SavedQueryService;
import com.example.datn_sd_29.report.service.ExcelExportService;
import com.example.datn_sd_29.report.service.PdfExportService;
import com.example.datn_sd_29.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/query-builder")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class QueryBuilderController {
    
    private final DatabaseMetadataService metadataService;
    private final QueryExecutionService queryExecutionService;
    private final SavedQueryService savedQueryService;
    private final AiQueryService aiQueryService;
    private final DashboardLayoutService dashboardLayoutService;
    private final CustomDashboardService customDashboardService;
    private final JwtService jwtService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    
    @GetMapping("/metadata/tables")
    public ResponseEntity<ApiResponse<List<TableMetadataResponse>>> getAllTables() {
        try {
            List<TableMetadataResponse> tables = metadataService.getAllTableMetadata();
            return ResponseEntity.ok(ApiResponse.success("Tables retrieved successfully", tables));
        } catch (Exception e) {
            log.error("Error getting table metadata", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to get table metadata: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/metadata/tables/{tableName}")
    public ResponseEntity<ApiResponse<TableMetadataResponse>> getTableMetadata(
            @PathVariable String tableName) {
        try {
            TableMetadataResponse table = metadataService.getTableMetadata(tableName);
            return ResponseEntity.ok(ApiResponse.success("Table metadata retrieved successfully", table));
        } catch (Exception e) {
            log.error("Error getting table metadata for: " + tableName, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to get table metadata: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<QueryResponse>> executeQuery(
            @RequestBody QueryRequest request) {
        try {
            QueryResponse response = queryExecutionService.executeQuery(request);
            return ResponseEntity.ok(ApiResponse.success("Query executed successfully", response));
        } catch (SecurityException e) {
            log.warn("Security violation in query execution", e);
            return ResponseEntity.status(403)
                .body(ApiResponse.error("Security violation: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error executing query", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Query execution failed: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/saved-queries")
    public ResponseEntity<ApiResponse<SavedQuery>> saveQuery(
            @RequestBody SavedQueryRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    // Token invalid or expired, use default employeeId = 1
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            SavedQuery savedQuery = savedQueryService.saveQuery(request, employeeId);
            return ResponseEntity.ok(ApiResponse.success("Query saved successfully", savedQuery));
        } catch (Exception e) {
            log.error("Error saving query", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to save query: " + e.getMessage(), null));
        }
    }
    
    @PutMapping("/saved-queries/{queryId}")
    public ResponseEntity<ApiResponse<SavedQuery>> updateQuery(
            @PathVariable Long queryId,
            @RequestBody SavedQueryRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            SavedQuery savedQuery = savedQueryService.updateQuery(queryId, request, employeeId);
            return ResponseEntity.ok(ApiResponse.success("Query updated successfully", savedQuery));
        } catch (SecurityException e) {
            log.warn("Permission denied for query update", e);
            return ResponseEntity.status(403)
                .body(ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating query", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update query: " + e.getMessage(), null));
        }
    }
    
    @DeleteMapping("/saved-queries/{queryId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuery(
            @PathVariable Long queryId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            savedQueryService.deleteQuery(queryId, employeeId);
            return ResponseEntity.ok(ApiResponse.success("Query deleted successfully", null));
        } catch (SecurityException e) {
            log.warn("Permission denied for query deletion", e);
            return ResponseEntity.status(403)
                .body(ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error deleting query", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to delete query: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/saved-queries")
    public ResponseEntity<ApiResponse<List<SavedQuery>>> getAllQueries() {
        try {
            List<SavedQuery> queries = savedQueryService.getAllQueries();
            return ResponseEntity.ok(ApiResponse.success("Queries retrieved successfully", queries));
        } catch (Exception e) {
            log.error("Error getting saved queries", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to get saved queries: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/saved-queries/my")
    public ResponseEntity<ApiResponse<List<SavedQuery>>> getMyQueries(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            List<SavedQuery> queries = savedQueryService.getMyQueries(employeeId);
            return ResponseEntity.ok(ApiResponse.success("Queries retrieved successfully", queries));
        } catch (Exception e) {
            log.error("Error getting my queries", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to get queries: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/saved-queries/{queryId}")
    public ResponseEntity<ApiResponse<SavedQuery>> getQuery(@PathVariable Long queryId) {
        try {
            SavedQuery query = savedQueryService.getQuery(queryId);
            return ResponseEntity.ok(ApiResponse.success("Query retrieved successfully", query));
        } catch (Exception e) {
            log.error("Error getting query", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to get query: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/saved-queries/{queryId}/definition")
    public ResponseEntity<ApiResponse<QueryRequest>> getQueryDefinition(@PathVariable Long queryId) {
        try {
            QueryRequest queryDef = savedQueryService.getQueryDefinition(queryId);
            return ResponseEntity.ok(ApiResponse.success("Query definition retrieved successfully", queryDef));
        } catch (Exception e) {
            log.error("Error getting query definition", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to get query definition: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/saved-queries/{queryId}/execute")
    public ResponseEntity<ApiResponse<QueryResponse>> executeSavedQuery(@PathVariable Long queryId) {
        try {
            QueryRequest queryDef = savedQueryService.getQueryDefinition(queryId);
            QueryResponse response = queryExecutionService.executeQuery(queryDef);
            return ResponseEntity.ok(ApiResponse.success("Query executed successfully", response));
        } catch (Exception e) {
            log.error("Error executing saved query", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to execute query: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/ai/generate-sql")
    public ResponseEntity<ApiResponse<AiQueryResponse>> generateSqlFromQuestion(
            @RequestBody AiQueryRequest request) {
        try {
            AiQueryResponse response = aiQueryService.generateSqlFromQuestion(request.getQuestion());
            return ResponseEntity.ok(ApiResponse.success("SQL generated successfully", response));
        } catch (SecurityException e) {
            log.warn("Security violation in AI query generation", e);
            return ResponseEntity.status(403)
                .body(ApiResponse.error("Security violation: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error generating SQL from question", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to generate SQL: " + e.getMessage(), null));
        }
    }
    
    // ============================================
    // DASHBOARD MANAGEMENT ENDPOINTS
    // ============================================
    
    @GetMapping("/dashboards")
    public ResponseEntity<ApiResponse<List<DashboardResponse>>> getMyDashboards(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            List<DashboardResponse> dashboards = customDashboardService.getMyDashboards(employeeId);
            return ResponseEntity.ok(ApiResponse.success("Dashboards retrieved successfully", dashboards));
        } catch (Exception e) {
            log.error("Error getting dashboards", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to get dashboards: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/dashboards")
    public ResponseEntity<ApiResponse<DashboardResponse>> createDashboard(
            @RequestBody DashboardRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            DashboardResponse dashboard = customDashboardService.createDashboard(employeeId, request);
            return ResponseEntity.ok(ApiResponse.success("Dashboard created successfully", dashboard));
        } catch (Exception e) {
            log.error("Error creating dashboard", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to create dashboard: " + e.getMessage(), null));
        }
    }
    
    @PutMapping("/dashboards/{dashboardId}")
    public ResponseEntity<ApiResponse<DashboardResponse>> updateDashboard(
            @PathVariable Long dashboardId,
            @RequestBody DashboardRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            DashboardResponse dashboard = customDashboardService.updateDashboard(dashboardId, employeeId, request);
            return ResponseEntity.ok(ApiResponse.success("Dashboard updated successfully", dashboard));
        } catch (SecurityException e) {
            log.warn("Permission denied for dashboard update", e);
            return ResponseEntity.status(403)
                .body(ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating dashboard", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update dashboard: " + e.getMessage(), null));
        }
    }
    
    @DeleteMapping("/dashboards/{dashboardId}")
    public ResponseEntity<ApiResponse<Void>> deleteDashboard(
            @PathVariable Long dashboardId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            customDashboardService.deleteDashboard(dashboardId, employeeId);
            return ResponseEntity.ok(ApiResponse.success("Dashboard deleted successfully", null));
        } catch (SecurityException e) {
            log.warn("Permission denied for dashboard deletion", e);
            return ResponseEntity.status(403)
                .body(ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error deleting dashboard", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to delete dashboard: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/dashboards/{dashboardId}")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @PathVariable Long dashboardId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            DashboardResponse dashboard = customDashboardService.getDashboard(dashboardId, employeeId);
            return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully", dashboard));
        } catch (SecurityException e) {
            log.warn("Permission denied for dashboard access", e);
            return ResponseEntity.status(403)
                .body(ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting dashboard", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to get dashboard: " + e.getMessage(), null));
        }
    }
    
    // ============================================
    // DASHBOARD LAYOUT ENDPOINTS
    // ============================================
    
    @GetMapping("/dashboards/{dashboardId}/layouts")
    public ResponseEntity<ApiResponse<List<DashboardLayoutResponse>>> getDashboardLayouts(
            @PathVariable Long dashboardId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            List<DashboardLayoutResponse> layouts = dashboardLayoutService.getDashboardLayouts(dashboardId, employeeId);
            return ResponseEntity.ok(ApiResponse.success("Dashboard layouts retrieved successfully", layouts));
        } catch (Exception e) {
            log.error("Error getting dashboard layouts", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to get dashboard layouts: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/dashboards/{dashboardId}/layouts")
    public ResponseEntity<ApiResponse<Void>> saveDashboardLayouts(
            @PathVariable Long dashboardId,
            @RequestBody List<DashboardLayoutRequest> layouts,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            dashboardLayoutService.saveDashboardLayouts(dashboardId, employeeId, layouts);
            return ResponseEntity.ok(ApiResponse.success("Dashboard layouts saved successfully", null));
        } catch (Exception e) {
            log.error("Error saving dashboard layouts", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to save dashboard layouts: " + e.getMessage(), null));
        }
    }
    
    @DeleteMapping("/dashboards/{dashboardId}/layouts/{savedQueryId}")
    public ResponseEntity<ApiResponse<Void>> removeDashboardLayout(
            @PathVariable Long dashboardId,
            @PathVariable Long savedQueryId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = 1; // Default for dev mode
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    employeeId = jwtService.extractEmployeeId(token.substring(7));
                } catch (Exception e) {
                    log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
                }
            }
            dashboardLayoutService.removeDashboardLayout(dashboardId, employeeId, savedQueryId);
            return ResponseEntity.ok(ApiResponse.success("Dashboard layout removed successfully", null));
        } catch (Exception e) {
            log.error("Error removing dashboard layout", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to remove dashboard layout: " + e.getMessage(), null));
        }
    }

    // ============================================
    // CUSTOM DASHBOARD EXPORT ENDPOINTS
    // ============================================

    @GetMapping("/dashboards/{dashboardId}/export/excel")
    public ResponseEntity<byte[]> exportCustomDashboardExcel(
            @PathVariable Long dashboardId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = extractEmployeeId(token);
            DashboardResponse dashboard = customDashboardService.getDashboard(dashboardId, employeeId);
            List<DashboardLayoutResponse> layouts = dashboardLayoutService.getDashboardLayouts(dashboardId, employeeId);

            List<ExcelExportService.WidgetExportData> widgetDataList = buildWidgetExportData(layouts);

            byte[] excelData = excelExportService.exportCustomDashboard(
                dashboard.getDashboardName(), widgetDataList);

            String filename = String.format("custom-dashboard-%s.xlsx",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);

            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            log.error("Error exporting custom dashboard to Excel", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/dashboards/{dashboardId}/export/pdf")
    public ResponseEntity<byte[]> exportCustomDashboardPdf(
            @PathVariable Long dashboardId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer employeeId = extractEmployeeId(token);
            DashboardResponse dashboard = customDashboardService.getDashboard(dashboardId, employeeId);
            List<DashboardLayoutResponse> layouts = dashboardLayoutService.getDashboardLayouts(dashboardId, employeeId);

            List<ExcelExportService.WidgetExportData> widgetDataList = buildWidgetExportData(layouts);

            byte[] pdfData = pdfExportService.exportCustomDashboard(
                dashboard.getDashboardName(), widgetDataList);

            String filename = String.format("custom-dashboard-%s.pdf",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return ResponseEntity.ok().headers(headers).body(pdfData);
        } catch (Exception e) {
            log.error("Error exporting custom dashboard to PDF", e);
            return ResponseEntity.status(500).build();
        }
    }

    private Integer extractEmployeeId(String token) {
        Integer employeeId = 1;
        if (token != null && token.startsWith("Bearer ")) {
            try {
                employeeId = jwtService.extractEmployeeId(token.substring(7));
            } catch (Exception e) {
                log.warn("Failed to extract employeeId from token, using default: {}", e.getMessage());
            }
        }
        return employeeId;
    }

    private List<ExcelExportService.WidgetExportData> buildWidgetExportData(
            List<DashboardLayoutResponse> layouts) {
        List<ExcelExportService.WidgetExportData> widgetDataList = new ArrayList<>();

        for (DashboardLayoutResponse layout : layouts) {
            try {
                SavedQuery savedQuery = savedQueryService.getQuery(layout.getSavedQueryId());
                QueryRequest queryDef = savedQueryService.getQueryDefinition(layout.getSavedQueryId());
                QueryResponse result = queryExecutionService.executeQuery(queryDef);

                if (result == null || result.getColumns() == null || result.getRows() == null) {
                    continue;
                }

                List<String> displayNames = new ArrayList<>();
                List<String> columnKeys = new ArrayList<>();
                for (QueryResponse.ColumnInfo col : result.getColumns()) {
                    displayNames.add(col.getDisplayName() != null ? col.getDisplayName() : col.getName());
                    columnKeys.add(col.getDisplayName() != null ? col.getDisplayName() : col.getName());
                }

                widgetDataList.add(new ExcelExportService.WidgetExportData(
                    savedQuery.getName(),
                    displayNames,
                    columnKeys,
                    result.getRows()
                ));
            } catch (Exception e) {
                log.warn("Failed to export widget for savedQueryId {}: {}",
                    layout.getSavedQueryId(), e.getMessage());
            }
        }

        return widgetDataList;
    }
}
