package com.example.datn_sd_29.service;

import com.example.datn_sd_29.dto.ProductComboRequest;
import com.example.datn_sd_29.dto.ProductComboResponse;
import com.example.datn_sd_29.entity.ProductCombo;
import com.example.datn_sd_29.repository.ProductComboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductComboService {

    private final ProductComboRepository productComboRepository;

    public List<ProductComboResponse> getAllProductCombos() {
        return productComboRepository.findAll()
                .stream()
                .map(ProductComboResponse::new)
                .toList();
    }

    public ProductComboResponse createProductCombo(ProductComboRequest request) {
        ProductCombo combo = new ProductCombo();
        combo.setComboName(request.getComboName());
        combo.setDescription(request.getDescription());
        combo.setComboPrice(request.getComboPrice());
        combo.setIsActive(true);
        combo.setCreatedAt(Instant.now());

        return new ProductComboResponse(
                productComboRepository.save(combo)
        );
    }

    public ProductComboResponse updateProductCombo(Integer id, ProductComboRequest request) {
        ProductCombo combo = productComboRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("ProductCombo not found with id: " + id)
                );
        if (combo.getIsActive()
                && Boolean.FALSE.equals(request.getIsActive())) {
            throw new IllegalArgumentException(
                    "Không được cập nhật trạng thái từ ACTIVE sang INACTIVE."
            );
        }

        combo.setComboName(request.getComboName());
        combo.setDescription(request.getDescription());
        combo.setComboPrice(request.getComboPrice());


        if (!combo.getIsActive()
                && Boolean.TRUE.equals(request.getIsActive())) {
            combo.setIsActive(true);
        }

        combo.setUpdatedAt(Instant.now());

        return new ProductComboResponse(
                productComboRepository.save(combo)
        );
    }

    // delete = true -> false
    public void deleteProductCombo(Integer id) {
        ProductCombo combo = productComboRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("ProductCombo not found with id: " + id)
                );

        combo.setIsActive(false);
        combo.setUpdatedAt(Instant.now());
        productComboRepository.save(combo);
    }
}
