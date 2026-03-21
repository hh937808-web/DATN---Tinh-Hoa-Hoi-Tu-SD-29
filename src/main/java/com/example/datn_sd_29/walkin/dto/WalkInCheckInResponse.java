package com.example.datn_sd_29.walkin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WalkInCheckInResponse {
    private Integer invoiceId;
    private String invoiceCode;
    private String message;
}
