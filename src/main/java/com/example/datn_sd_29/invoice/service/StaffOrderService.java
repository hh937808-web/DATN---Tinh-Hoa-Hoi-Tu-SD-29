package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import com.example.datn_sd_29.invoice.dto.InvoiceGroupResponse;
import com.example.datn_sd_29.invoice.dto.InvoiceItemResponse;
import com.example.datn_sd_29.invoice.dto.OrderItemRequest;
import com.example.datn_sd_29.invoice.dto.StaffTableResponse;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffOrderService {
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String TABLE_STATUS_IN_USE = "IN_USE";

    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final ProductComboRepository productComboRepository;
    private final DiningTableRepository diningTableRepository;
    private final com.example.datn_sd_29.common.service.KitchenBroadcastService kitchenBroadcastService;

    public List<InvoiceGroupResponse> getInProgressInvoices() {
        List<Invoice> invoices = invoiceRepository.findAllInProgressInvoicesWithCustomer();
        
        return invoices.stream()
                .map(this::mapToInvoiceGroupResponse)
                .collect(Collectors.toList());
    }

    private InvoiceGroupResponse mapToInvoiceGroupResponse(Invoice invoice) {
        List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository
                .findByInvoiceIdWithTable(invoice.getId());
        
        List<InvoiceGroupResponse.TableInfo> tableInfos = invoiceTables.stream()
                .map(idt -> InvoiceGroupResponse.TableInfo.builder()
                        .tableId(idt.getDiningTable().getId())
                        .tableName(idt.getDiningTable().getTableName())
                        .floor(idt.getDiningTable().getFloor())
                        .area(idt.getDiningTable().getArea())
                        .build())
                .collect(Collectors.toList());
        
        String customerName = invoice.getCustomer() != null 
                ? invoice.getCustomer().getFullName() 
                : "Khách vãng lai";
        
        return InvoiceGroupResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .customerName(customerName)
                .guestCount(invoice.getGuestCount())
                .subtotalAmount(invoice.getSubtotalAmount() != null 
                        ? invoice.getSubtotalAmount() 
                        : BigDecimal.ZERO)
                .checkedInAt(invoice.getCheckedInAt())
                .tables(tableInfos)
                .build();
    }

    public List<StaffTableResponse> getServingTables() {
        List<DiningTable> tables = diningTableRepository.findAll();
        return tables.stream()
                .filter(t -> TABLE_STATUS_IN_USE.equals(t.getTableStatus()))
                .map(this::mapToStaffTableResponse)
                .collect(Collectors.toList());
    }

    public StaffTableResponse getTableById(Integer tableId) {
        DiningTable table = diningTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Table not found"
                ));
        return mapToStaffTableResponse(table);
    }

    private StaffTableResponse mapToStaffTableResponse(DiningTable table) {
        return StaffTableResponse.builder()
                .tableId(table.getId())
                .tableName(table.getTableName())
                .floor(table.getFloor())
                .seatingCapacity(table.getSeatingCapacity())
                .tableStatus(table.getTableStatus())
                .build();
    }

    @Transactional
    public void addItemsToInvoice(Integer invoiceId, List<OrderItemRequest> items) {
        if (invoiceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice id is required");
        }
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invoice not found"
                ));
        
        if (!STATUS_IN_PROGRESS.equals(invoice.getInvoiceStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invoice is not in progress");
        }
        
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Items is required");
        }

        // Get first table from invoice for dining table reference
        List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository
                .findByInvoiceIdWithTable(invoiceId);
        if (invoiceTables.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invoice has no associated tables");
        }
        DiningTable diningTable = invoiceTables.get(0).getDiningTable();

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
        
        // Broadcast kitchen update
        kitchenBroadcastService.broadcastBulkKitchenUpdate("ITEMS_ORDERED", toSave.size());
    }

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
        
        // Broadcast kitchen update
        kitchenBroadcastService.broadcastBulkKitchenUpdate("ITEMS_ORDERED", toSave.size());
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

    public List<InvoiceItemResponse> getInvoiceItems(Integer invoiceId) {
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithItem(invoiceId);
        
        return items.stream()
                .filter(item -> item.getStatus() != InvoiceItemStatus.CANCELLED)
                .map(item -> {
                    InvoiceItemResponse response = new InvoiceItemResponse();
                    response.setId(item.getId());
                    
                    // Get item name from product or combo
                    if (item.getProduct() != null) {
                        response.setItemName(item.getProduct().getProductName());
                    } else if (item.getProductCombo() != null) {
                        response.setItemName(item.getProductCombo().getComboName());
                    }
                    
                    response.setQuantity(item.getQuantity());
                    response.setPrice(item.getUnitPrice());
                    response.setStatus(item.getStatus().name());
                    
                    return response;
                })
                .toList();
    }
}
