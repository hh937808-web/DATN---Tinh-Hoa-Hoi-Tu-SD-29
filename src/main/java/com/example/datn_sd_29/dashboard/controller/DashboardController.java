package com.example.datn_sd_29.dashboard.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.dashboard.dto.DashboardStatsResponse;
import com.example.datn_sd_29.dashboard.dto.RecentInvoiceResponse;
import com.example.datn_sd_29.dashboard.dto.TopProductResponse;
import com.example.datn_sd_29.dashboard.dto.InvoicePageResponse;
import com.example.datn_sd_29.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (endDate == null) {
                endDate = startDate;
            }
            DashboardStatsResponse stats = dashboardService.getStats(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("Lấy thống kê thành công", stats));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                ApiResponse.error("Lỗi: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "5") int limit
    ) {
        try {
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (endDate == null) {
                endDate = startDate;
            }
            List<TopProductResponse> products = dashboardService.getTopProducts(startDate, endDate, limit);
            return ResponseEntity.ok(ApiResponse.success("Lấy sản phẩm bán chạy thành công", products));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                ApiResponse.error("Lỗi: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/recent-invoices")
    public ResponseEntity<ApiResponse<List<RecentInvoiceResponse>>> getRecentInvoices(
            @RequestParam(defaultValue = "5") int limit
    ) {
        try {
            List<RecentInvoiceResponse> invoices = dashboardService.getRecentInvoices(limit);
            return ResponseEntity.ok(ApiResponse.success("Lấy hóa đơn gần đây thành công", invoices));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                ApiResponse.error("Lỗi: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/table-status")
    public ResponseEntity<ApiResponse<List<com.example.datn_sd_29.dashboard.dto.TableStatusResponse>>> getTableStatus() {
        try {
            List<com.example.datn_sd_29.dashboard.dto.TableStatusResponse> tables = dashboardService.getTableStatus();
            return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái bàn thành công", tables));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                ApiResponse.error("Lỗi: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/table-detail/{tableId}")
    public ResponseEntity<ApiResponse<com.example.datn_sd_29.dashboard.dto.TableDetailResponse>> getTableDetail(
            @PathVariable Integer tableId
    ) {
        try {
            com.example.datn_sd_29.dashboard.dto.TableDetailResponse detail = dashboardService.getTableDetail(tableId);
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin bàn thành công", detail));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                ApiResponse.error("Lỗi: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<ApiResponse<com.example.datn_sd_29.dashboard.dto.RevenueChartResponse>> getRevenueChart(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (endDate == null) {
                endDate = startDate;
            }
            com.example.datn_sd_29.dashboard.dto.RevenueChartResponse chart = dashboardService.getRevenueChart(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("Lấy biểu đồ doanh thu thành công", chart));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                ApiResponse.error("Lỗi: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<InvoicePageResponse>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(defaultValue = "time") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        try {
            InvoicePageResponse response = dashboardService.getAllInvoices(
                page, size, status, startDate, endDate, search, paymentMethod, sortBy, sortDirection
            );
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hóa đơn thành công", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                ApiResponse.error("Lỗi: " + e.getMessage(), null)
            );
        }
    }
}
