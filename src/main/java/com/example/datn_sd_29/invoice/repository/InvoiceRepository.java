package com.example.datn_sd_29.invoice.repository;

import com.example.datn_sd_29.dashboard.dto.RecentInvoiceResponse;
import com.example.datn_sd_29.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    Optional<Invoice> findByReservationCode(String reservationCode);
    
    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    @Query("SELECT COALESCE(SUM(i.finalAmount), 0) FROM Invoice i " +
           "WHERE i.paidAt >= :startDate AND i.paidAt < :endDate AND i.invoiceStatus = 'PAID'")
    BigDecimal sumFinalAmountByDateRange(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(i) FROM Invoice i " +
           "WHERE i.paidAt >= :startDate AND i.paidAt < :endDate AND i.invoiceStatus = 'PAID'")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate, 
                         @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT " +
           "i.id, i.invoice_code, " +
           "(SELECT TOP 1 dt.table_name FROM InvoiceDiningTable idt " +
           " JOIN DiningTable dt ON idt.dining_table_id = dt.id " +
           " WHERE idt.invoice_id = i.id) as table_name, " +
           "i.paid_at, " +
           "i.subtotal_amount, i.discount_amount, i.service_fee_amount, i.tax_amount, " +
           "i.final_amount, i.invoice_status, i.payment_method " +
           "FROM Invoice i " +
           "WHERE i.invoice_status IN ('PAID', 'IN_PROGRESS') " +
           "ORDER BY COALESCE(i.paid_at, i.checked_in_at) DESC", 
           nativeQuery = true)
    List<Object[]> findRecentInvoicesRaw(org.springframework.data.domain.Pageable pageable);

    // Overtime detection query - Requirements 1.4, 7.2
    @Query("SELECT i FROM Invoice i WHERE i.invoiceStatus = :status AND i.checkedInAt < :threshold")
    List<Invoice> findByInvoiceStatusAndCheckedInAtBefore(@Param("status") String status, 
                                                           @Param("threshold") Instant threshold);

    // Next reservation query - Requirements 3.1, 3.2, 3.3
    @Query(value = "SELECT TOP 1 * FROM (" +
           "  SELECT i.* " +
           "  FROM Invoice i " +
           "  JOIN InvoiceDiningTable idt ON idt.invoice_id = i.invoice_id " +
           "  WHERE idt.dining_table_id = :tableId " +
           "  AND i.invoice_status = :status " +
           "  AND i.reserved_at BETWEEN :start AND :end " +
           ") ranked " +
           "ORDER BY ranked.reserved_at ASC", nativeQuery = true)
    List<Invoice> findFirstByDiningTableIdAndInvoiceStatusAndReservedAtBetweenOrderByReservedAtAsc(
            @Param("tableId") Integer tableId,
            @Param("status") String status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // No-show detection query
    @Query("SELECT i FROM Invoice i WHERE i.invoiceStatus = 'RESERVED' AND i.reservedAt < :cutoffTime")
    List<Invoice> findExpiredReservations(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Find reservations by customer phone number
    @Query("SELECT i FROM Invoice i " +
           "WHERE i.customer.phoneNumber = :phoneNumber " +
           "AND i.invoiceStatus = 'RESERVED' " +
           "ORDER BY i.reservedAt ASC")
    List<Invoice> findReservationsByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    // Find all RESERVED reservations
    @Query("SELECT i FROM Invoice i " +
           "WHERE i.invoiceStatus = 'RESERVED' " +
           "ORDER BY i.reservedAt ASC")
    List<Invoice> findAllReservedReservations();

    // Find all PENDING_CONFIRMATION reservations
    @Query("SELECT i FROM Invoice i " +
           "WHERE i.invoiceStatus = 'PENDING_CONFIRMATION' " +
           "ORDER BY i.reservedAt ASC")
    List<Invoice> findPendingConfirmationReservations();

    // Find invoices by status
    List<Invoice> findByInvoiceStatus(String invoiceStatus);

    // Find most recent invoice for a specific table (for priority scoring)
    @Query(value = "SELECT TOP 1 * FROM (" +
           "  SELECT i.* " +
           "  FROM Invoice i " +
           "  JOIN InvoiceDiningTable idt ON idt.invoice_id = i.invoice_id " +
           "  WHERE idt.dining_table_id = :tableId " +
           "  AND i.reserved_at IS NOT NULL " +
           ") ranked " +
           "ORDER BY ranked.reserved_at DESC", nativeQuery = true)
    List<Invoice> findMostRecentInvoicesByTableId(@Param("tableId") Integer tableId);

    // Check if table has recent reservations within time window
    @Query("SELECT COUNT(i) > 0 FROM Invoice i " +
           "JOIN InvoiceDiningTable idt ON idt.invoice.id = i.id " +
           "WHERE idt.diningTable.id = :tableId " +
           "AND i.invoiceStatus IN ('RESERVED', 'IN_PROGRESS') " +
           "AND i.reservedAt IS NOT NULL " +
           "AND i.reservedAt > :checkStart " +
           "AND i.reservedAt < :checkEnd")
    boolean hasRecentReservationInWindow(@Param("tableId") Integer tableId,
                                         @Param("checkStart") LocalDateTime checkStart,
                                         @Param("checkEnd") LocalDateTime checkEnd);

    // Find current active invoice for a table (IN_PROGRESS or most recent RESERVED)
    @Query(value = "SELECT TOP 1 * FROM (" +
           "  SELECT i.*, " +
           "    CASE i.invoice_status " +
           "      WHEN 'IN_PROGRESS' THEN 1 " +
           "      WHEN 'RESERVED' THEN 2 " +
           "      WHEN 'PAID' THEN 3 " +
           "      WHEN 'CANCELLED' THEN 4 " +
           "    END as status_priority " +
           "  FROM Invoice i " +
           "  JOIN InvoiceDiningTable idt ON idt.invoice_id = i.invoice_id " +
           "  WHERE idt.dining_table_id = :tableId " +
           "  AND i.invoice_status IN ('IN_PROGRESS', 'RESERVED', 'PAID', 'CANCELLED') " +
           ") ranked " +
           "ORDER BY ranked.status_priority, ranked.reserved_at DESC", nativeQuery = true)
    List<Invoice> findCurrentActiveInvoicesByTableId(@Param("tableId") Integer tableId);

    // Check if table has future reservation within time window (for walk-in conflict prevention)
    @Query("SELECT COUNT(i) > 0 FROM Invoice i " +
           "JOIN InvoiceDiningTable idt ON idt.invoice.id = i.id " +
           "WHERE idt.diningTable.id = :tableId " +
           "AND i.invoiceStatus = 'RESERVED' " +
           "AND i.reservedAt > :currentTime " +
           "AND i.reservedAt < :futureCheckEnd")
    boolean hasFutureReservationInWindow(@Param("tableId") Integer tableId,
                                         @Param("currentTime") LocalDateTime currentTime,
                                         @Param("futureCheckEnd") LocalDateTime futureCheckEnd);

    /**
     * Find RESERVED reservations for a specific table.
     * Used for validateReservationGap optimization.
     */
    @Query("SELECT i FROM Invoice i " +
           "JOIN InvoiceDiningTable idt ON idt.invoice.id = i.id " +
           "WHERE idt.diningTable.id = :tableId " +
           "AND i.invoiceStatus = 'RESERVED' " +
           "AND i.reservedAt IS NOT NULL " +
           "ORDER BY i.reservedAt ASC")
    List<Invoice> findReservedReservationsByTableId(@Param("tableId") Integer tableId);
}
