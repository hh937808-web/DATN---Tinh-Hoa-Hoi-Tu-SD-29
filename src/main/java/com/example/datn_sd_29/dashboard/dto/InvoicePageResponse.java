package com.example.datn_sd_29.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePageResponse {
    private List<RecentInvoiceResponse> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
}
