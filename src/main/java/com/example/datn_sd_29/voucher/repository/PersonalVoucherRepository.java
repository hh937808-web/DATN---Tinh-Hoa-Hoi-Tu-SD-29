package com.example.datn_sd_29.voucher.repository;

import com.example.datn_sd_29.voucher.entity.PersonalVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalVoucherRepository extends JpaRepository<PersonalVoucher, Integer> {
    // FIX #19: Check if voucher code already exists to prevent duplicates
    boolean existsByVoucherCode(String voucherCode);
}
