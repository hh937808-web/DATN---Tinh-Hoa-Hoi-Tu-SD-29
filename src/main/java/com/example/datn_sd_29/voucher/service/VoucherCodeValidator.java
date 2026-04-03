package com.example.datn_sd_29.voucher.service;

import com.example.datn_sd_29.voucher.repository.PersonalVoucherRepository;
import com.example.datn_sd_29.voucher.repository.ProductComboVoucherRepository;
import com.example.datn_sd_29.voucher.repository.ProductVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * FIX #19: Centralized voucher code validation service
 * Checks for duplicate voucher codes across ALL voucher types
 */
@Service
@RequiredArgsConstructor
public class VoucherCodeValidator {

    private final ProductVoucherRepository productVoucherRepository;
    private final ProductComboVoucherRepository productComboVoucherRepository;
    private final PersonalVoucherRepository personalVoucherRepository;

    /**
     * Check if voucher code already exists in ANY voucher table
     * @param voucherCode the code to check
     * @throws IllegalArgumentException if code already exists
     */
    public void validateUniqueVoucherCode(String voucherCode) {
        if (productVoucherRepository.existsByVoucherCode(voucherCode)) {
            throw new IllegalArgumentException("Mã voucher '" + voucherCode + "' đã tồn tại trong Product Voucher");
        }
        
        if (productComboVoucherRepository.existsByVoucherCode(voucherCode)) {
            throw new IllegalArgumentException("Mã voucher '" + voucherCode + "' đã tồn tại trong Product Combo Voucher");
        }
        
        if (personalVoucherRepository.existsByVoucherCode(voucherCode)) {
            throw new IllegalArgumentException("Mã voucher '" + voucherCode + "' đã tồn tại trong Personal Voucher");
        }
    }
    
    /**
     * Check if voucher code exists (returns boolean instead of throwing)
     * @param voucherCode the code to check
     * @return true if code exists in any table, false otherwise
     */
    public boolean isVoucherCodeExists(String voucherCode) {
        return productVoucherRepository.existsByVoucherCode(voucherCode) ||
               productComboVoucherRepository.existsByVoucherCode(voucherCode) ||
               personalVoucherRepository.existsByVoucherCode(voucherCode);
    }
}
