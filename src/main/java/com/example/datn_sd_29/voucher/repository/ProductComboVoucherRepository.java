package com.example.datn_sd_29.voucher.repository;

import com.example.datn_sd_29.voucher.entity.ProductComboVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductComboVoucherRepository extends JpaRepository<ProductComboVoucher, Integer> {
    List<ProductComboVoucher> findByIsActiveTrue();
    Optional<ProductComboVoucher> findByVoucherCode(String voucherCode);
}
