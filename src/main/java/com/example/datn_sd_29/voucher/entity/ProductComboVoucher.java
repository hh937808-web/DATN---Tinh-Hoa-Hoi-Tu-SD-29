package com.example.datn_sd_29.voucher.entity;

import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
public class ProductComboVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_combo_voucher_id", nullable = false)
    private Integer id;

    @Size(max = 8)
    @Column(name = "voucher_code", length = 8)
    private String voucherCode;

    @Size(max = 50)
    @Column(name = "voucher_name", length = 50)
    private String voucherName;

    @Column(name = "discount_percent")
    private Integer discountPercent;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_combo_id", nullable = false)
    private ProductCombo productCombo;

    @Column(name = "remaining_quantity")
    private Integer remainingQuantity;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "is_active")
    private Boolean isActive;

    @ColumnDefault("getdate()")
    @Column(name = "created_at")
    private Instant createdAt;

}