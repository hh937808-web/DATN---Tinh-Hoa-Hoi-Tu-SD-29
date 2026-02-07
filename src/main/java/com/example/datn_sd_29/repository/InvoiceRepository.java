package com.example.datn_sd_29.repository;

import com.example.datn_sd_29.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    @Query(value = """
        SELECT 
            MONTH(i.paid_at) AS month,
            YEAR(i.paid_at) AS year,
            SUM(i.final_amount) AS totalRevenue 
            FROM Invoice i
        WHERE i.invoice_status = 'PAID'
        GROUP BY MONTH(i.paid_at), YEAR(i.paid_at)
        ORDER BY year DESC, month DESC
    """, nativeQuery = true)
    List<Object[]> getRevenueByMonth();

    @Query(value = """
    SELECT 
        YEAR(i.paid_at) AS year,
        SUM(i.final_amount) AS totalRevenue
    FROM Invoice i
    WHERE i.invoice_status = 'PAID'
    GROUP BY YEAR(i.paid_at)
    ORDER BY year DESC
    """, nativeQuery = true)
    List<Object[]> getRevenueByYear();

    @Query(value = """
    SELECT i.invoice_id, i.invoice_code, i.final_amount, i.paid_at
    FROM Invoice i
    WHERE i.invoice_status = 'PAID'
    ORDER BY i.paid_at DESC
    """, nativeQuery = true)
    List<Object[]> getRevenueByInvoice();

    @Query(value = """
    SELECT 
        DATEPART(HOUR, i.paid_at) AS hour,
        COUNT(DISTINCT i.invoice_id) AS totalCustomers
    FROM invoice i
    WHERE i.paid_at IS NOT NULL
    GROUP BY DATEPART(HOUR, i.paid_at)
    ORDER BY hour
""", nativeQuery = true)
    List<Object[]> getCustomerDensityByHour();

}
