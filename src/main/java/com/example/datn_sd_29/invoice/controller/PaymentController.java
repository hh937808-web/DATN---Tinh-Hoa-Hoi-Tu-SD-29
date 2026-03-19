package com.example.datn_sd_29.invoice.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.invoice.dto.PaymentCheckoutRequest;
import com.example.datn_sd_29.invoice.dto.PaymentCheckoutResponse;
import com.example.datn_sd_29.invoice.dto.PaymentCancelRequest;
import com.example.datn_sd_29.invoice.dto.PaymentDetailResponse;
import com.example.datn_sd_29.invoice.dto.PaymentUpdateItemRequest;
import com.example.datn_sd_29.invoice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reception/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/by-table/{tableId}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getByTable(
            @PathVariable Integer tableId
    ) {
        PaymentDetailResponse response = paymentService.getPaymentByTable(tableId);
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<PaymentCheckoutResponse>> checkout(
            @Valid @RequestBody PaymentCheckoutRequest request
    ) {
        PaymentCheckoutResponse response = paymentService.checkout(request);
        return ResponseEntity.ok(ApiResponse.success("Paid", response));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @Valid @RequestBody PaymentCancelRequest request
    ) {
        paymentService.cancelByTable(request.getTableId());
        return ResponseEntity.ok(ApiResponse.success("Cancelled", null));
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> updateItem(
            @PathVariable Integer itemId,
            @Valid @RequestBody PaymentUpdateItemRequest request
    ) {
        paymentService.updateItemQuantity(itemId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Updated", null));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Integer itemId
    ) {
        paymentService.updateItemQuantity(itemId, 0);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
