package com.example.datn_sd_29.reservation.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.reservation.dto.ReservationRequest;
import com.example.datn_sd_29.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> reserve(@Valid @RequestBody ReservationRequest request) {
        reservationService.reserveTable(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt bàn thành công, vui lòng kiểm tra email", null));
    }

}
