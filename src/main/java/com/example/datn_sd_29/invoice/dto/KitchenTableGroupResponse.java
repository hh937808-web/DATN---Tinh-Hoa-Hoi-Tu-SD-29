package com.example.datn_sd_29.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KitchenTableGroupResponse {
    private Integer tableId;
    private String tableName;
    private String customerName;
    private Integer guestCount;
    private BigDecimal totalAmount;
    private List<KitchenItemResponse> items;
    private Integer totalItems;
}
