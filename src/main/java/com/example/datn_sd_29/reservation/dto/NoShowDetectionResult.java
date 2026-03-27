package com.example.datn_sd_29.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of no-show detection process
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoShowDetectionResult {
    
    /**
     * Total number of reservations checked
     */
    private int reservationsChecked;
    
    /**
     * Number of no-shows detected
     */
    private int noShowsDetected;
    
    /**
     * Number of reservations auto-cancelled
     */
    private int autoCancelled;
    
    /**
     * Number of notifications sent to staff
     */
    private int notificationsSent;
    
    /**
     * List of errors encountered during processing
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    /**
     * Add an error message
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }
}
