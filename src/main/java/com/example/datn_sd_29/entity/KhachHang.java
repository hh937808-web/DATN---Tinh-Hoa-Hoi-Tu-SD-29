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

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
public class KhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_khach_hang", nullable = false)
    private Integer id;

    @Size(max = 150)
    @Nationalized
    @Column(name = "ho_ten", length = 150)
    private String hoTen;

    @Size(max = 10)
    @Column(name = "so_dien_thoai", length = 10)
    private String soDienThoai;

    @Size(max = 200)
    @Column(name = "email", length = 200)
    private String email;

    @Size(max = 150)
    @Column(name = "password", length = 150)
    private String password;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "diem_tich_luy")
    private Integer diemTichLuy;

    @ColumnDefault("getdate()")
    @Column(name = "ngay_tao")
    private Instant ngayTao;

    @Column(name = "trang_thai")
    private Boolean trangThai;

}