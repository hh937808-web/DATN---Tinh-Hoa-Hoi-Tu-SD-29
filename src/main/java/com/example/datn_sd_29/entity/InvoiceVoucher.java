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
public class InvoiceVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_voucher_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Size(max = 20)
    @Column(name = "voucher_scope", length = 20)
    private String voucherScope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_voucher_id")
    private CustomerVoucher customerVoucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_voucher_id")
    private com.example.datn_sd_29.entity.ProductVoucher productVoucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_combo_voucher_id")
    private com.example.datn_sd_29.entity.ProductComboVoucher productComboVoucher;

}