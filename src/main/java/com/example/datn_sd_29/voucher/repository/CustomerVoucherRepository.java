package com.example.datn_sd_29.voucher.repository;

import com.example.datn_sd_29.voucher.entity.CustomerVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Integer> {
    List<CustomerVoucher> findByCustomerIdAndVoucherStatus(Integer customerId, String voucherStatus);

    List<CustomerVoucher> findByCustomerId(Integer customerId);

    Optional<CustomerVoucher> findByIdAndCustomerId(Integer id, Integer customerId);
}
