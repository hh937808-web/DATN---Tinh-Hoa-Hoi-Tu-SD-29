package com.example.datn_sd_29.overtime.service;

import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.overtime.dto.OvertimeDetectionResult;
import com.example.datn_sd_29.overtime.service.impl.OvertimeDetectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OvertimeDetectionService.
 * Tests specific examples and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class OvertimeDetectionServiceTest {
    
    @Mock
    private InvoiceRepository invoiceRepository;
    
    @Mock
    private DiningTableRepository diningTableRepository;
    
    @Mock
    private InvoiceDiningTableRepository invoiceDiningTableRepository;
    
    @Mock
    private AlertService alertService;
    
    @InjectMocks
    private OvertimeDetectionServiceImpl overtimeDetectionService;
    
    @BeforeEach
    void setUp() {
        // Reset mocks before each test
    }
    
    @Test
    void testCalculateDiningDuration_WithValidCheckedInTime() {
        // Given
        Invoice invoice = new Invoice();
        Instant checkedInAt = Instant.now().minus(95, ChronoUnit.MINUTES);
        invoice.setCheckedInAt(checkedInAt);
        
        // When
        Long duration = overtimeDetectionService.calculateDiningDuration(invoice);
        
        // Then
        assertTrue(duration >= 94 && duration <= 96, 
                "Duration should be approximately 95 minutes, got: " + duration);
    }
    
    @Test
    void testCalculateDiningDuration_WithNullCheckedInTime() {
        // Given
        Invoice invoice = new Invoice();
        invoice.setCheckedInAt(null);
        
        // When
        Long duration = overtimeDetectionService.calculateDiningDuration(invoice);
        
        // Then
        assertEquals(0L, duration, "Duration should be 0 when checked_in_at is null");
    }
    
    @Test
    void testMarkTablesAsOvertime_WithValidTableIds() {
        // Given
        List<Integer> tableIds = List.of(1, 2, 3);
        
        // When
        overtimeDetectionService.markTablesAsOvertime(tableIds);
        
        // Then
        verify(diningTableRepository, times(1))
                .updateTableStatusByIdIn(tableIds, "OVERTIME");
    }
    
    @Test
    void testMarkTablesAsOvertime_WithEmptyList() {
        // Given
        List<Integer> tableIds = new ArrayList<>();
        
        // When
        overtimeDetectionService.markTablesAsOvertime(tableIds);
        
        // Then
        verify(diningTableRepository, never())
                .updateTableStatusByIdIn(any(), any());
    }
    
    @Test
    void testMarkTablesAsOvertime_WithNullList() {
        // When
        overtimeDetectionService.markTablesAsOvertime(null);
        
        // Then
        verify(diningTableRepository, never())
                .updateTableStatusByIdIn(any(), any());
    }
    
    @Test
    void testFindNextReservation_WithReservationFound() {
        // Given
        Integer tableId = 1;
        LocalDateTime currentTime = LocalDateTime.now();
        Invoice reservation = new Invoice();
        reservation.setInvoiceCode("RES-001");
        reservation.setReservedAt(currentTime.plusMinutes(30));
        
        when(invoiceRepository.findFirstByDiningTableIdAndInvoiceStatusAndReservedAtBetweenOrderByReservedAtAsc(
                eq(tableId), eq("RESERVED"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(reservation));
        
        // When
        Optional<Invoice> result = overtimeDetectionService.findNextReservation(tableId, currentTime);
        
        // Then
        assertTrue(result.isPresent(), "Should find next reservation");
        assertEquals("RES-001", result.get().getInvoiceCode());
    }
    
    @Test
    void testFindNextReservation_WithNoReservationFound() {
        // Given
        Integer tableId = 1;
        LocalDateTime currentTime = LocalDateTime.now();
        
        when(invoiceRepository.findFirstByDiningTableIdAndInvoiceStatusAndReservedAtBetweenOrderByReservedAtAsc(
                eq(tableId), eq("RESERVED"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        
        // When
        Optional<Invoice> result = overtimeDetectionService.findNextReservation(tableId, currentTime);
        
        // Then
        assertFalse(result.isPresent(), "Should not find next reservation");
    }
    
    @Test
    void testDetectAndProcessOvertimeTables_WithNoOvertimeInvoices() {
        // Given
        when(invoiceRepository.findByInvoiceStatusAndCheckedInAtBefore(
                eq("IN_PROGRESS"), any(Instant.class)))
                .thenReturn(new ArrayList<>());
        
        // When
        OvertimeDetectionResult result = overtimeDetectionService.detectAndProcessOvertimeTables();
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.getTablesProcessed());
        assertEquals(0, result.getOvertimeDetected());
        assertEquals(0, result.getAlertsGenerated());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void testDetectAndProcessOvertimeTables_ExecutionTimeTracking() {
        // Given
        when(invoiceRepository.findByInvoiceStatusAndCheckedInAtBefore(
                eq("IN_PROGRESS"), any(Instant.class)))
                .thenReturn(new ArrayList<>());
        
        // When
        OvertimeDetectionResult result = overtimeDetectionService.detectAndProcessOvertimeTables();
        
        // Then
        assertNotNull(result.getExecutionTimeMs());
        assertTrue(result.getExecutionTimeMs() >= 0, 
                "Execution time should be non-negative");
    }
}
