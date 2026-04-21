package com.example.datn_sd_29.media.entity;

import com.example.datn_sd_29.blog.entity.BlogPost;
import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_combo_id")
    private ProductCombo productCombo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_post_id")
    private BlogPost blogPost;

    @Lob
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_primary")
    private Boolean isPrimary;

}