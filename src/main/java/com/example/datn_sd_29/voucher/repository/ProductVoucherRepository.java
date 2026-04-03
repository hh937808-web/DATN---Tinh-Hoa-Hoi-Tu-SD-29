package com.example.datn_sd_29.voucher.repository;

import com.example.datn_sd_29.voucher.entity.ProductVoucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface ProductVoucherRepository extends JpaRepository<ProductVoucher, Integer> {
    List<ProductVoucher> findByIsActiveTrue();
    
    // FIX #6: Add pessimistic lock to prevent race condition when multiple users use same voucher
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductVoucher> findByVoucherCode(String voucherCode);
    
    // FIX #19: Check if voucher code already exists to prevent duplicates
    boolean existsByVoucherCode(String voucherCode);
}
