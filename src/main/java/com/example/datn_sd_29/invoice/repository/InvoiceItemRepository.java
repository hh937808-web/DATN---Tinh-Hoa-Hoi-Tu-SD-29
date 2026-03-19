package com.example.datn_sd_29.invoice.repository;

import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
