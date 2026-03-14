package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.invoice.dto.UpdateInvoiceRequest;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImp implements InvoiceService {
    private final InvoiceRepository invoiceRepository;

    //Search
    @Override
    public List<Invoice> searchInvoice(
            String invoiceCode,
            String reservationCode,
            String customerName,
            String customerPhone,
            String employeeName,
            String invoiceType,
            String status,
            String paymentMethod
    ) {
        return invoiceRepository.searchInvoice(
                invoiceCode,
                reservationCode,
                customerName,
                customerPhone,
                employeeName,
                invoiceType,
                status,
                paymentMethod
        );
    }

    //Sort
    @Override
    public List<Invoice> sortInvoice(String sortBy, String direction) {
        // Chỉ cho phép sort theo 2 field
        if (!sortBy.equals("id") && !sortBy.equals("finalAmount")) {
            throw new RuntimeException("Chỉ sort theo id và finalAmount");
        }
        Sort sort;
        if ("asc".equalsIgnoreCase(direction)) {
            sort = Sort.by(sortBy).ascending();
        } else {
            sort = Sort.by(sortBy).descending();
        }
        return invoiceRepository.findAll(sort);
    }

    //Get All Invoice
    @Override
    public List<Invoice> getAllInvoice() {
        return invoiceRepository.findAll();
    }

    //Update Invoice
    @Override
    public Invoice updateInvoice(Integer invoiceId, UpdateInvoiceRequest request) {

        // 1 tìm hóa đơn
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // 2 kiểm tra hóa đơn đã thanh toán chưa
        if ("PAID".equals(invoice.getInvoiceStatus())) {
            throw new RuntimeException("Invoice already paid");
        }

        // 3 ÁP DỤNG VOUCHER
        if (request.getDiscountAmount() != null) {

            BigDecimal currentDiscount = invoice.getDiscountAmount() == null
                    ? BigDecimal.ZERO
                    : invoice.getDiscountAmount();

            invoice.setDiscountAmount(
                    currentDiscount.add(request.getDiscountAmount())
            );
        }

        // 4 TRỪ ĐIỂM TÍCH LŨY
        if (request.getUsedPoints() != null && request.getUsedPoints() > 0) {

            Customer customer = invoice.getCustomer();

            // kiểm tra đủ điểm không
            if (customer.getLoyaltyPoints() < request.getUsedPoints()) {
                throw new RuntimeException("Not enough loyalty points");
            }
            // trừ điểm khách hàng
            customer.setLoyaltyPoints(
                    customer.getLoyaltyPoints() - request.getUsedPoints()
            );
            // lưu điểm đã dùng trong hóa đơn
            invoice.setUsedPoints(request.getUsedPoints());
        }

        // 5 PHƯƠNG THỨC THANH TOÁN
        if (request.getPaymentMethod() != null) {
            invoice.setPaymentMethod(request.getPaymentMethod());
        }

        // 6 XUẤT HÓA ĐƠN
        invoice.setInvoiceStatus("PAID");
        invoice.setPaidAt(java.time.Instant.now());
        return invoiceRepository.save(invoice);
    }
}
