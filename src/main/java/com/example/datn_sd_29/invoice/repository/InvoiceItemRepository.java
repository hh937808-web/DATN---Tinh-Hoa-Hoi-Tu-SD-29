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


    List<InvoiceItem> findByInvoiceId(Integer invoiceId);
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

    @Query("""
        select ii
        from InvoiceItem ii
        where ii.invoice.id = :invoiceId
          and ii.product.id = :productId
          and ii.status <> com.example.datn_sd_29.invoice.entity.InvoiceItemStatus.CANCELLED
    """)
    java.util.Optional<InvoiceItem> findActiveByInvoiceAndProduct(
            @Param("invoiceId") Integer invoiceId,
            @Param("productId") Integer productId);

    @Query("""
        select ii
        from InvoiceItem ii
        where ii.invoice.id = :invoiceId
          and ii.productCombo.id = :comboId
          and ii.status <> com.example.datn_sd_29.invoice.entity.InvoiceItemStatus.CANCELLED
    """)
    java.util.Optional<InvoiceItem> findActiveByInvoiceAndCombo(
            @Param("invoiceId") Integer invoiceId,
            @Param("comboId") Integer comboId);

    @Query("""
        select ii
        from InvoiceItem ii
        where ii.invoice.id = :invoiceId
          and ii.product.id = :productId
          and ii.status = com.example.datn_sd_29.invoice.entity.InvoiceItemStatus.SERVED
          and ii.id <> :excludeId
    """)
    java.util.Optional<InvoiceItem> findServedByInvoiceAndProductExcluding(
            @Param("invoiceId") Integer invoiceId,
            @Param("productId") Integer productId,
            @Param("excludeId") Integer excludeId);

    @Query("""
        select ii
        from InvoiceItem ii
        where ii.invoice.id = :invoiceId
          and ii.productCombo.id = :comboId
          and ii.status = com.example.datn_sd_29.invoice.entity.InvoiceItemStatus.SERVED
          and ii.id <> :excludeId
    """)
    java.util.Optional<InvoiceItem> findServedByInvoiceAndComboExcluding(
            @Param("invoiceId") Integer invoiceId,
            @Param("comboId") Integer comboId,
            @Param("excludeId") Integer excludeId);

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
