package com.example.datn_sd_29.invoice.entity;

import com.example.datn_sd_29.voucher.entity.CustomerVoucher;
import com.example.datn_sd_29.voucher.entity.ProductComboVoucher;
import com.example.datn_sd_29.voucher.entity.ProductVoucher;
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
    private ProductVoucher productVoucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_combo_voucher_id")
    private ProductComboVoucher productComboVoucher;

}