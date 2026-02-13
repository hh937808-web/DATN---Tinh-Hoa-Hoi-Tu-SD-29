package com.example.datn_sd_29.product.service;

import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product.enums.ProductStatus;
import com.example.datn_sd_29.product.dto.ProductRequest;
import com.example.datn_sd_29.product.dto.ProductResponse;
import com.example.datn_sd_29.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::new)
                .toList();
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        product.setProductName(request.getProductName());
        product.setProductCategory(request.getProductCategory());
        product.setUnitPrice(request.getUnitPrice());
        product.setDescription(request.getDescription());
        product.setAvailabilityStatus(request.getAvailabilityStatus());

        Product saved = productRepository.save(product);
        return new ProductResponse(saved);

    }

    public ProductResponse updateProduct(Integer id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found with id: " + id
                ));

        if (request.getAvailabilityStatus() == ProductStatus.DISCONTINUED) {
            throw new IllegalArgumentException(
                    "Không được cập nhật trạng thái DISCONTINUED khi update product"
            );
        }

        product.setProductName(request.getProductName());
        product.setProductCategory(request.getProductCategory());
        product.setUnitPrice(request.getUnitPrice());
        product.setDescription(request.getDescription());
        product.setAvailabilityStatus(request.getAvailabilityStatus());

        Product updated = productRepository.save(product);
        return new ProductResponse(updated);
    }

    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found with id: " + id
                ));

        product.setAvailabilityStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);
    }


}
