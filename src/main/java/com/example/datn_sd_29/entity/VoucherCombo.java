package com.example.datn_sd_29.entity;

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
public class VoucherCombo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_voucher_combo", nullable = false)
    private Integer id;

    @Size(max = 8)
    @Column(name = "ma_voucher", length = 8)
    private String maVoucher;

    @Size(max = 50)
    @Column(name = "ten_voucher", length = 50)
    private String tenVoucher;

    @Column(name = "gia_tri_giam")
    private Integer giaTriGiam;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_combo", nullable = false)
    private Combo idCombo;

    @Column(name = "so_luong_voucher")
    private Integer soLuongVoucher;

    @Column(name = "thoi_gian_bat_dau")
    private LocalDate thoiGianBatDau;

    @Column(name = "thoi_gian_ket_thuc")
    private LocalDate thoiGianKetThuc;

    @Column(name = "trang_thai")
    private Boolean trangThai;

    @ColumnDefault("getdate()")
    @Column(name = "ngay_tao")
    private Instant ngayTao;

}