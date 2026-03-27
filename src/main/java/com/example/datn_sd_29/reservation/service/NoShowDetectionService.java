package com.example.datn_sd_29.reservation.service;

import com.example.datn_sd_29.common.service.EmailService;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.reservation.dto.NoShowDetectionResult;
import com.example.datn_sd_29.reservation.dto.NoShowNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for detecting and handling no-show reservations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoShowDetectionService {
    
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    
    @Value("${reservation.noshow.grace-period-minutes:15}")
    private int gracePeriodMinutes;
    
    private static final String INVOICE_STATUS_RESERVED = "RESERVED";
    private static final String INVOICE_STATUS_CANCELLED = "CANCELLED";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    /**
     * Detect and process no-show reservations
     * 
     * @return Detection result with statistics
     */
    @Transactional
    public NoShowDetectionResult detectAndProcessNoShows() {
        log.debug("Starting no-show detection process");
        
        NoShowDetectionResult result = NoShowDetectionResult.builder()
                .reservationsChecked(0)
                .noShowsDetected(0)
                .autoCancelled(0)
                .notificationsSent(0)
                .build();
        
        try {
            // Calculate cutoff time: reserved_at + grace_period
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(gracePeriodMinutes);
            
            // Find expired reservations
            List<Invoice> expiredReservations = invoiceRepository.findExpiredReservations(cutoffTime);
            result.setReservationsChecked(expiredReservations.size());
            
            log.debug("Found {} expired reservations (cutoff: {})", 
                    expiredReservations.size(), 
                    cutoffTime.format(FORMATTER));
            
            // Process each expired reservation
            for (Invoice invoice : expiredReservations) {
                try {
                    processNoShow(invoice, result);
                } catch (Exception e) {
                    String error = String.format("Failed to process no-show for reservation %s: %s", 
                            invoice.getReservationCode(), 
                            e.getMessage());
                    log.error(error, e);
                    result.addError(error);
                }
            }
            
            log.info("No-show detection completed - Checked: {}, Detected: {}, Auto-cancelled: {}, Notifications: {}",
                    result.getReservationsChecked(),
                    result.getNoShowsDetected(),
                    result.getAutoCancelled(),
                    result.getNotificationsSent());
            
        } catch (Exception e) {
            String error = "No-show detection process failed: " + e.getMessage();
            log.error(error, e);
            result.addError(error);
        }
        
        return result;
    }
    
    /**
     * Process a single no-show reservation
     */
    private void processNoShow(Invoice invoice, NoShowDetectionResult result) {
        log.debug("Processing no-show for reservation: {}", invoice.getReservationCode());
        
        // Auto-cancel the reservation
        invoice.setInvoiceStatus(INVOICE_STATUS_CANCELLED);
        invoiceRepository.save(invoice);
        
        result.setNoShowsDetected(result.getNoShowsDetected() + 1);
        result.setAutoCancelled(result.getAutoCancelled() + 1);
        
        // Send notification to staff via WebSocket
        try {
            sendNoShowNotification(invoice);
            result.setNotificationsSent(result.getNotificationsSent() + 1);
        } catch (Exception e) {
            log.error("Failed to send no-show notification for {}: {}", 
                    invoice.getReservationCode(), 
                    e.getMessage());
        }
        
        // Send email to customer (optional - can be disabled)
        try {
            sendNoShowEmail(invoice);
        } catch (Exception e) {
            log.warn("Failed to send no-show email to customer {}: {}", 
                    invoice.getCustomer().getPhoneNumber(), 
                    e.getMessage());
        }
        
        log.info("No-show processed and auto-cancelled: {} (Customer: {}, Reserved: {})",
                invoice.getReservationCode(),
                invoice.getCustomer().getPhoneNumber(),
                invoice.getReservedAt().format(FORMATTER));
    }
    
    /**
     * Send WebSocket notification to staff
     */
    private void sendNoShowNotification(Invoice invoice) {
        String tableNames = getTableNames(invoice);
        
        NoShowNotification notification = NoShowNotification.builder()
                .reservationCode(invoice.getReservationCode())
                .customerName(invoice.getCustomer().getFullName())
                .phoneNumber(invoice.getCustomer().getPhoneNumber())
                .reservedAt(invoice.getReservedAt())
                .tableNames(tableNames)
                .guestCount(invoice.getGuestCount())
                .detectedAt(LocalDateTime.now())
                .notificationType("NO_SHOW_AUTO_CANCELLED")
                .message(String.format("Đặt bàn %s đã bị hủy tự động do khách không đến", 
                        invoice.getReservationCode()))
                .build();
        
        messagingTemplate.convertAndSend("/topic/noshow", notification);
        
        log.debug("No-show notification sent via WebSocket for {}", invoice.getReservationCode());
    }
    
    /**
     * Send email to customer about no-show
     */
    private void sendNoShowEmail(Invoice invoice) {
        if (invoice.getCustomer() == null || invoice.getCustomer().getEmail() == null) {
            log.debug("No email address for customer, skipping email notification");
            return;
        }
        
        String subject = "Thông báo hủy đặt bàn - " + invoice.getReservationCode();
        String tableNames = getTableNames(invoice);
        
        // Build email body
        String body = String.format("""
                Kính gửi Quý khách %s,
                
                Đặt bàn của Quý khách đã bị hủy tự động do không check-in đúng giờ.
                
                Thông tin đặt bàn:
                - Mã đặt bàn: %s
                - Thời gian đặt: %s
                - Số lượng khách: %d người
                - Bàn: %s
                
                Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ nhà hàng.
                
                Trân trọng,
                Nhà hàng Tinh Hoa Hội Tụ
                """,
                invoice.getCustomer().getFullName(),
                invoice.getReservationCode(),
                invoice.getReservedAt().format(FORMATTER),
                invoice.getGuestCount(),
                tableNames);
        
        // Send email using JavaMailSender directly
        org.springframework.mail.SimpleMailMessage mail = new org.springframework.mail.SimpleMailMessage();
        mail.setTo(invoice.getCustomer().getEmail());
        mail.setSubject(subject);
        mail.setText(body);
        
        try {
            emailService.sendNoShowEmail(mail);
            log.debug("No-show email sent to customer: {}", invoice.getCustomer().getEmail());
        } catch (Exception e) {
            log.warn("Failed to send no-show email: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get table names from invoice
     */
    private String getTableNames(Invoice invoice) {
        List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository
                .findByInvoiceIdWithTable(invoice.getId());
        
        if (invoiceTables.isEmpty()) {
            return "N/A";
        }
        
        return invoiceTables.stream()
                .map(idt -> idt.getDiningTable().getTableName())
                .collect(Collectors.joining(", "));
    }
}
