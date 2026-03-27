package com.example.datn_sd_29.invoice.repository;

import com.example.datn_sd_29.dashboard.dto.TopProductResponse;
import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.entity.InvoiceItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Integer> {
    @Query("""
        select ii
        from InvoiceItem ii
        left join fetch ii.product p
        left join fetch ii.productCombo pc
        where ii.invoice.id = :invoiceId
    """)
    List<InvoiceItem> findByInvoiceIdWithItem(@Param("invoiceId") Integer invoiceId);

    @Query(value = "SELECT " +
           "p.id, p.product_name, p.product_category, " +
           "SUM(ii.quantity) as total_quantity, " +
           "SUM(ii.quantity * ii.unit_price) as total_revenue " +
           "FROM InvoiceItem ii " +
           "JOIN Product p ON ii.product_id = p.id " +
           "JOIN Invoice i ON ii.invoice_id = i.id " +
           "WHERE i.paid_at >= :startDate AND i.paid_at < :endDate AND i.invoice_status = 'PAID' " +
           "GROUP BY p.id, p.product_name, p.product_category " +
           "ORDER BY total_revenue DESC", 
           nativeQuery = true)
    List<Object[]> findTopProductsByDateRangeRaw(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 org.springframework.data.domain.Pageable pageable);

    // ===== KITCHEN =====
    @Query("""
        select ii
        from InvoiceItem ii
        left join fetch ii.product p
        left join fetch ii.productCombo pc
        left join fetch ii.diningTable dt
        where ii.status in :statuses
    """)
    List<InvoiceItem> findKitchenItems(@Param("statuses") List<InvoiceItemStatus> statuses);
}
