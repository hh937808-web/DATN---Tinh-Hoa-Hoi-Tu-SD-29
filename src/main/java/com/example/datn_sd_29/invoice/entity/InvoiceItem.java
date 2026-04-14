package com.example.datn_sd_29.invoice.entity;

import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_item_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Size(max = 20)
    @Column(name = "item_type", length = 20)
    private String itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_combo_id")
    private ProductCombo productCombo;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @ColumnDefault("[quantity]*[unit_price]")
    @Column(name = "line_total", insertable = false, updatable = false, precision = 29, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private InvoiceItemStatus status = InvoiceItemStatus.ORDERED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dining_table_id")
    private DiningTable diningTable;
    
    @Size(max = 100)
    @Column(name = "product_version_id", length = 100)
    private String productVersionId;
}
