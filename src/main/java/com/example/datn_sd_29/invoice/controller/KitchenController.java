package com.example.datn_sd_29.invoice.controller;


import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.invoice.dto.KitchenTableGroupResponse;
import com.example.datn_sd_29.invoice.entity.InvoiceItemStatus;
import com.example.datn_sd_29.invoice.service.KitchenService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
@Validated
@Slf4j
public class KitchenController {

    private final KitchenService kitchenService;

    // ================= GET GROUP BY TABLE =================
    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<List<KitchenTableGroupResponse>>> getKitchenByTable(
            @RequestParam(required = false) List<InvoiceItemStatus> statuses
    ) {

        List<KitchenTableGroupResponse> data =
                kitchenService.getKitchenGroupedByTable(statuses);

        return ResponseEntity.ok(
                ApiResponse.success("Get kitchen items successfully", data)
        );
    }

    // ================= START COOKING =================
    @PutMapping("/items/{id}/start")
    public ResponseEntity<ApiResponse<Void>> startCooking(
            @PathVariable @Min(1) Integer id
    ) {

        log.info("Start cooking item id={}", id);

        kitchenService.startCooking(id);

        return ResponseEntity.ok(
                ApiResponse.success("Start cooking successfully", null)
        );
    }

    // ================= DONE COOKING =================
    @PutMapping("/items/{id}/done")
    public ResponseEntity<ApiResponse<Void>> doneCooking(
            @PathVariable @Min(1) Integer id
    ) {

        log.info("Done cooking item id={}", id);

        kitchenService.doneCooking(id);

        return ResponseEntity.ok(
                ApiResponse.success("Done cooking successfully", null)
        );
    }

    // ================= SERVE =================
    @PutMapping("/items/{id}/serve")
    public ResponseEntity<ApiResponse<Void>> serveItem(
            @PathVariable @Min(1) Integer id
    ) {

        log.info("Serve item id={}", id);

        kitchenService.serveItem(id);

        return ResponseEntity.ok(
                ApiResponse.success("Serve item successfully", null)
        );
    }

    // ================= CANCEL =================
    @PutMapping("/items/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelItem(
            @PathVariable @Min(1) Integer id,
            @RequestParam(required = false) Integer quantityToCancel
    ) {

        log.info("Cancel item id={}, quantityToCancel={}", id, quantityToCancel);

        kitchenService.cancelItem(id, quantityToCancel);

        return ResponseEntity.ok(
                ApiResponse.success("Cancel item successfully", null)
        );
    }
}