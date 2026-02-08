package com.example.datn_sd_29.controller;

import com.example.datn_sd_29.dto.ApiResponse;
import com.example.datn_sd_29.dto.UpdateProductRequest;
import com.example.datn_sd_29.entity.Product;
import com.example.datn_sd_29.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable Integer id,
            @RequestBody UpdateProductRequest request
    ) {
        Product product = productService.updateProduct(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật trạng thái món ăn thành công!", product)
        );
    }
}
