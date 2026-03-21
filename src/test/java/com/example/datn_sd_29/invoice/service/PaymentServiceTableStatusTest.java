package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService table status clearing functionality
 * Tests Requirements 4.1, 4.2, 4.3, 4.4 from table-overtime-smart-alerts spec
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTableStatusTest {

    @Mock
    private InvoiceDiningTableRepository invoiceDiningTableRepository;

    @Mock
    private DiningTableRepository diningTableRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void cancelByTable_shouldClearTableStatusToAvailable() {
        // Arrange
        Integer tableId = 1;
        Invoice invoice = new Invoice();
        invoice.setId(100);
        invoice.setInvoiceStatus("IN_PROGRESS");
        invoice.setInvoiceCode("INV-001");

        DiningTable table1 = new DiningTable();
        table1.setId(1);
        table1.setTableName("Table 1");

        DiningTable table2 = new DiningTable();
        table2.setId(2);
        table2.setTableName("Table 2");

        InvoiceDiningTable link1 = new InvoiceDiningTable();
        link1.setDiningTable(table1);

        InvoiceDiningTable link2 = new InvoiceDiningTable();
        link2.setDiningTable(table2);

        when(invoiceDiningTableRepository.findInvoiceByTableAndStatus(tableId, "IN_PROGRESS"))
                .thenReturn(Optional.of(invoice));
        when(invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId()))
                .thenReturn(List.of(link1, link2));

        // Act
        paymentService.cancelByTable(tableId);

        // Assert
        verify(invoiceRepository).save(argThat(inv -> 
            "CANCELLED".equals(inv.getInvoiceStatus())
        ));
        
        ArgumentCaptor<List<Integer>> tableIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(diningTableRepository).updateTableStatusByIdIn(tableIdsCaptor.capture(), eq("AVAILABLE"));
        
        List<Integer> capturedTableIds = tableIdsCaptor.getValue();
        assertEquals(2, capturedTableIds.size());
        assertTrue(capturedTableIds.contains(1));
        assertTrue(capturedTableIds.contains(2));
        
        // Verify WebSocket broadcast
        verify(messagingTemplate).convertAndSend(eq("/topic/table-status"), any(Object.class));
    }

    @Test
    void cancelByTable_shouldHandleSingleTable() {
        // Arrange
        Integer tableId = 1;
        Invoice invoice = new Invoice();
        invoice.setId(100);
        invoice.setInvoiceStatus("IN_PROGRESS");

        DiningTable table = new DiningTable();
        table.setId(1);

        InvoiceDiningTable link = new InvoiceDiningTable();
        link.setDiningTable(table);

        when(invoiceDiningTableRepository.findInvoiceByTableAndStatus(tableId, "IN_PROGRESS"))
                .thenReturn(Optional.of(invoice));
        when(invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId()))
                .thenReturn(List.of(link));

        // Act
        paymentService.cancelByTable(tableId);

        // Assert
        ArgumentCaptor<List<Integer>> tableIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(diningTableRepository).updateTableStatusByIdIn(tableIdsCaptor.capture(), eq("AVAILABLE"));
        
        List<Integer> capturedTableIds = tableIdsCaptor.getValue();
        assertEquals(1, capturedTableIds.size());
        assertEquals(1, capturedTableIds.get(0));
    }

    @Test
    void cancelByTable_shouldThrowExceptionWhenNoActiveInvoice() {
        // Arrange
        Integer tableId = 1;
        when(invoiceDiningTableRepository.findInvoiceByTableAndStatus(tableId, "IN_PROGRESS"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> 
            paymentService.cancelByTable(tableId)
        );
        
        verify(diningTableRepository, never()).updateTableStatusByIdIn(any(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void cancelByTable_shouldThrowExceptionWhenInvoiceAlreadyPaid() {
        // Arrange
        Integer tableId = 1;
        Invoice invoice = new Invoice();
        invoice.setId(100);
        invoice.setInvoiceStatus("PAID");

        when(invoiceDiningTableRepository.findInvoiceByTableAndStatus(tableId, "IN_PROGRESS"))
                .thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> 
            paymentService.cancelByTable(tableId)
        );
        
        verify(diningTableRepository, never()).updateTableStatusByIdIn(any(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
