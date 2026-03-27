package com.example.datn_sd_29.reservation.scheduler;

import com.example.datn_sd_29.reservation.dto.NoShowDetectionResult;
import com.example.datn_sd_29.reservation.service.NoShowDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that monitors no-show reservations.
 * Executes every 5 minutes (configurable) to detect reservations where customers didn't show up.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "reservation.noshow.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class NoShowMonitorScheduler {
    
    private final NoShowDetectionService noShowDetectionService;
    
    /**
     * Monitors no-show reservations on a fixed schedule.
     * Executes every 5 minutes (300000ms) with 30-second initial delay.
     * Implements retry logic with exponential backoff on failures.
     */
    @Scheduled(fixedRateString = "${reservation.noshow.monitor.fixed-rate:300000}", 
               initialDelayString = "${reservation.noshow.monitor.initial-delay:30000}")
    public void monitorNoShowReservations() {
        log.debug("Starting no-show reservation monitoring cycle");
        
        long startTime = System.currentTimeMillis();
        int maxRetries = 3;
        int retryCount = 0;
        NoShowDetectionResult result = null;
        
        while (retryCount < maxRetries) {
            try {
                result = noShowDetectionService.detectAndProcessNoShows();
                
                long executionTime = System.currentTimeMillis() - startTime;
                
                // Log execution metrics
                log.info("No-show monitoring completed - Reservations checked: {}, No-shows detected: {}, Auto-cancelled: {}, Notifications sent: {}, Execution time: {}ms",
                        result.getReservationsChecked(),
                        result.getNoShowsDetected(),
                        result.getAutoCancelled(),
                        result.getNotificationsSent(),
                        executionTime);
                
                // Log warning if execution time exceeds threshold
                if (executionTime > 5000) {
                    log.warn("No-show monitoring execution time exceeded 5 seconds: {}ms - Reservations checked: {}, No-shows detected: {}",
                            executionTime,
                            result.getReservationsChecked(),
                            result.getNoShowsDetected());
                }
                
                // Log any errors from the detection process
                if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                    log.warn("No-show monitoring completed with {} errors: {}", 
                            result.getErrors().size(), 
                            result.getErrors());
                }
                
                // Success - exit retry loop
                break;
                
            } catch (Exception e) {
                retryCount++;
                
                if (retryCount >= maxRetries) {
                    log.error("No-show monitoring failed after {} attempts. Error: {}", 
                            maxRetries, 
                            e.getMessage(), 
                            e);
                } else {
                    // Calculate exponential backoff delay
                    long backoffDelay = (long) Math.pow(2, retryCount) * 1000;
                    
                    log.warn("No-show monitoring attempt {} failed, retrying in {}ms. Error: {}", 
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
        
        log.debug("No-show reservation monitoring cycle completed");
    }
}
