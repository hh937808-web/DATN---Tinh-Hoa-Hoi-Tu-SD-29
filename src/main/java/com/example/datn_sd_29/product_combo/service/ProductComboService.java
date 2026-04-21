package com.example.datn_sd_29.product_combo.service;

import com.example.datn_sd_29.product_combo.dto.ProductComboRequest;
import com.example.datn_sd_29.product_combo.dto.ProductComboResponse;
import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import com.example.datn_sd_29.product_combo.repository.ProductComboRepository;
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

    public List<ProductComboResponse> getActiveProductCombos() {
        return productComboRepository.findByIsActiveTrue()
                .stream()
                .map(ProductComboResponse::new)
                .toList();
    }

    public ProductComboResponse createProductCombo(ProductComboRequest request) {
        String code = request.getComboCode();
        if (code != null && !code.isBlank()) {
            if (productComboRepository.existsByComboCode(code)) {
                throw new IllegalArgumentException("Mã combo '" + code + "' đã tồn tại");
            }
        }

        ProductCombo combo = new ProductCombo();
        combo.setComboName(request.getComboName());
        combo.setDescription(request.getDescription());
        combo.setComboPrice(request.getComboPrice());
        combo.setIsActive(true);
        combo.setCreatedAt(Instant.now());
        // Save trước để lấy ID auto-generate
        ProductCombo saved = productComboRepository.save(combo);

        // Tự động tạo mã nếu không cung cấp
        if (code == null || code.isBlank()) {
            saved.setComboCode("COMBO-" + String.format("%05d", saved.getId()));
        } else {
            saved.setComboCode(code);
        }

        return new ProductComboResponse(productComboRepository.save(saved));
    }

    public ProductComboResponse updateProductCombo(Integer id, ProductComboRequest request) {
        ProductCombo combo = productComboRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("ProductCombo not found with id: " + id)
                );
        String code = request.getComboCode();
        if (code != null && !code.isBlank()) {
            if (productComboRepository.existsByComboCodeAndIdNot(code, id)) {
                throw new IllegalArgumentException("Mã combo '" + code + "' đã tồn tại");
            }
            combo.setComboCode(code);
        }

        combo.setComboName(request.getComboName());
        combo.setDescription(request.getDescription());
        combo.setComboPrice(request.getComboPrice());
        combo.setIsActive(request.getIsActive());

        combo.setUpdatedAt(Instant.now());

        return new ProductComboResponse(productComboRepository.save(combo));
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
