package com.example.datn_sd_29.voucher.repository;

import com.example.datn_sd_29.voucher.entity.ProductVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVoucherRepository extends JpaRepository<ProductVoucher, Integer> {
    List<ProductVoucher> findByIsActiveTrue();
    Optional<ProductVoucher> findByVoucherCode(String voucherCode);
}
