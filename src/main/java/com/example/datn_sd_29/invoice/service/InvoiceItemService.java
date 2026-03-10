package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.invoice.dto.InvoiceItemRequest;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.entity.InvoiceItemStatus;
import com.example.datn_sd_29.invoice.repository.InvoiceItemRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product.repository.ProductRepository;
import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import com.example.datn_sd_29.product_combo.repository.ProductComboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceItemService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductRepository productRepository;
    private final ProductComboRepository productComboRepository;

    public void orderItem(InvoiceItemRequest request) {

        // tìm invoice theo bàn
        Invoice invoice = invoiceRepository
                .findActiveInvoiceByTableId(request.getTableId())
                .orElseThrow(() -> new RuntimeException("Bàn chưa check-in"));

        InvoiceItem item = new InvoiceItem();

        item.setInvoice(invoice);
        item.setItemType(request.getItemType());
        item.setQuantity(request.getQuantity());
        item.setStatus(InvoiceItemStatus.ORDERED);

        if ("PRODUCT".equals(request.getItemType())) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            item.setProduct(product);
        } else {
            ProductCombo combo = productComboRepository.findById(request.getProductComboId())
                    .orElseThrow(() -> new RuntimeException("Combo not found"));
            item.setProductCombo(combo);
        }

        invoiceItemRepository.save(item);
    }
}
