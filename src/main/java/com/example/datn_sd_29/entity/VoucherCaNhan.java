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

@Getter
@Setter
@Entity
public class VoucherCaNhan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_voucher_ca_nhan", nullable = false)
    private Integer id;

    @Size(max = 8)
    @Column(name = "ma_voucher", length = 8)
    private String maVoucher;

    @Size(max = 50)
    @Column(name = "ten_voucher", length = 50)
    private String tenVoucher;

    @Column(name = "gia_tri_giam")
    private Integer giaTriGiam;

    @Size(max = 50)
    @Nationalized
    @Column(name = "loai_voucher", length = 50)
    private String loaiVoucher;

}