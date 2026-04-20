package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.invoice.dto.KitchenItemResponse;
import com.example.datn_sd_29.invoice.dto.KitchenTableGroupResponse;
import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.entity.InvoiceItemStatus;
import com.example.datn_sd_29.invoice.repository.InvoiceItemRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KitchenService {

    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final com.example.datn_sd_29.common.service.KitchenBroadcastService kitchenBroadcastService;

    // ================= GET KITCHEN =================
    public List<KitchenTableGroupResponse> getKitchenGroupedByTable(List<InvoiceItemStatus> statuses) {

        // 🔥 default nếu FE không truyền
        if (statuses == null || statuses.isEmpty()) {
            statuses = List.of(
                    InvoiceItemStatus.ORDERED,
                    InvoiceItemStatus.IN_PROGRESS,
                    InvoiceItemStatus.DONE
            );
        }

        List<InvoiceItem> items = invoiceItemRepository.findKitchenItems(statuses);

        Map<Integer, KitchenTableGroupResponse> map = new LinkedHashMap<>();

        for (InvoiceItem item : items) {

            DiningTable table = item.getDiningTable();
            if (table == null) continue;

            Integer tableId = table.getId();

            if (!map.containsKey(tableId)) {
                // Lấy thông tin từ invoice
                var invoice = item.getInvoice();
                String customerName = "Khách vãng lai";
                if (invoice != null && invoice.getCustomer() != null) {
                    var customer = invoice.getCustomer();
                    customerName = customer.getFullName() != null ? customer.getFullName() : "Khách vãng lai";
                }
                
                Integer guestCount = invoice != null && invoice.getGuestCount() != null 
                    ? invoice.getGuestCount() 
                    : 0;
                
                // Get serving staff info from invoice
                Integer servingStaffId = null;
                String servingStaffName = null;
                if (invoice != null && invoice.getServingStaff() != null) {
                    servingStaffId = invoice.getServingStaff().getId();
                    servingStaffName = invoice.getServingStaff().getFullName();
                }
                
                KitchenTableGroupResponse response = new KitchenTableGroupResponse(
                    tableId,
                    table.getTableName(),
                    customerName,
                    guestCount,
                    java.math.BigDecimal.ZERO, // Sẽ tính sau
                    new ArrayList<>(),
                    0,
                    servingStaffId,
                    servingStaffName
                );
                
                map.put(tableId, response);
            }

            String itemName = "PRODUCT".equals(item.getItemType())
                    ? item.getProduct().getProductName()
                    : item.getProductCombo().getComboName();

            KitchenItemResponse response = new KitchenItemResponse(
                    item.getId(),
                    itemName,
                    item.getQuantity(),
                    item.getStatus().name(),
                    item.getItemType()
            );

            map.get(tableId).getItems().add(response);
            
            // Tính tổng tiền (chỉ tính món chưa CANCELLED)
            if (item.getStatus() != InvoiceItemStatus.CANCELLED && item.getUnitPrice() != null) {
                var currentTotal = map.get(tableId).getTotalAmount();
                var itemTotal = item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()));
                map.get(tableId).setTotalAmount(currentTotal.add(itemTotal));
            }
        }

        return new ArrayList<>(map.values());
    }

    // ================= START COOKING =================
    @Transactional
    public void startCooking(Integer id) {

        InvoiceItem item = getItemOrThrow(id);

        if (item.getStatus() != InvoiceItemStatus.ORDERED) {
            throw new RuntimeException("Only ORDERED items can be started");
        }

        item.setStatus(InvoiceItemStatus.IN_PROGRESS);
        
        // Broadcast kitchen update
        String itemName = getItemName(item);
        kitchenBroadcastService.broadcastKitchenUpdate("STATUS_CHANGED", id, itemName);
    }

    // ================= DONE COOKING =================
    @Transactional
    public void doneCooking(Integer id) {

        InvoiceItem item = getItemOrThrow(id);

        if (item.getStatus() != InvoiceItemStatus.IN_PROGRESS) {
            throw new RuntimeException("Only IN_PROGRESS items can be marked DONE");
        }

        item.setStatus(InvoiceItemStatus.DONE);
        
        // Broadcast kitchen update
        String itemName = getItemName(item);
        kitchenBroadcastService.broadcastKitchenUpdate("STATUS_CHANGED", id, itemName);
    }

    // ================= SERVE =================
    @Transactional
    public void serveItem(Integer id) {

        InvoiceItem item = getItemOrThrow(id);

        if (item.getStatus() != InvoiceItemStatus.DONE) {
            throw new RuntimeException("Only DONE items can be served");
        }

        // Capture name before potential merge/delete (lazy relations still in memory)
        String itemName = getItemName(item);

        item.setStatus(InvoiceItemStatus.SERVED);

        // After marking SERVED, merge with an existing SERVED row of the same item
        // in the same invoice (happens when staff re-orders the same dish).
        Integer invoiceId = item.getInvoice() != null ? item.getInvoice().getId() : null;
        if (invoiceId != null) {
            if (item.getProduct() != null) {
                invoiceItemRepository
                        .findServedByInvoiceAndProductExcluding(invoiceId, item.getProduct().getId(), item.getId())
                        .ifPresent(existing -> {
                            existing.setQuantity(existing.getQuantity() + item.getQuantity());
                            invoiceItemRepository.save(existing);
                            invoiceItemRepository.delete(item);
                        });
            } else if (item.getProductCombo() != null) {
                invoiceItemRepository
                        .findServedByInvoiceAndComboExcluding(invoiceId, item.getProductCombo().getId(), item.getId())
                        .ifPresent(existing -> {
                            existing.setQuantity(existing.getQuantity() + item.getQuantity());
                            invoiceItemRepository.save(existing);
                            invoiceItemRepository.delete(item);
                        });
            }
        }

        // Broadcast kitchen update
        kitchenBroadcastService.broadcastKitchenUpdate("STATUS_CHANGED", id, itemName);
    }

    // ================= CANCEL =================
    @Transactional
    public void cancelItem(Integer id, Integer quantityToCancel) {

        InvoiceItem item = getItemOrThrow(id);

        if (item.getStatus() != InvoiceItemStatus.ORDERED) {
            throw new RuntimeException("Only ORDERED items can be cancelled");
        }

        // Tính số lượng thực tế bị hủy
        int actualQuantityToCancel;
        if (quantityToCancel == null || quantityToCancel >= item.getQuantity()) {
            actualQuantityToCancel = item.getQuantity();
        } else {
            if (quantityToCancel < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity to cancel must be >= 1");
            }
            actualQuantityToCancel = quantityToCancel;
        }

        // Tính số tiền cần trừ khỏi subtotalAmount
        java.math.BigDecimal amountToDeduct = item.getUnitPrice()
                .multiply(java.math.BigDecimal.valueOf(actualQuantityToCancel));

        // Cập nhật invoice subtotalAmount
        var invoice = item.getInvoice();
        if (invoice != null && invoice.getSubtotalAmount() != null) {
            java.math.BigDecimal currentSubtotal = invoice.getSubtotalAmount();
            java.math.BigDecimal newSubtotal = currentSubtotal.subtract(amountToDeduct);
            
            // Đảm bảo subtotal không bị âm
            if (newSubtotal.compareTo(java.math.BigDecimal.ZERO) < 0) {
                newSubtotal = java.math.BigDecimal.ZERO;
            }
            
            invoice.setSubtotalAmount(newSubtotal);
            invoiceRepository.save(invoice);
        }

        // Cập nhật item status/quantity
        if (actualQuantityToCancel >= item.getQuantity()) {
            // Hủy toàn bộ → set status CANCELLED
            item.setStatus(InvoiceItemStatus.CANCELLED);
        } else {
            // Còn lại → giảm quantity, giữ nguyên status ORDERED
            int newQuantity = item.getQuantity() - actualQuantityToCancel;
            item.setQuantity(newQuantity);
        }
        
        // Broadcast kitchen update
        String itemName = getItemName(item);
        kitchenBroadcastService.broadcastKitchenUpdate("ITEM_CANCELLED", id, itemName);
    }

    // ================= COMMON =================
    private InvoiceItem getItemOrThrow(Integer id) {
        return invoiceItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice item not found"));
    }
    
    private String getItemName(InvoiceItem item) {
        if ("PRODUCT".equals(item.getItemType()) && item.getProduct() != null) {
            return item.getProduct().getProductName();
        } else if ("COMBO".equals(item.getItemType()) && item.getProductCombo() != null) {
            return item.getProductCombo().getComboName();
        }
        return "Unknown Item";
    }
}