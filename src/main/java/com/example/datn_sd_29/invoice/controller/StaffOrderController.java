package com.example.datn_sd_29.invoice.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.invoice.dto.InvoiceGroupResponse;
import com.example.datn_sd_29.invoice.dto.InvoiceItemResponse;
import com.example.datn_sd_29.invoice.dto.StaffTableResponse;
import com.example.datn_sd_29.invoice.dto.TableOrderRequest;
import com.example.datn_sd_29.invoice.service.KitchenService;
import com.example.datn_sd_29.invoice.service.StaffOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StaffOrderController {
    private final StaffOrderService staffOrderService;
    private final KitchenService kitchenService;

    @PreAuthorize("hasAnyRole('STAFF', 'RECEPTION', 'ADMIN')")
    @GetMapping("/invoices/in-progress")
    public ResponseEntity<ApiResponse<List<InvoiceGroupResponse>>> getInProgressInvoices() {
        List<InvoiceGroupResponse> invoices = staffOrderService.getInProgressInvoices();
        return ResponseEntity.ok(ApiResponse.success("Success", invoices));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'RECEPTION', 'ADMIN')")
    @GetMapping("/invoices/{invoiceId}/items")
    public ResponseEntity<ApiResponse<List<InvoiceItemResponse>>> getInvoiceItems(@PathVariable Integer invoiceId) {
        List<InvoiceItemResponse> items = staffOrderService.getInvoiceItems(invoiceId);
        return ResponseEntity.ok(ApiResponse.success("Success", items));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'RECEPTION', 'ADMIN')")
    @PostMapping("/invoices/{invoiceId}/order-items")
    public ResponseEntity<ApiResponse<Void>> addItemsToInvoice(
            @PathVariable Integer invoiceId,
            @Valid @RequestBody TableOrderRequest request
    ) {
        staffOrderService.addItemsToInvoice(invoiceId, request.getItems());
        return ResponseEntity.ok(ApiResponse.success("Order success", null));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'RECEPTION', 'ADMIN')")
    @GetMapping("/tables/serving")
    public ResponseEntity<ApiResponse<List<StaffTableResponse>>> getServingTables() {
        List<StaffTableResponse> tables = staffOrderService.getServingTables();
        return ResponseEntity.ok(ApiResponse.success("Success", tables));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'RECEPTION', 'ADMIN')")
    @GetMapping("/tables/staff/{tableId}")
    public ResponseEntity<ApiResponse<StaffTableResponse>> getTableById(@PathVariable Integer tableId) {
        StaffTableResponse table = staffOrderService.getTableById(tableId);
        return ResponseEntity.ok(ApiResponse.success("Success", table));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'RECEPTION', 'ADMIN')")
    @PostMapping("/tables/{tableId}/order-items")
    public ResponseEntity<ApiResponse<Void>> addItemsToTable(
            @PathVariable Integer tableId,
            @Valid @RequestBody TableOrderRequest request
    ) {
        staffOrderService.addItemsToTable(tableId, request.getItems());
        return ResponseEntity.ok(ApiResponse.success("Order success", null));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'RECEPTION', 'ADMIN')")
    @PutMapping("/invoices/items/{itemId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateDessert(@PathVariable Integer itemId) {
        kitchenService.activateItem(itemId);
        return ResponseEntity.ok(ApiResponse.success("Dessert activated", null));
    }
}
