package com.example.datn_sd_29.reservation.scheduler;

import com.example.datn_sd_29.common.service.TableStatusBroadcastService;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled job that monitors reserved tables and marks them as NO_SHOW
 * if customers don't arrive within the grace period.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "reservation.noshow.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class NoShowMonitorScheduler {
    
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final DiningTableRepository diningTableRepository;
    private final TableStatusBroadcastService tableStatusBroadcastService;
    
    /**
     * Monitors reserved tables and marks as NO_SHOW if grace period expired.
     * Runs every 5 minutes with 30-second initial delay.
     */
    @Scheduled(fixedRateString = "${reservation.noshow.monitor.fixed-rate:300000}", 
               initialDelayString = "${reservation.noshow.monitor.initial-delay:30000}")
    @Transactional
    public void monitorNoShowReservations() {
        log.debug("Starting no-show reservation monitoring cycle");
        
        try {
            // Get grace period from config (default 15 minutes)
            int gracePeriodMinutes = 15; // Can be made configurable
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(gracePeriodMinutes);
            
            // Find all RESERVED invoices where reserved_at has passed the grace period
            List<Invoice> expiredReservations = invoiceRepository.findExpiredReservations(cutoffTime);
            
            if (expiredReservations.isEmpty()) {
                log.debug("No expired reservations found");
                return;
            }
            
            log.info("Found {} expired reservations to mark as NO_SHOW", expiredReservations.size());
            
            int processedCount = 0;
            for (Invoice invoice : expiredReservations) {
                try {
                    // Update invoice status to NO_SHOW
                    invoice.setInvoiceStatus("NO_SHOW");
                    invoiceRepository.save(invoice);
                    
                    // Get all tables associated with this invoice
                    List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository
                            .findByInvoiceIdWithTable(invoice.getId());
                    
                    if (!invoiceTables.isEmpty()) {
                        List<Integer> tableIds = invoiceTables.stream()
                                .map(idt -> idt.getDiningTable().getId())
                                .collect(Collectors.toList());
                        
                        // Release tables back to AVAILABLE
                        diningTableRepository.updateTableStatusByIdIn(tableIds, "AVAILABLE");
                        tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "AVAILABLE");
                        
                        log.info("Marked reservation {} as NO_SHOW and released {} tables", 
                                invoice.getReservationCode(), tableIds.size());
                    }
                    
                    processedCount++;
                    
                } catch (Exception e) {
                    log.error("Error processing reservation {}: {}", 
                            invoice.getReservationCode(), e.getMessage(), e);
                }
            }
            
            log.info("No-show monitoring completed: {} reservations marked as NO_SHOW", processedCount);
            
        } catch (Exception e) {
            log.error("Fatal error in no-show monitoring: {}", e.getMessage(), e);
        }
        
        log.debug("No-show reservation monitoring cycle completed");
    }
}
