package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.invoice.dto.InvoiceDetailResponse;
import com.example.datn_sd_29.invoice.dto.InvoiceResponse;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.repository.InvoiceItemRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceDiningTableRepository invoiceDiningTableRepository;


    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            InvoiceDiningTableRepository invoiceDiningTableRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.invoiceDiningTableRepository = invoiceDiningTableRepository;
    }

    // invoice theo customer
    public List<InvoiceResponse> getByCustomerId(Integer customerId) {
        return invoiceRepository.findByCustomerId(customerId)
                .stream()
                .map(i -> {
                    InvoiceResponse res = new InvoiceResponse();
                    res.setId(i.getId());
                    res.setInvoiceCode(i.getInvoiceCode());
                    res.setStatus(i.getInvoiceStatus());
                    res.setFinalAmount(i.getFinalAmount());
                    res.setReservedAt(i.getReservedAt());
                    res.setGuestCount(i.getGuestCount());
                    res.setEarnedPoints(i.getEarnedPoints());
                    res.setUsedPoints(i.getUsedPoints());
                    return res;
                })
                .toList();
    }

    // invoice detail
    public InvoiceDetailResponse getInvoiceDetail(Integer id) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(id);

        InvoiceDetailResponse response = new InvoiceDetailResponse();

        response.setId(invoice.getId());
        response.setCode(invoice.getInvoiceCode());
        response.setTime(invoice.getReservedAt());

        // ===== KHÁCH HÀNG =====
        if (invoice.getCustomer() != null) {
            response.setCustomerName(invoice.getCustomer().getFullName());
            response.setPhone(invoice.getCustomer().getPhoneNumber());
            response.setEmail(invoice.getCustomer().getEmail());
        }

        // ===== BÀN =====
        List<InvoiceDiningTable> tableLinks =
                invoiceDiningTableRepository.findByInvoiceId(invoice.getId());

        List<String> tableNames = tableLinks.stream()
                .map(link -> link.getDiningTable().getTableName())
                .toList();

        response.setTables(tableNames);

        // ===== TIỀN =====
        response.setSubtotal(invoice.getSubtotalAmount());
        response.setDiscount(invoice.getDiscountAmount());
        response.setServiceFee(invoice.getServiceFeeAmount());
        response.setTax(invoice.getTaxAmount());
        response.setFinalAmount(invoice.getFinalAmount());

        response.setStatus(invoice.getInvoiceStatus());
        response.setPaymentMethod(invoice.getPaymentMethod());

        // ===== DANH SÁCH MÓN =====
        List<InvoiceDetailResponse.Item> itemList = items.stream().map(i -> {

            InvoiceDetailResponse.Item item = new InvoiceDetailResponse.Item();

            item.setId(i.getId());

            if (i.getProduct() != null) {
                item.setName(i.getProduct().getProductName());
            } else if (i.getProductCombo() != null) {
                item.setName(i.getProductCombo().getComboName());
            }

            item.setQuantity(i.getQuantity());
            item.setUnitPrice(i.getUnitPrice());
            item.setLineTotal(i.getLineTotal());

            return item;

        }).toList();

        response.setItems(itemList);

        return response;
    }
}