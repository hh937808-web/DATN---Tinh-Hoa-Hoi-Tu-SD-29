package com.example.datn_sd_29.reservation.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.reservation.dto.AvailableTableResponse;
import com.example.datn_sd_29.reservation.dto.ReservationRequest;
import com.example.datn_sd_29.reservation.dto.ReservationResponse;
import com.example.datn_sd_29.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<AvailableTableResponse>>> availableTables(
            @RequestParam("reservedAt")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime reservedAt,
            @RequestParam("guestCount") Integer guestCount
    ) {
        List<AvailableTableResponse> tables = reservationService.findAvailableTables(reservedAt, guestCount);
        return ResponseEntity.ok(ApiResponse.success("OK", tables));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> reserve(@Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.reserveTable(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt bàn thành công", response));
    }

    @PostMapping("/{reservationCode}/send-email")
    public ResponseEntity<ApiResponse<Void>> sendReservationEmail(
            @PathVariable String reservationCode
    ) {
        reservationService.sendReservationDetailsEmail(reservationCode);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi email thành công", null));
    }

}
