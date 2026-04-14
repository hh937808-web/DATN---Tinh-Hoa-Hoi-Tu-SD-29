package com.example.datn_sd_29.product_version.service;

import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import com.example.datn_sd_29.product_combo.entity.ProductComboItem;
import com.example.datn_sd_29.product_combo.repository.ProductComboItemRepository;
import com.example.datn_sd_29.product_version.document.ProductVersionDocument;
import com.example.datn_sd_29.product_version.repository.ProductVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductVersionService {
    
    private final ProductVersionRepository productVersionRepository;
    private final ProductComboItemRepository productComboItemRepository;
    
    public String createProductSnapshot(Product product) {
        ProductVersionDocument version = ProductVersionDocument.builder()
                .productId(product.getId())
                .itemType("PRODUCT")
                .itemName(product.getProductName())
                .unitPrice(product.getUnitPrice())
                .description(product.getDescription())
                .category(product.getProductCategory() != null ? product.getProductCategory().name() : null)
                .createdAt(Instant.now())
                .build();
        
        ProductVersionDocument saved = productVersionRepository.save(version);
        return saved.getId();
    }
    
    public String createComboSnapshot(ProductCombo combo) {
        List<ProductComboItem> comboItems = productComboItemRepository.findByProductComboId(combo.getId());
        
        List<ProductVersionDocument.ComboItemSnapshot> itemSnapshots = comboItems.stream()
                .map(item -> ProductVersionDocument.ComboItemSnapshot.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getProductName())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());
        
        ProductVersionDocument version = ProductVersionDocument.builder()
                .productComboId(combo.getId())
                .itemType("COMBO")
                .itemName(combo.getComboName())
                .unitPrice(combo.getComboPrice())
                .description(combo.getDescription())
                .comboItems(itemSnapshots)
                .createdAt(Instant.now())
                .build();
        
        ProductVersionDocument saved = productVersionRepository.save(version);
        return saved.getId();
    }
    
    public ProductVersionDocument getVersionById(String versionId) {
        return productVersionRepository.findById(versionId).orElse(null);
    }
}
