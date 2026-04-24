package com.example.datn_sd_29.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KitchenItemResponse {
    private Integer id;
    private String itemName;
    private Integer quantity;
    private String status;   // 🔥 bắt buộc
    private String itemType; // 🔥 nên có
    private String note;     // Ghi chú của khách (không cay, ít muối, dị ứng...)
}
