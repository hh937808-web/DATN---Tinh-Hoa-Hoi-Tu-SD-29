package com.example.datn_sd_29.voucher.service;

import com.example.datn_sd_29.voucher.dto.ProductVoucherRequest;
import com.example.datn_sd_29.voucher.dto.ProductVoucherResponse;
import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.voucher.entity.ProductVoucher;
import com.example.datn_sd_29.product.repository.ProductRepository;
import com.example.datn_sd_29.voucher.repository.ProductVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVoucherService {

    private final ProductVoucherRepository productVoucherRepository;
    private final ProductRepository productRepository;

    // =====================
    // GET ALL
    // =====================
    public List<ProductVoucherResponse> getAll() {
        return productVoucherRepository.findAll()
                .stream()
                .map(ProductVoucherResponse::new)
                .toList();
    }

    // =====================
    // CREATE
    // =====================
    public ProductVoucherResponse create(ProductVoucherRequest request) {

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy Product id = " + request.getProductId()
                ));

        ProductVoucher voucher = new ProductVoucher();
        voucher.setVoucherCode(request.getVoucherCode());
        voucher.setVoucherName(request.getVoucherName());
        voucher.setDiscountPercent(request.getDiscountPercent());
        voucher.setProduct(product);
        voucher.setRemainingQuantity(request.getRemainingQuantity());
        voucher.setValidFrom(request.getValidFrom());
        voucher.setValidTo(request.getValidTo());
        voucher.setIsActive(true);
        voucher.setCreatedAt(Instant.now());

        ProductVoucher saved = productVoucherRepository.save(voucher);

        return new ProductVoucherResponse(saved);
    }

    public ProductVoucherResponse update(Integer id, ProductVoucherRequest request) {

        ProductVoucher voucher = productVoucherRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("ProductVoucher not found with id: " + id)
                );

        Boolean newStatus = request.getIsActive();

        if (voucher.getIsActive() && Boolean.FALSE.equals(newStatus)) {
            throw new IllegalArgumentException(
                    "Không được cập nhật trạng thái từ ACTIVE sang INACTIVE."
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

        return new ProductVoucherResponse(
                productVoucherRepository.save(voucher)
        );
    }



    public void delete(Integer id) {

        ProductVoucher voucher = productVoucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy ProductVoucher id = " + id
                ));

        voucher.setIsActive(false);
        productVoucherRepository.save(voucher);
    }
}
