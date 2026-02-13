package com.example.datn_sd_29.voucher.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.voucher.dto.ProductVoucherRequest;
import com.example.datn_sd_29.voucher.dto.ProductVoucherResponse;
import com.example.datn_sd_29.voucher.service.ProductVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-vouchers")
@RequiredArgsConstructor
public class ProductVoucherController {

    private final ProductVoucherService productVoucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductVoucherResponse>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy danh sách voucher thành công!",
                        productVoucherService.getAll()
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductVoucherResponse>> create(
            @Valid @RequestBody ProductVoucherRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tạo voucher thành công!",
                        productVoucherService.create(request)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductVoucherResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody ProductVoucherRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cập nhật voucher thành công!",
                        productVoucherService.update(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        productVoucherService.delete(id);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Ngừng hoạt động voucher thành công!",
                        null
                )
        );
    }
}
