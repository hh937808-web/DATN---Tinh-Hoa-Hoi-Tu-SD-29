package com.example.datn_sd_29.product_combo.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.product_combo.dto.ProductComboItemRequest;
import com.example.datn_sd_29.product_combo.dto.ProductComboItemResponse;
import com.example.datn_sd_29.product_combo.service.ProductComboItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-combo-items")
@RequiredArgsConstructor
public class ProductComboItemController {

    private final ProductComboItemService productComboItemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductComboItemResponse>>> getAll() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy danh sách ProductComboItem thành công",
                        productComboItemService.getAll()
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductComboItemResponse>> create(
            @Valid @RequestBody ProductComboItemRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Thêm sản phẩm vào combo thành công",
                        productComboItemService.create(request)
                )
        );
    }
}
