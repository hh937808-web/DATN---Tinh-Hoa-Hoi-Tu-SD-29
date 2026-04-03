package com.example.datn_sd_29.product.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.product.dto.ProductRequest;
import com.example.datn_sd_29.product.dto.ProductResponse;
import com.example.datn_sd_29.product.enums.ProductCategory;
import com.example.datn_sd_29.product.enums.ProductStatus;
import com.example.datn_sd_29.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(
                ApiResponse.success("Get all products successfully!", products)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse response = productService.createProduct(request);

        return ResponseEntity.ok(
                ApiResponse.success("Create product successfully!", response)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody ProductRequest request
    ) {

        ProductResponse response = productService.updateProduct(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Update product successfully!", response)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Integer id
    ) {

        productService.deleteProduct(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Product discontinued successfully!",
                        null
                )
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) ProductStatus status
    ) {

        List<ProductResponse> products =
                productService.searchProducts(name, category, status);

        return ResponseEntity.ok(
                ApiResponse.success("Search products successfully!", products)
        );
    }


    @GetMapping("/sort")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> sortProducts(
            @RequestParam(defaultValue = "id") String field,
            @RequestParam(defaultValue = "asc") String direction
    ) {

        List<ProductResponse> products =
                productService.sortProducts(field, direction);

        return ResponseEntity.ok(
                ApiResponse.success("Sort products successfully!", products)
        );
    }
}