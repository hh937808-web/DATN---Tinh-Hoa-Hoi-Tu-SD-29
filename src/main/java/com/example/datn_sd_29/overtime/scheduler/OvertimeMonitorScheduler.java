package com.example.datn_sd_29.overtime.scheduler;

import com.example.datn_sd_29.overtime.dto.OvertimeDetectionResult;
import com.example.datn_sd_29.overtime.service.OvertimeDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that monitors table overtime status.
 * Executes every 60 seconds to detect tables exceeding dining time limits.
 * Requirements: 1.1
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "overtime.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class OvertimeMonitorScheduler {
    
    private final OvertimeDetectionService overtimeDetectionService;
    
    /**
     * Monitors overtime tables on a fixed schedule.
     * Executes every 60 seconds with 10-second initial delay.
     * Implements retry logic with exponential backoff on failures.
     * Requirements: 1.1, 7.1, 7.4, 7.5, 9.4, 9.5
     */
    @Scheduled(fixedRateString = "${overtime.monitor.fixed-rate:60000}", 
               initialDelayString = "${overtime.monitor.initial-delay:10000}")
    public void monitorOvertimeTables() {
        log.debug("Starting overtime table monitoring cycle");
        
        long startTime = System.currentTimeMillis();
        int maxRetries = 3;
        int retryCount = 0;
        OvertimeDetectionResult result = null;
        
        while (retryCount < maxRetries) {
            try {
                result = overtimeDetectionService.detectAndProcessOvertimeTables();
                
                long executionTime = System.currentTimeMillis() - startTime;
                
                // Log execution metrics
                log.info("Overtime monitoring completed - Tables processed: {}, Overtime detected: {}, Alerts generated: {}, Execution time: {}ms",
                        result.getTablesProcessed(),
                        result.getOvertimeDetected(),
                        result.getAlertsGenerated(),
                        executionTime);
                
                // Log warning if execution time exceeds threshold
                if (executionTime > 10000) {
                    log.warn("Overtime monitoring execution time exceeded 10 seconds: {}ms - Tables processed: {}, Overtime detected: {}",
                            executionTime,
                            result.getTablesProcessed(),
                            result.getOvertimeDetected());
                }
                
                // Log any errors from the detection process
                if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                    log.warn("Overtime monitoring completed with {} errors: {}", 
                            result.getErrors().size(), 
                            result.getErrors());
                }
                
                // Success - exit retry loop
                break;
                
            } catch (Exception e) {
                retryCount++;
                
                if (retryCount >= maxRetries) {
                    log.error("Overtime monitoring failed after {} attempts. Error: {}", 
                            maxRetries, 
                            e.getMessage(), 
                            e);
                } else {
                    // Calculate exponential backoff delay
                    long backoffDelay = (long) Math.pow(2, retryCount) * 1000;
                    
                    log.warn("Overtime monitoring attempt {} failed, retrying in {}ms. Error: {}", 
                            retryCount, 
                            backoffDelay, 
                            e.getMessage());
                    
                    try {
                        Thread.sleep(backoffDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry sleep interrupted", ie);
                        break;
                    }
                }
            }
        }
        
        log.debug("Overtime table monitoring cycle completed");
    }
}
