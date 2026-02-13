package com.example.datn_sd_29.product.entity;

import com.example.datn_sd_29.product.enums.ProductCategory;
import com.example.datn_sd_29.product.enums.ProductStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @Nationalized
    @Column(name = "product_name", length = 100)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_category", length = 50)
    private ProductCategory productCategory;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Size(max = 300)
    @Nationalized
    @Column(name = "description", length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", length = 50)
    private ProductStatus availabilityStatus;

}