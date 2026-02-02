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
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hoa_don", nullable = false)
    private Integer id;

    @Size(max = 100)
    @Column(name = "ma_hoa_don", length = 100)
    private String maHoaDon;

    @Size(max = 100)
    @Column(name = "ma_dat_ban", length = 100)
    private String maDatBan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private com.example.datn_sd_29.entity.KhachHang idKhachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private com.example.datn_sd_29.entity.NhanVien idNhanVien;

    @Size(max = 20)
    @Column(name = "loai_hoa_don", length = 20)
    private String loaiHoaDon;

    @Size(max = 50)
    @Column(name = "trang_thai", length = 50)
    private String trangThai;

    @Column(name = "thoi_gian_dat")
    private Instant thoiGianDat;

    @Column(name = "thoi_gian_checkin")
    private Instant thoiGianCheckin;

    @Column(name = "tong_tien", precision = 18, scale = 2)
    private BigDecimal tongTien;

    @Column(name = "giam_gia", precision = 18, scale = 2)
    private BigDecimal giamGia;

    @ColumnDefault("isnull([tong_tien], 0)-isnull([giam_gia], 0)")
    @Column(name = "thanh_tien", precision = 19, scale = 2)
    private BigDecimal thanhTien;

    @Size(max = 20)
    @Column(name = "phuong_thuc_thanh_toan", length = 20)
    private String phuongThucThanhToan;

    @Column(name = "thoi_gian_thanh_toan")
    private Instant thoiGianThanhToan;

    @Column(name = "diem_tich_luy")
    private Integer diemTichLuy;

    @Column(name = "diem_su_dung")
    private Integer diemSuDung;

}