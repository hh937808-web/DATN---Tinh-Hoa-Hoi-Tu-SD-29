package com.example.datn_sd_29.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
public class SanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_san_pham", nullable = false)
    private Integer id;

    @Size(max = 100)
    @Nationalized
    @Column(name = "ten_san_pham", length = 100)
    private String tenSanPham;

    @Size(max = 50)
    @Column(name = "loai_san_pham", length = 50)
    private String loaiSanPham;

    @Column(name = "gia_san_pham", precision = 18, scale = 2)
    private BigDecimal giaSanPham;

    @Size(max = 300)
    @Nationalized
    @Column(name = "mo_ta", length = 300)
    private String moTa;

    @Size(max = 50)
    @Column(name = "trang_thai", length = 50)
    private String trangThai;

}