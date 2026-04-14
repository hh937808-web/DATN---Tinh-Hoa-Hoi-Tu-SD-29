package com.example.datn_sd_29.product.repository;

import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product.enums.ProductCategory;
import com.example.datn_sd_29.product.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {


    List<Product> findByProductNameContainingIgnoreCase(String name);


    List<Product> findByProductCategory(ProductCategory category);


    List<Product> findByAvailabilityStatus(ProductStatus status);

    List<Product> findByProductCategoryAndAvailabilityStatus(
            ProductCategory category,
            ProductStatus status
    );
    
    List<Product> findByAvailabilityStatusNot(ProductStatus status);
}