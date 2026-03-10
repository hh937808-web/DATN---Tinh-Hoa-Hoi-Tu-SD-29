package com.example.datn_sd_29.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ReservationResponse {
    private String reservationCode;
    private LocalDateTime reservedAt;
    private Integer guestCount;
    private String fullName;
    private String phoneNumber;
    private String promotionType;
    private String note;
    private String foodNote;
    private List<TableInfo> tables;

    @Data
    @AllArgsConstructor
    public static class TableInfo {
        private Integer id;
        private String tableCode;
        private String tableName;
        private Integer seatingCapacity;
    }
}
