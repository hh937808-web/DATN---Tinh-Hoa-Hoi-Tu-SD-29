package com.example.datn_sd_29.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class HoaDonVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hoa_don_voucher", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoa_don")
    private HoaDon idHoaDon;

    @Size(max = 20)
    @Column(name = "loai_voucher", length = 20)
    private String loaiVoucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_voucher_khach_hang")
    private com.example.datn_sd_29.entity.VoucherKhachHang idVoucherKhachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_voucher_san_pham")
    private com.example.datn_sd_29.entity.VoucherSanPham idVoucherSanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_voucher_combo")
    private com.example.datn_sd_29.entity.VoucherCombo idVoucherCombo;

}