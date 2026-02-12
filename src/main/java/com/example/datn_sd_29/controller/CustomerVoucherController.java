package com.example.datn_sd_29.controller;

import com.example.datn_sd_29.dto.ApiResponse;
import com.example.datn_sd_29.dto.CustomerVoucherRequest;
import com.example.datn_sd_29.dto.CustomerVoucherResponse;
import com.example.datn_sd_29.service.CustomerVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-vouchers")
@RequiredArgsConstructor
public class CustomerVoucherController {

    private final CustomerVoucherService customerVoucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerVoucherResponse>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get customer voucher list successfully!",
                        customerVoucherService.getAll()
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerVoucherResponse>> create(
            @Valid @RequestBody CustomerVoucherRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Create customer voucher successfully!",
                        customerVoucherService.create(request)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerVoucherResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerVoucherRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Update customer voucher successfully!",
                        customerVoucherService.update(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        customerVoucherService.delete(id);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Customer voucher deactivated successfully!",
                        null
                )
        );
    }
}
