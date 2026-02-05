package com.example.datn_sd_29.controller;

import com.example.datn_sd_29.dto.ReservationRequest;
import com.example.datn_sd_29.service.ReservationService;
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
    public ResponseEntity<?> reserve(@RequestBody ReservationRequest request) {
        reservationService.reserveTable(request);
        return ResponseEntity.ok("Đặt bàn thành công, vui lòng kiểm tra email");
    }

}
