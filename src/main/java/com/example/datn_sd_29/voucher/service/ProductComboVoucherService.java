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
        voucher.setVoucherCode(request.getVoucherCode());
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
