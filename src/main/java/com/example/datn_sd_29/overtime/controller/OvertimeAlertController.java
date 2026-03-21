package com.example.datn_sd_29.overtime.controller;

import com.example.datn_sd_29.overtime.dto.OvertimeAlert;
import com.example.datn_sd_29.overtime.dto.OvertimeAlertResponse;
import com.example.datn_sd_29.overtime.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for managing overtime alerts.
 * Provides endpoints for staff to view and acknowledge overtime alerts.
 * Requirements: 5.1
 */
@RestController
@RequestMapping("/api/overtime/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'RECEPTION')")
public class OvertimeAlertController {
    
    private final AlertService alertService;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    /**
     * GET /api/overtime/alerts
     * Retrieves current active overtime alerts.
     * Requirements: 5.1, 5.5
     * 
     * @return List of active overtime alerts with formatted timestamps
     */
    @GetMapping
    public ResponseEntity<List<OvertimeAlertResponse>> getActiveAlerts() {
        List<OvertimeAlert> alerts = alertService.getActiveAlerts();
        
        List<OvertimeAlertResponse> response = alerts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/overtime/alerts/{id}/acknowledge
     * Acknowledges an overtime alert.
     * Requirements: 5.4
     * 
     * @param id Alert ID to acknowledge
     * @return 204 No Content on success, 404 Not Found if alert doesn't exist
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(@PathVariable String id) {
        // Extract staff ID from authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        // For now, we'll use a placeholder staff ID since the user entity structure
        // doesn't have a numeric ID readily available. In production, this should
        // be extracted from the user entity.
        Integer staffId = 1; // Placeholder - should be extracted from user entity
        
        // Check if alert exists before acknowledging
        List<OvertimeAlert> activeAlerts = alertService.getActiveAlerts();
        boolean alertExists = activeAlerts.stream()
                .anyMatch(alert -> alert.getId().equals(id));
        
        if (!alertExists) {
            return ResponseEntity.notFound().build();
        }
        
        alertService.acknowledgeAlert(id, staffId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Maps OvertimeAlert to OvertimeAlertResponse with formatted timestamps.
     * Requirements: 5.2
     * 
     * @param alert The alert to map
     * @return Formatted response DTO
     */
    private OvertimeAlertResponse mapToResponse(OvertimeAlert alert) {
        // Create table info list
        List<OvertimeAlertResponse.TableInfo> tables = alert.getTableIds().stream()
                .map(tableId -> {
                    int index = alert.getTableIds().indexOf(tableId);
                    String tableName = index < alert.getTableNames().size() 
                            ? alert.getTableNames().get(index) 
                            : "Unknown";
                    
                    return OvertimeAlertResponse.TableInfo.builder()
                            .id(tableId)
                            .name(tableName)
                            .status("OVERTIME")
                            .build();
                })
                .collect(Collectors.toList());
        
        // Format timestamps to ISO 8601
        String nextReservationTime = alert.getNextReservationTime() != null
                ? alert.getNextReservationTime().atZone(ZoneId.systemDefault()).format(ISO_FORMATTER)
                : null;
        
        String generatedAt = alert.getGeneratedAt() != null
                ? alert.getGeneratedAt().atZone(ZoneId.systemDefault()).format(ISO_FORMATTER)
                : null;
        
        return OvertimeAlertResponse.builder()
                .id(alert.getId())
                .tables(tables)
                .diningDuration(alert.getDiningDuration())
                .nextReservationTime(nextReservationTime)
                .invoiceCode(alert.getInvoiceCode())
                .urgency(alert.getUrgency() != null ? alert.getUrgency().name() : null)
                .generatedAt(generatedAt)
                .build();
    }
}
