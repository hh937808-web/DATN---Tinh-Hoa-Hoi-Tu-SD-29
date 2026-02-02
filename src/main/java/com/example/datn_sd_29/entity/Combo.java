package com.example.datn_sd_29.entity;

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
public class Combo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_combo", nullable = false)
    private Integer id;

    @Size(max = 100)
    @Nationalized
    @Column(name = "ten_combo", length = 100)
    private String tenCombo;

    @Size(max = 300)
    @Nationalized
    @Column(name = "mo_ta", length = 300)
    private String moTa;

    @Column(name = "gia_combo", precision = 18, scale = 2)
    private BigDecimal giaCombo;

    @Size(max = 50)
    @Column(name = "trang_thai", length = 50)
    private String trangThai;

    @ColumnDefault("getdate()")
    @Column(name = "ngay_tao")
    private Instant ngayTao;

    @Column(name = "ngay_cap_nhat")
    private Instant ngayCapNhat;

}