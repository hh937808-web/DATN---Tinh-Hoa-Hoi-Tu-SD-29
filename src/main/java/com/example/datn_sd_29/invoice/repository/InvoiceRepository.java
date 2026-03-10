package com.example.datn_sd_29.invoice.repository;

import com.example.datn_sd_29.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    @Query("""
        SELECT i FROM Invoice i
        JOIN InvoiceDiningTable idt ON i.id = idt.invoice.id
        WHERE idt.diningTable.id = :tableId
        AND i.invoiceStatus = 'IN_PROGRESS'
    """)
    Optional<Invoice> findActiveInvoiceByTableId(Integer tableId);

    Optional<Invoice> findByReservationCode(String reservationCode);
}
