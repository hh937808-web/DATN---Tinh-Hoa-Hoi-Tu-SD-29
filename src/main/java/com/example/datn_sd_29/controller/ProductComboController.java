package com.example.datn_sd_29.controller;

import com.example.datn_sd_29.dto.ApiResponse;
import com.example.datn_sd_29.dto.ProductComboRequest;
import com.example.datn_sd_29.dto.ProductComboResponse;
import com.example.datn_sd_29.service.ProductComboService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-combos")
@RequiredArgsConstructor
public class ProductComboController {

    private final ProductComboService productComboService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductComboResponse>>> getAllProductCombos() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get product combo list successfully!",
                        productComboService.getAllProductCombos()
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductComboResponse>> createProductCombo(
            @Valid @RequestBody ProductComboRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Create product combo successfully!",
                        productComboService.createProductCombo(request)
                )
        );
    }


    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductComboResponse>> updateProductCombo(
            @PathVariable Integer id,
            @Valid @RequestBody ProductComboRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Update product combo successfully!",
                        productComboService.updateProductCombo(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProductCombo(@PathVariable Integer id) {
        productComboService.deleteProductCombo(id);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Product combo discontinued successfully!",
                        null
                )
        );
    }
}
