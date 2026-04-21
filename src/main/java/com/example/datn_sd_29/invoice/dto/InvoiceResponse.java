package com.example.datn_sd_29.invoice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class InvoiceResponse {
    private Integer id;
    private String invoiceCode;
    private String status;
    private BigDecimal finalAmount;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private LocalDateTime reservedAt;
    private Integer guestCount;
    private Integer earnedPoints;   // điểm được cộng sau khi thanh toán
    private Integer usedPoints;     // điểm đã dùng để trừ vào hóa đơn
}