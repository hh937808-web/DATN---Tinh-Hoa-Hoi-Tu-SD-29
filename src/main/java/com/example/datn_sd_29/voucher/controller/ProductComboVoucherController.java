package com.example.datn_sd_29.voucher.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.voucher.dto.ProductComboVoucherRequest;
import com.example.datn_sd_29.voucher.dto.ProductComboVoucherResponse;
import com.example.datn_sd_29.voucher.service.ProductComboVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-combo-vouchers")
@RequiredArgsConstructor
public class ProductComboVoucherController {

    private final ProductComboVoucherService productComboVoucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductComboVoucherResponse>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get product combo voucher list successfully!",
                        productComboVoucherService.getAll()
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductComboVoucherResponse>> create(
            @Valid @RequestBody ProductComboVoucherRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Create product combo voucher successfully!",
                        productComboVoucherService.create(request)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductComboVoucherResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody ProductComboVoucherRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Update product combo voucher successfully!",
                        productComboVoucherService.update(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        productComboVoucherService.delete(id);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Product combo voucher discontinued successfully!",
                        null
                )
        );
    }
}
