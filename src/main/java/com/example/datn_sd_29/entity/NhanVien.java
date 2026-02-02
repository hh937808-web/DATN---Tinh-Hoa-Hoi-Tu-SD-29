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

@Getter
@Setter
@Entity
public class NhanVien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nhan_vien", nullable = false)
    private Integer id;

    @Size(max = 150)
    @Nationalized
    @Column(name = "ho_ten", length = 150)
    private String hoTen;

    @Size(max = 100)
    @Column(name = "username", length = 100)
    private String username;

    @Size(max = 150)
    @Column(name = "password", length = 150)
    private String password;

    @Size(max = 50)
    @Nationalized
    @Column(name = "vai_tro", length = 50)
    private String vaiTro;

    @Size(max = 10)
    @Column(name = "so_dien_thoai", length = 10)
    private String soDienThoai;

    @Size(max = 200)
    @Column(name = "email", length = 200)
    private String email;

    @ColumnDefault("getdate()")
    @Column(name = "ngay_tao")
    private Instant ngayTao;

    @Column(name = "trang_thai")
    private Boolean trangThai;

}