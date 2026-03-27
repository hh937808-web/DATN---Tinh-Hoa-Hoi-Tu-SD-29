package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.invoice.dto.KitchenItemResponse;
import com.example.datn_sd_29.invoice.dto.KitchenTableGroupResponse;
import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.entity.InvoiceItemStatus;
import com.example.datn_sd_29.invoice.repository.InvoiceItemRepository;
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

            map.putIfAbsent(tableId,
                    new KitchenTableGroupResponse(
                            tableId,
                            table.getTableName(),
                            new ArrayList<>(),
                            0
                    )
            );

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
    }

    // ================= DONE COOKING =================
    @Transactional
    public void doneCooking(Integer id) {

        InvoiceItem item = getItemOrThrow(id);

        if (item.getStatus() != InvoiceItemStatus.IN_PROGRESS) {
            throw new RuntimeException("Only IN_PROGRESS items can be marked DONE");
        }

        item.setStatus(InvoiceItemStatus.DONE);
    }

    // ================= SERVE =================
    @Transactional
    public void serveItem(Integer id) {

        InvoiceItem item = getItemOrThrow(id);

        if (item.getStatus() != InvoiceItemStatus.DONE) {
            throw new RuntimeException("Only DONE items can be served");
        }

        item.setStatus(InvoiceItemStatus.SERVED);
    }

    // ================= CANCEL =================
    @Transactional
    public void cancelItem(Integer id) {

        InvoiceItem item = getItemOrThrow(id);

        if (item.getStatus() != InvoiceItemStatus.ORDERED) {
            throw new RuntimeException("Only ORDERED items can be cancelled");
        }

        item.setStatus(InvoiceItemStatus.CANCELLED);
    }

    // ================= COMMON =================
    private InvoiceItem getItemOrThrow(Integer id) {
        return invoiceItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice item not found"));
    }
}