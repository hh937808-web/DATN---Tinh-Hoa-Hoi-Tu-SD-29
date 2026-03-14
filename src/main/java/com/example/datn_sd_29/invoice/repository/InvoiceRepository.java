package com.example.datn_sd_29.invoice.repository;

import com.example.datn_sd_29.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    Optional<Invoice> findByReservationCode(String reservationCode);
    @Query("""
        SELECT i FROM Invoice i
        LEFT JOIN i.customer c
        LEFT JOIN i.employee e
        WHERE
        (:invoiceCode IS NULL OR :invoiceCode = '' OR i.invoiceCode = :invoiceCode)
        AND (:reservationCode IS NULL OR :reservationCode = '' OR i.reservationCode = :reservationCode)
        AND (:customerName IS NULL OR :customerName = '' 
             OR LOWER(c.fullName) LIKE LOWER(CONCAT(:customerName, '%')))
        AND (:customerPhone IS NULL OR :customerPhone = '' 
             OR c.phoneNumber = :customerPhone)
        AND (:employeeName IS NULL OR :employeeName = '' 
             OR LOWER(e.fullName) LIKE LOWER(CONCAT(:employeeName, '%')))
        AND (:invoiceType IS NULL OR :invoiceType = '' 
             OR i.invoiceChannel = :invoiceType)
        AND (:status IS NULL OR :status = '' 
             OR i.invoiceStatus = :status)
        AND (:paymentMethod IS NULL OR :paymentMethod = '' 
             OR i.paymentMethod = :paymentMethod)
    """)
    List<Invoice> searchInvoice(
            String invoiceCode,
            String reservationCode,
            String customerName,
            String customerPhone,
            String employeeName,
            String invoiceType,
            String status,
            String paymentMethod
    );

}
