package com.example.datn_sd_29.invoice.controller;

import com.example.datn_sd_29.invoice.dto.InvoiceDetailResponse;
import com.example.datn_sd_29.invoice.dto.InvoiceResponse;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.invoice.service.InvoiceService;
import com.example.datn_sd_29.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final JwtService jwtService;
    private final InvoiceRepository invoiceRepository;

    public InvoiceController(
            InvoiceService invoiceService,
            JwtService jwtService,
            InvoiceRepository invoiceRepository
    ) {
        this.invoiceService = invoiceService;
        this.jwtService = jwtService;
        this.invoiceRepository = invoiceRepository;
    }

    // =========================
    // LẤY HÓA ĐƠN CỦA CHÍNH MÌNH
    // =========================
    @GetMapping("/my")
    public List<InvoiceResponse> getMyInvoices(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Integer customerId = extractCustomerIdFromAuth(authHeader);
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cần đăng nhập để xem hóa đơn của bạn");
        }
        return invoiceService.getByCustomerId(customerId);
    }

    /**
     * Xem chi tiết hóa đơn — kiểm tra ownership:
     *   - Customer (USER role): chỉ được xem hóa đơn của chính mình
     *   - Staff/Reception/Kitchen/Admin: được xem mọi hóa đơn (cho support khách)
     *   - Khi JWT off (header không có): cho qua (dev mode)
     */
    @GetMapping("/{id}")
    public InvoiceDetailResponse getInvoiceDetail(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String role = jwtService.extractRole(token);

                // Chỉ enforce cho customer (USER). Nhân viên có quyền xem mọi hóa đơn.
                if ("USER".equalsIgnoreCase(role)) {
                    Integer requesterCustomerId = jwtService.extractCustomerId(token);
                    Invoice invoice = invoiceRepository.findById(id)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));
                    Integer ownerId = invoice.getCustomer() != null ? invoice.getCustomer().getId() : null;
                    if (requesterCustomerId == null || !requesterCustomerId.equals(ownerId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Bạn không có quyền xem hóa đơn này");
                    }
                }
            } catch (ResponseStatusException e) {
                throw e;
            } catch (Exception ignored) {
                // Token lỗi → để filter chain xử lý / cho qua (security filter sẽ chặn ở mức authenticated())
            }
        }

        return invoiceService.getInvoiceDetail(id);
    }

    private Integer extractCustomerIdFromAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            return jwtService.extractCustomerId(token);
        } catch (Exception e) {
            return null;
        }
    }
}