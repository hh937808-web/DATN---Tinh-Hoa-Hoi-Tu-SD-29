package com.example.datn_sd_29.voucher.service;

import com.example.datn_sd_29.voucher.dto.ProductComboVoucherRequest;
import com.example.datn_sd_29.voucher.dto.ProductComboVoucherResponse;
import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import com.example.datn_sd_29.voucher.entity.ProductComboVoucher;
import com.example.datn_sd_29.product_combo.repository.ProductComboRepository;
import com.example.datn_sd_29.voucher.repository.ProductComboVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductComboVoucherService {

    private final ProductComboVoucherRepository productComboVoucherRepository;
    private final ProductComboRepository productComboRepository;

    public List<ProductComboVoucherResponse> getAll() {
        return productComboVoucherRepository.findAll()
                .stream()
                .map(ProductComboVoucherResponse::new)
                .toList();
    }

    public ProductComboVoucherResponse create(ProductComboVoucherRequest request) {

        ProductCombo combo = productComboRepository.findById(request.getProductComboId())
                .orElseThrow(() ->
                        new IllegalArgumentException("ProductCombo not found with id: " + request.getProductComboId())
                );

        ProductComboVoucher voucher = new ProductComboVoucher();
        
        // Voucher code generation logic:
        // 1. If admin provides code -> use it
        // 2. If code is "AUTO" -> generate based on combo ID (C{comboId})
        // 3. If code is "RANDOM" -> generate random code
        // 4. If code is empty/null -> generate random code (default)
        String voucherCode = request.getVoucherCode();
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            voucherCode = generateRandomVoucherCode();
        } else if ("AUTO".equalsIgnoreCase(voucherCode.trim())) {
            voucherCode = generateComboVoucherCode(request.getProductComboId());
        } else if ("RANDOM".equalsIgnoreCase(voucherCode.trim())) {
            voucherCode = generateRandomVoucherCode();
        }
        // else: use admin's custom code as-is
        
        voucher.setVoucherCode(voucherCode);
        voucher.setVoucherName(request.getVoucherName());
        voucher.setDiscountPercent(request.getDiscountPercent());
        voucher.setProductCombo(combo);
        voucher.setRemainingQuantity(request.getRemainingQuantity());
        voucher.setValidFrom(request.getValidFrom());
        voucher.setValidTo(request.getValidTo());
        voucher.setIsActive(true);
        voucher.setCreatedAt(Instant.now());

        ProductComboVoucher saved = productComboVoucherRepository.save(voucher);

        return new ProductComboVoucherResponse(saved);
    }
    
    /**
     * Generate voucher code based on combo ID
     * Format: C{comboId}
     * Example: Combo ID 5 -> C5
     */
    private String generateComboVoucherCode(Integer comboId) {
        return "C" + comboId;
    }
    
    /**
     * Generate random voucher code
     * Format: 8 uppercase alphanumeric characters
     * Example: VCH8X2K9, A7B3C9D2
     */
    private String generateRandomVoucherCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder(8);
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    public ProductComboVoucherResponse update(Integer id, ProductComboVoucherRequest request) {

        ProductComboVoucher voucher = productComboVoucherRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("ProductComboVoucher not found with id: " + id)
                );

        Boolean newStatus = request.getIsActive();

        if (voucher.getIsActive() && Boolean.FALSE.equals(newStatus)) {
            throw new IllegalArgumentException(
                    "Cannot update status from ACTIVE to INACTIVE."
            );
        }

        voucher.setVoucherCode(request.getVoucherCode());
        voucher.setVoucherName(request.getVoucherName());
        voucher.setDiscountPercent(request.getDiscountPercent());
        voucher.setRemainingQuantity(request.getRemainingQuantity());
        voucher.setValidFrom(request.getValidFrom());
        voucher.setValidTo(request.getValidTo());

        if (!voucher.getIsActive() && Boolean.TRUE.equals(newStatus)) {
            voucher.setIsActive(true);
        }

        return new ProductComboVoucherResponse(
                productComboVoucherRepository.save(voucher)
        );
    }

    public void delete(Integer id) {

        ProductComboVoucher voucher = productComboVoucherRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("ProductComboVoucher not found with id: " + id)
                );

        voucher.setIsActive(false);
        productComboVoucherRepository.save(voucher);
    }
}
