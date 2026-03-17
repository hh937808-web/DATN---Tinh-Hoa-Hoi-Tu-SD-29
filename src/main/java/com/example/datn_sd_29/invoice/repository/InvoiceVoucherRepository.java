package com.example.datn_sd_29.invoice.repository;

import com.example.datn_sd_29.invoice.entity.InvoiceVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceVoucherRepository extends JpaRepository<InvoiceVoucher, Integer> {
}
