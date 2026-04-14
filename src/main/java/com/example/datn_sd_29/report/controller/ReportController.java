package com.example.datn_sd_29.report.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.report.dto.ProductReportResponse;
import com.example.datn_sd_29.report.dto.RevenueReportResponse;
import com.example.datn_sd_29.report.service.ExcelExportService;
import com.example.datn_sd_29.report.service.PdfExportService;
import com.example.datn_sd_29.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            RevenueReportResponse report = reportService.generateRevenueReport(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo doanh thu thành công", report));
        } catch (Exception e) {
            log.error("Error generating revenue report: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                ApiResponse.error("Lỗi: " + e.getMessage(), null)
            );
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<ProductReportResponse>> getProductReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            ProductReportResponse report = reportService.generateProductReport(startDate, endDate, limit);
            return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo món ăn thành công", report));
        } catch (Exception e) {
            log.error("Error generating product report: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                ApiResponse.error("Lỗi: " + e.getMessage(), null)
            );
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/revenue/export/excel")
    public ResponseEntity<byte[]> exportRevenueExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            RevenueReportResponse report = reportService.generateRevenueReport(startDate, endDate);
            byte[] excelData = excelExportService.exportRevenueReport(report);
            
            String filename = String.format("bao-cao-doanh-thu-%s.xlsx", 
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
        } catch (Exception e) {
            log.error("Error exporting revenue report to Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/revenue/export/pdf")
    public ResponseEntity<byte[]> exportRevenuePdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            RevenueReportResponse report = reportService.generateRevenueReport(startDate, endDate);
            byte[] pdfData = pdfExportService.exportRevenueReport(report);
            
            String filename = String.format("bao-cao-doanh-thu-%s.pdf", 
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);
        } catch (Exception e) {
            log.error("Error exporting revenue report to PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/products/export/excel")
    public ResponseEntity<byte[]> exportProductExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            ProductReportResponse report = reportService.generateProductReport(startDate, endDate, limit);
            byte[] excelData = excelExportService.exportProductReport(report);
            
            String filename = String.format("bao-cao-mon-an-%s.xlsx", 
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
        } catch (Exception e) {
            log.error("Error exporting product report to Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/products/export/pdf")
    public ResponseEntity<byte[]> exportProductPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            ProductReportResponse report = reportService.generateProductReport(startDate, endDate, limit);
            byte[] pdfData = pdfExportService.exportProductReport(report);
            
            String filename = String.format("bao-cao-mon-an-%s.pdf", 
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);
        } catch (Exception e) {
            log.error("Error exporting product report to PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
