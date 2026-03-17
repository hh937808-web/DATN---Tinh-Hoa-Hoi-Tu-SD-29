package com.example.datn_sd_29.invoice.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.invoice.dto.TableOrderRequest;
import com.example.datn_sd_29.invoice.service.StaffOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class StaffOrderController {
    private final StaffOrderService staffOrderService;

    @PostMapping("/{tableId}/order-items")
    public ResponseEntity<ApiResponse<Void>> addItemsToTable(
            @PathVariable Integer tableId,
            @Valid @RequestBody TableOrderRequest request
    ) {
        staffOrderService.addItemsToTable(tableId, request.getItems());
        return ResponseEntity.ok(ApiResponse.success("Order success", null));
    }
}
