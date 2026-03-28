package com.example.datn_sd_29.invoice.repository;

import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceDiningTableRepository extends JpaRepository<InvoiceDiningTable, Integer> {

    @Query("""
        select idt
        from InvoiceDiningTable idt
        join fetch idt.diningTable dt
        where idt.invoice.id = :invoiceId
    """)
    List<InvoiceDiningTable> findByInvoiceIdWithTable(@Param("invoiceId") Integer invoiceId);

    @Query("""
        select count(idt) > 0
        from InvoiceDiningTable idt
        join idt.invoice inv
        where idt.diningTable.id = :tableId
          and inv.invoiceStatus in :activeStatuses
          and inv.reservedAt < :windowEnd
          and timestampadd(minute, :durationMinutes, inv.reservedAt) > :windowStart
    """)
    boolean existsOverlappingReservation(
            @Param("tableId") Integer tableId,
            @Param("activeStatuses") Collection<String> activeStatuses,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd,
            @Param("durationMinutes") int durationMinutes
    );

    @Query("""
        select idt.invoice
        from InvoiceDiningTable idt
        join idt.invoice inv
        where idt.diningTable.id = :tableId
          and inv.invoiceStatus = :status
        order by inv.checkedInAt desc nulls last, inv.reservedAt desc nulls last
        limit 1
    """)
    Optional<Invoice> findInvoiceByTableAndStatus(
            @Param("tableId") Integer tableId,
            @Param("status") String status
    );

    /**
     * Find all distinct invoices for a table with given statuses.
     * Used for safe invoice lookup - returns all matching invoices to detect conflicts.
     * Does NOT use LIMIT 1 to avoid silently picking wrong invoice for merged tables.
     */
    @Query("""
        select distinct idt.invoice
        from InvoiceDiningTable idt
        join idt.invoice inv
        where idt.diningTable.id = :tableId
          and inv.invoiceStatus in :statuses
    """)
    List<Invoice> findDistinctInvoicesByTableAndStatuses(
            @Param("tableId") Integer tableId,
            @Param("statuses") Collection<String> statuses
    );

    /**
     * Find invoices that conflict with a table within a time window.
     * Used for check-in conflict detection with proper overlap checking.
     * 
     * Overlap logic: Two time ranges [A_start, A_end] and [B_start, B_end] overlap if:
     * A_start < B_end AND A_end > B_start
     * 
     * For our case:
     * - Existing invoice range: [existingStart, existingEnd]
     * - New window range: [windowStart, windowEnd]
     * - They overlap if: existingStart < windowEnd AND existingEnd > windowStart
     */
    @Query("""
        select distinct idt.invoice
        from InvoiceDiningTable idt
        join idt.invoice inv
        where idt.diningTable.id = :tableId
          and inv.invoiceStatus in :statuses
          and (
            (
              inv.checkedInAt is not null
              and inv.checkedInAt < :windowEnd
              and timestampadd(minute, :durationMinutes, inv.checkedInAt) > :windowStart
            )
            or (
              inv.reservedAt is not null
              and inv.reservedAt < :windowEnd
              and timestampadd(minute, :durationMinutes, inv.reservedAt) > :windowStart
            )
          )
    """)
    List<Invoice> findConflictingInvoicesByTableWithinWindow(
            @Param("tableId") Integer tableId,
            @Param("statuses") Collection<String> statuses,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd,
            @Param("durationMinutes") int durationMinutes
    );
}
