package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import com.example.datn_sd_29.invoice.dto.OrderItemRequest;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.entity.InvoiceItemStatus;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceItemRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product.repository.ProductRepository;
import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import com.example.datn_sd_29.product_combo.repository.ProductComboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffOrderService {
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final ProductComboRepository productComboRepository;
    private final DiningTableRepository diningTableRepository;

    @Transactional
    public void addItemsToTable(Integer tableId, List<OrderItemRequest> items) {
        if (tableId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table id is required");
        }
        DiningTable diningTable = diningTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dining table not found"
                ));
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Items is required");
        }

        Invoice invoice = getSingleInProgressInvoice(tableId);

        List<InvoiceItem> toSave = new ArrayList<>();
        BigDecimal addedSubtotal = BigDecimal.ZERO;

        for (OrderItemRequest item : items) {
            if (item.getQuantity() == null || item.getQuantity() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be >= 1");
            }

            String type = item.getItemType() == null ? "" : item.getItemType().trim().toUpperCase();
            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setInvoice(invoice);
            invoiceItem.setItemType(type);
            invoiceItem.setQuantity(item.getQuantity());
            invoiceItem.setDiningTable(diningTable);
            invoiceItem.setStatus(InvoiceItemStatus.ORDERED);

            if ("PRODUCT".equals(type)) {
                if (item.getProductId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
                }
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Product not found"));
                invoiceItem.setProduct(product);
                invoiceItem.setUnitPrice(product.getUnitPrice());
            } else if ("COMBO".equals(type)) {
                if (item.getProductComboId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productComboId is required");
                }
                ProductCombo combo = productComboRepository.findById(item.getProductComboId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Combo not found"));
                invoiceItem.setProductCombo(combo);
                invoiceItem.setUnitPrice(combo.getComboPrice());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid itemType");
            }

            if (invoiceItem.getUnitPrice() != null) {
                addedSubtotal = addedSubtotal.add(
                        invoiceItem.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                );
            }

            toSave.add(invoiceItem);
        }

        invoiceItemRepository.saveAll(toSave);

        BigDecimal currentSubtotal = invoice.getSubtotalAmount() == null
                ? BigDecimal.ZERO
                : invoice.getSubtotalAmount();
        invoice.setSubtotalAmount(currentSubtotal.add(addedSubtotal));
        invoiceRepository.save(invoice);
    }

    /**
     * Safely get single IN_PROGRESS invoice for a table.
     * Throws CONFLICT error if multiple invoices found (merged table issue).
     * Throws NOT_FOUND if no invoice found.
     */
    private Invoice getSingleInProgressInvoice(Integer tableId) {
        if (tableId == null || tableId < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table id is invalid");
        }

        List<Invoice> invoices = invoiceDiningTableRepository.findDistinctInvoicesByTableAndStatuses(
                tableId,
                List.of(STATUS_IN_PROGRESS)
        );

        if (invoices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "No active invoice for table #" + tableId);
        }
        
        if (invoices.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Table #" + tableId + " is attached to multiple active invoices. Please resolve the conflict.");
        }
        
        return invoices.get(0);
    }
}
