package com.example.datn_sd_29.product.service;

import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product.enums.ProductCategory;
import com.example.datn_sd_29.product.enums.ProductStatus;
import com.example.datn_sd_29.product.dto.ProductRequest;
import com.example.datn_sd_29.product.dto.ProductResponse;
import com.example.datn_sd_29.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
        product.setStockQuantity(request.getStockQuantity());
        Product saved = productRepository.save(product);

        return new ProductResponse(saved);
    }

    public ProductResponse updateProduct(Integer id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not found with id: " + id)
                );

        product.setProductName(request.getProductName());
        product.setProductCategory(request.getProductCategory());
        product.setUnitPrice(request.getUnitPrice());
        product.setDescription(request.getDescription());
        product.setAvailabilityStatus(request.getAvailabilityStatus());
        product.setStockQuantity(request.getStockQuantity());
        Product updated = productRepository.save(product);

        return new ProductResponse(updated);
    }

    public void deleteProduct(Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not found with id: " + id)
                );

        product.setAvailabilityStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);
    }


    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(
            String name,
            ProductCategory category,
            ProductStatus status
    ) {

        return productRepository.findAll().stream()
                .filter(p -> name == null || p.getProductName().toLowerCase().contains(name.toLowerCase()))
                .filter(p -> category == null || p.getProductCategory() == category)
                .filter(p -> status == null || p.getAvailabilityStatus() == status)
                .map(ProductResponse::new)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<ProductResponse> sortProducts(String field, String direction) {

        // map field từ FE sang DB
        String sortField = switch (field) {
            case "name" -> "productName";
            case "price" -> "unitPrice";
            default -> "id";
        };

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();

        return productRepository.findAll(sort)
                .stream()
                .map(ProductResponse::new)
                .toList();
    }
}