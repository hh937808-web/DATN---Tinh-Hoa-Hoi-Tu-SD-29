package com.example.datn_sd_29.walkin.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.walkin.dto.WalkInCheckInRequest;
import com.example.datn_sd_29.walkin.dto.WalkInCheckInResponse;
import com.example.datn_sd_29.walkin.service.WalkInService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/walk-in")
@RequiredArgsConstructor
public class WalkInController {
    
    private final WalkInService walkInService;

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<WalkInCheckInResponse>> checkIn(
            @RequestBody WalkInCheckInRequest request) {
        WalkInCheckInResponse response = walkInService.checkInWalkIn(request);
        return ResponseEntity.ok(ApiResponse.success("Check-in thành công", response));
    }

    @PostMapping("/cancel/{invoiceCode}")
    public ResponseEntity<ApiResponse<Void>> cancelTable(@PathVariable String invoiceCode) {
        walkInService.cancelTable(invoiceCode);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy bàn thành công", null));
    }
}
