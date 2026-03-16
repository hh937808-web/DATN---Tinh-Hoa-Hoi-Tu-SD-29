package com.example.datn_sd_29.voucher.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.voucher.dto.ProductVoucherRequest;
import com.example.datn_sd_29.voucher.dto.ProductVoucherResponse;
import com.example.datn_sd_29.voucher.entity.ProductVoucher;
import com.example.datn_sd_29.voucher.service.ProductVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
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

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductVoucherResponse>>> search(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer percent,
            @RequestParam(required = false) Boolean status
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Search voucher thành công",
                        productVoucherService.search(code, name, productName, percent, status)
                )
        );
    }


    @GetMapping("/sort/id")
    public ResponseEntity<ApiResponse<List<ProductVoucherResponse>>> sortById(
            @RequestParam(defaultValue = "asc") String direction
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Sort theo ID",
                        productVoucherService.sortById(direction)
                )
        );
    }


    @GetMapping("/sort/percent")
    public ResponseEntity<ApiResponse<List<ProductVoucherResponse>>> sortByPercent(
            @RequestParam(defaultValue = "desc") String direction
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Sort theo % giảm giá",
                        productVoucherService.sortByPercent(direction)
                )
        );
    }


    @GetMapping("/sort/quantity")
    public ResponseEntity<ApiResponse<List<ProductVoucherResponse>>> sortByQuantity(
            @RequestParam(defaultValue = "desc") String direction
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Sort theo số lượng",
                        productVoucherService.sortByQuantity(direction)
                )
        );
    }


    @GetMapping("/sort/created")
    public ResponseEntity<ApiResponse<List<ProductVoucherResponse>>> sortByCreated(
            @RequestParam(defaultValue = "desc") String direction
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Sort theo ngày tạo",
                        productVoucherService.sortByCreatedAt(direction)
                )
        );
    }


    @GetMapping("/sort/duration")
    public ResponseEntity<ApiResponse<List<ProductVoucherResponse>>> sortDuration(
            @RequestParam(defaultValue = "desc") String direction
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Sort theo thời gian hoạt động",
                        productVoucherService.sortDuration(direction)
                )
        );
    }
}