package com.example.datn_sd_29.product.service;

import com.example.datn_sd_29.media.entity.Image;
import com.example.datn_sd_29.media.repository.ImageRepository;
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
    private final ImageRepository imageRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toProductResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByAvailabilityStatusNot(ProductStatus.DISCONTINUED)
                .stream()
                .map(this::toProductResponse)
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

        return toProductResponse(saved);
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
        Product updated = productRepository.save(product);

        return toProductResponse(updated);
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
                .map(this::toProductResponse)
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
                .map(this::toProductResponse)
                .toList();
    }
    
    private ProductResponse toProductResponse(Product product) {
        ProductResponse response = new ProductResponse(product);
        
        // Load primary image if exists
        imageRepository.findByProduct_IdAndIsPrimaryTrue(product.getId())
                .ifPresent(image -> response.setImageUrl(image.getImageUrl()));
        
        return response;
    }
}