package com.example.datn_sd_29.product_combo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
public class ProductCombo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_combo_id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @Nationalized
    @Column(name = "combo_name", length = 100)
    private String comboName;

    @Size(max = 300)
    @Nationalized
    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "combo_price", precision = 18, scale = 2)
    private BigDecimal comboPrice;

    @Column(name = "is_active")
    private Boolean isActive;

    @ColumnDefault("getdate()")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

}