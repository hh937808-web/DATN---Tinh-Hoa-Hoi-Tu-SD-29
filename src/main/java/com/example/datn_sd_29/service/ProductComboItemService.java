package com.example.datn_sd_29.service;

import com.example.datn_sd_29.dto.ProductComboItemRequest;
import com.example.datn_sd_29.dto.ProductComboItemResponse;
import com.example.datn_sd_29.entity.Product;
import com.example.datn_sd_29.entity.ProductCombo;
import com.example.datn_sd_29.entity.ProductComboItem;
import com.example.datn_sd_29.repository.ProductComboItemRepository;
import com.example.datn_sd_29.repository.ProductComboRepository;
import com.example.datn_sd_29.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductComboItemService {

    private final ProductComboItemRepository productComboItemRepository;
    private final ProductComboRepository productComboRepository;
    private final ProductRepository productRepository;


    public List<ProductComboItemResponse> getAll() {

        return productComboItemRepository.findAll()
                .stream()
                .map(ProductComboItemResponse::new)
                .toList();
    }


    public ProductComboItemResponse create(ProductComboItemRequest request) {

        ProductCombo combo = productComboRepository.findById(request.getProductComboId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy ProductCombo id = " + request.getProductComboId()
                ));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy Product id = " + request.getProductId()
                ));

        ProductComboItem item = new ProductComboItem();
        item.setProductCombo(combo);
        item.setProduct(product);
        item.setQuantity(request.getQuantity());

        ProductComboItem saved = productComboItemRepository.save(item);

        return new ProductComboItemResponse(saved);
    }
}
