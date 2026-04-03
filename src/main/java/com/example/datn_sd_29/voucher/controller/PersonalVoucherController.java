package com.example.datn_sd_29.voucher.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.voucher.dto.PersonalVoucherResponse;
import com.example.datn_sd_29.voucher.service.PersonalVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personal-vouchers")
@RequiredArgsConstructor
public class PersonalVoucherController {

    private final PersonalVoucherService personalVoucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonalVoucherResponse>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get personal voucher list successfully!",
                        personalVoucherService.getAll()
                )
        );
    }
}
