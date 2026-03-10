package com.example.datn_sd_29.invoice.repository;

import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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
          and inv.reservedAt between :windowStart and :windowEnd
    """)
    boolean existsOverlappingReservation(
            @Param("tableId") Integer tableId,
            @Param("activeStatuses") Collection<String> activeStatuses,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd
    );
}
