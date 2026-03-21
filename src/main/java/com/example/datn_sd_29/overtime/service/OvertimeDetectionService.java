package com.example.datn_sd_29.overtime.service;

import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.overtime.dto.OvertimeDetectionResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for detecting overtime tables and processing alerts.
 * Requirements: 1.1
 */
public interface OvertimeDetectionService {
    
    /**
     * Scans all active invoices and detects overtime tables.
     * Requirements: 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 3.5
     * 
     * @return OvertimeDetectionResult containing processed count and alerts generated
     */
    OvertimeDetectionResult detectAndProcessOvertimeTables();
    
    /**
     * Calculates dining duration for an invoice.
     * Requirements: 1.3
     * 
     * @param invoice The invoice to check
     * @return Duration in minutes
     */
    Long calculateDiningDuration(Invoice invoice);
    
    /**
     * Finds the next reservation for a table within the alert window.
     * Requirements: 3.1, 3.2, 3.3
     * 
     * @param tableId The table to check
     * @param currentTime Current timestamp
     * @return Optional<Invoice> if found within 120 minutes
     */
    Optional<Invoice> findNextReservation(Integer tableId, LocalDateTime currentTime);
    
    /**
     * Updates table status to OVERTIME.
     * Requirements: 1.2, 9.1
     * 
     * @param tableIds List of table IDs to update
     */
    void markTablesAsOvertime(List<Integer> tableIds);
}
