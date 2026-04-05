package com.example.datn_sd_29.reservation.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.reservation.dto.AvailableTableResponse;
import com.example.datn_sd_29.reservation.dto.ConfirmReservationRequest;
import com.example.datn_sd_29.reservation.dto.ReservationRequest;
import com.example.datn_sd_29.reservation.dto.ReservationResponse;
import com.example.datn_sd_29.reservation.dto.ReservationListResponse;
import com.example.datn_sd_29.reservation.dto.TableReassignmentRequest;
import com.example.datn_sd_29.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping("/{reservationCode}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservationByCode(
            @PathVariable String reservationCode
    ) {
        ReservationResponse response = reservationService.getReservationByCode(reservationCode);
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }

    @PostMapping("/{reservationCode}/check-in")
    public ResponseEntity<ApiResponse<ReservationResponse>> checkInReservation(
            @PathVariable String reservationCode
    ) {
        ReservationResponse response = reservationService.checkInReservation(reservationCode);
        return ResponseEntity.ok(ApiResponse.success("Check-in thành công", response));
    }

    @PostMapping("/{reservationCode}/send-email")
    public ResponseEntity<ApiResponse<Void>> sendReservationEmail(
            @PathVariable String reservationCode
    ) {
        reservationService.sendReservationDetailsEmail(reservationCode);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi email thành công", null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ReservationListResponse>>> searchByPhoneNumber(
            @RequestParam("phoneNumber") String phoneNumber
    ) {
        List<ReservationListResponse> reservations = reservationService.findReservationsByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("OK", reservations));
    }

    @PostMapping("/{invoiceId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable Integer invoiceId
    ) {
        reservationService.cancelReservation(invoiceId);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy đặt bàn thành công", null));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ReservationListResponse>>> getAllReservedReservations() {
        List<ReservationListResponse> reservations = reservationService.findAllReservedReservations();
        return ResponseEntity.ok(ApiResponse.success("OK", reservations));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ReservationListResponse>>> getPendingConfirmationReservations() {
        List<ReservationListResponse> reservations = reservationService.findPendingConfirmationReservations();
        return ResponseEntity.ok(ApiResponse.success("OK", reservations));
    }

    @PostMapping("/{reservationCode}/confirm")
    public ResponseEntity<ApiResponse<ReservationResponse>> confirmReservation(
            @PathVariable String reservationCode
    ) {
        ReservationResponse response = reservationService.confirmReservation(reservationCode);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận đặt bàn thành công", response));
    }

    @GetMapping("/{reservationCode}/alternative-tables")
    public ResponseEntity<ApiResponse<List<ReservationResponse.TableInfo>>> getAlternativeTables(
            @PathVariable String reservationCode
    ) {
        List<DiningTable> availableTables = reservationService.getAvailableTablesForReassignment(reservationCode);
        
        // Convert to TableInfo DTOs
        List<ReservationResponse.TableInfo> result = availableTables.stream()
                .map(table -> new ReservationResponse.TableInfo(
                        table.getId(),
                        "MB-" + table.getId(),
                        table.getTableName(),
                        table.getSeatingCapacity(),
                        table.getArea(),
                        table.getFloor()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("OK", result));
    }

    @GetMapping("/{reservationCode}/recommended-tables")
    public ResponseEntity<ApiResponse<List<ReservationResponse.TableInfo>>> getRecommendedTables(
            @PathVariable String reservationCode,
            @RequestParam(required = false) String area
    ) {
        List<DiningTable> recommendedTables = reservationService.getRecommendedTablesForReassignment(reservationCode, area);
        
        // Convert to TableInfo DTOs
        List<ReservationResponse.TableInfo> result = recommendedTables.stream()
                .map(table -> new ReservationResponse.TableInfo(
                        table.getId(),
                        "MB-" + table.getId(),
                        table.getTableName(),
                        table.getSeatingCapacity(),
                        table.getArea(),
                        table.getFloor()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("OK", result));
    }

    @PostMapping("/{reservationCode}/reassign-tables")
    public ResponseEntity<ApiResponse<ReservationResponse>> reassignTables(
            @PathVariable String reservationCode,
            @Valid @RequestBody TableReassignmentRequest request
    ) {
        ReservationResponse response = reservationService.reassignReservationTables(
                reservationCode, 
                request.getTableIds()
        );
        return ResponseEntity.ok(ApiResponse.success("Đã chuyển bàn thành công", response));
    }

}
