package com.example.datn_sd_29.product_combo.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.product_combo.dto.ProductComboRequest;
import com.example.datn_sd_29.product_combo.dto.ProductComboResponse;
import com.example.datn_sd_29.product_combo.service.ProductComboService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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


    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductComboResponse>>> searchCombo(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) String productName
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Search product combo successfully!",
                        productComboService.searchCombo(name, price, status, productName)
                )
        );
    }


    @GetMapping("/sort/price/asc")
    public ResponseEntity<ApiResponse<List<ProductComboResponse>>> sortPriceAsc() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Sort price asc successfully!",
                        productComboService.sortByPriceAsc()
                )
        );
    }


    @GetMapping("/sort/price/desc")
    public ResponseEntity<ApiResponse<List<ProductComboResponse>>> sortPriceDesc() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Sort price desc successfully!",
                        productComboService.sortByPriceDesc()
                )
        );
    }


    @GetMapping("/sort/created/asc")
    public ResponseEntity<ApiResponse<List<ProductComboResponse>>> sortCreatedAsc() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Sort created date asc successfully!",
                        productComboService.sortByCreatedAsc()
                )
        );
    }


    @GetMapping("/sort/created/desc")
    public ResponseEntity<ApiResponse<List<ProductComboResponse>>> sortCreatedDesc() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Sort created date desc successfully!",
                        productComboService.sortByCreatedDesc()
                )
        );
    }

}