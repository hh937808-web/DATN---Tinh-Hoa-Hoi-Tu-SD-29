package com.example.datn_sd_29.voucher.repository;

import com.example.datn_sd_29.voucher.entity.CustomerVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Integer> {
}
