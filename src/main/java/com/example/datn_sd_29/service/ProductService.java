package com.example.datn_sd_29.service;

import com.example.datn_sd_29.dto.UpdateProductRequest;
import com.example.datn_sd_29.entity.Product;
import com.example.datn_sd_29.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    public Product updateProduct(Integer productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn với ID: " + productId));

        if (request.getAvailabilityStatus() != null) {
            product.setAvailabilityStatus(request.getAvailabilityStatus().name());
        }

        return productRepository.save(product);
    }
}
