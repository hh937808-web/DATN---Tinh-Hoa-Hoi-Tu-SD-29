package com.example.datn_sd_29.overtime.service.impl;

import com.example.datn_sd_29.common.service.TableStatusBroadcastService;
import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.overtime.dto.OvertimeAlert;
import com.example.datn_sd_29.overtime.dto.OvertimeDetectionResult;
import com.example.datn_sd_29.overtime.enums.AlertUrgency;
import com.example.datn_sd_29.overtime.service.AlertService;
import com.example.datn_sd_29.overtime.service.OvertimeDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of OvertimeDetectionService.
 * Requirements: 1.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OvertimeDetectionServiceImpl implements OvertimeDetectionService {
    
    private final InvoiceRepository invoiceRepository;
    private final DiningTableRepository diningTableRepository;
    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final AlertService alertService;
    private final TableStatusBroadcastService tableStatusBroadcastService;
    
    private static final long OVERTIME_THRESHOLD_MINUTES = 90;
    private static final long ALERT_WINDOW_MINUTES = 120;
    
    @Override
    @Transactional
    public OvertimeDetectionResult detectAndProcessOvertimeTables() {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        int tablesProcessed = 0;
        int overtimeDetected = 0;
        int alertsGenerated = 0;
        
        try {
            Instant currentTime = Instant.now();
            Instant thresholdTime = currentTime.minus(OVERTIME_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
            
            // Query all IN_PROGRESS invoices with checked_in_at before threshold
            List<Invoice> overtimeInvoices = invoiceRepository
                    .findByInvoiceStatusAndCheckedInAtBefore("IN_PROGRESS", thresholdTime);
            
            log.info("Found {} invoices exceeding {} minutes", 
                    overtimeInvoices.size(), OVERTIME_THRESHOLD_MINUTES);
            
            for (Invoice invoice : overtimeInvoices) {
                try {
                    // Get all tables associated with this invoice
                    List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository
                            .findByInvoiceIdWithTable(invoice.getId());
                    
                    if (invoiceTables.isEmpty()) {
                        log.warn("Invoice {} has no associated tables", invoice.getInvoiceCode());
                        continue;
                    }
                    
                    List<Integer> tableIds = invoiceTables.stream()
                            .map(idt -> idt.getDiningTable().getId())
                            .collect(Collectors.toList());
                    
                    List<String> tableNames = invoiceTables.stream()
                            .map(idt -> idt.getDiningTable().getTableName())
                            .collect(Collectors.toList());
                    
                    tablesProcessed += tableIds.size();
                    
                    // Check for next reservation within 120 minutes for any table
                    LocalDateTime currentLocalTime = LocalDateTime.ofInstant(currentTime, ZoneId.systemDefault());
                    Optional<Invoice> nextReservation = findNextReservationForAnyTable(tableIds, currentLocalTime);
                    
                    // Update all tables to OVERTIME status
                    markTablesAsOvertime(tableIds);
                    overtimeDetected += tableIds.size();
                    
                    // Generate alert only if next reservation exists
                    if (nextReservation.isPresent()) {
                        Invoice reservation = nextReservation.get();
                        Long diningDuration = calculateDiningDuration(invoice);
                        
                        OvertimeAlert alert = OvertimeAlert.builder()
                                .id(UUID.randomUUID().toString())
                                .tableNames(tableNames)
                                .tableIds(tableIds)
                                .diningDuration(diningDuration)
                                .nextReservationTime(reservation.getReservedAt())
                                .invoiceCode(invoice.getInvoiceCode())
                                .generatedAt(currentTime)
                                .urgency(calculateUrgency(reservation.getReservedAt(), currentLocalTime))
                                .build();
                        
                        alertService.broadcastOvertimeAlert(alert);
                        alertsGenerated++;
                        
                        log.info("Generated alert for tables {} with next reservation at {}", 
                                tableNames, reservation.getReservedAt());
                    } else {
                        log.debug("No next reservation found for tables {}, skipping alert", tableNames);
                    }
                    
                } catch (Exception e) {
                    String error = String.format("Error processing invoice %s: %s", 
                            invoice.getInvoiceCode(), e.getMessage());
                    errors.add(error);
                    log.error(error, e);
                }
            }
            
        } catch (Exception e) {
            String error = "Fatal error in overtime detection: " + e.getMessage();
            errors.add(error);
            log.error(error, e);
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        OvertimeDetectionResult result = OvertimeDetectionResult.builder()
                .tablesProcessed(tablesProcessed)
                .overtimeDetected(overtimeDetected)
                .alertsGenerated(alertsGenerated)
                .executionTimeMs(executionTime)
                .errors(errors)
                .build();
        
        log.info("Overtime detection completed: {} tables processed, {} overtime detected, {} alerts generated in {}ms",
                tablesProcessed, overtimeDetected, alertsGenerated, executionTime);
        
        return result;
    }
    
    @Override
    public Long calculateDiningDuration(Invoice invoice) {
        if (invoice.getCheckedInAt() == null) {
            return 0L;
        }
        Instant currentTime = Instant.now();
        return Duration.between(invoice.getCheckedInAt(), currentTime).toMinutes();
    }
    
    @Override
    public Optional<Invoice> findNextReservation(Integer tableId, LocalDateTime currentTime) {
        LocalDateTime endTime = currentTime.plusMinutes(ALERT_WINDOW_MINUTES);
        List<Invoice> invoices = invoiceRepository.findFirstByDiningTableIdAndInvoiceStatusAndReservedAtBetweenOrderByReservedAtAsc(
                tableId, "RESERVED", currentTime, endTime);
        return invoices.isEmpty() ? Optional.empty() : Optional.of(invoices.get(0));
    }
    
    @Override
    @Transactional
    public void markTablesAsOvertime(List<Integer> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            return;
        }
        diningTableRepository.updateTableStatusByIdIn(tableIds, "OVERTIME");
        tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "OVERTIME");
        log.debug("Marked {} tables as OVERTIME", tableIds.size());
    }
    
    /**
     * Finds the next reservation for any table in the provided list.
     * Requirements: 3.5
     */
    private Optional<Invoice> findNextReservationForAnyTable(List<Integer> tableIds, LocalDateTime currentTime) {
        for (Integer tableId : tableIds) {
            Optional<Invoice> reservation = findNextReservation(tableId, currentTime);
            if (reservation.isPresent()) {
                return reservation;
            }
        }
        return Optional.empty();
    }
    
    /**
     * Calculates alert urgency based on time until next reservation.
     * CRITICAL: < 30 minutes - Immediate action required
     * HIGH: < 60 minutes - Action needed soon
     * MEDIUM: < 120 minutes - Monitor closely
     * LOW: > 120 minutes - Informational only
     * Requirements: 5.3
     */
    private AlertUrgency calculateUrgency(LocalDateTime nextReservationTime, LocalDateTime currentTime) {
        long minutesUntilReservation = Duration.between(currentTime, nextReservationTime).toMinutes();
        
        if (minutesUntilReservation < 30) {
            return AlertUrgency.CRITICAL;
        } else if (minutesUntilReservation < 60) {
            return AlertUrgency.HIGH;
        } else if (minutesUntilReservation < 120) {
            return AlertUrgency.MEDIUM;
        } else {
            return AlertUrgency.LOW;
        }
    }
}
