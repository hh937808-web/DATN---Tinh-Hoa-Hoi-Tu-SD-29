package com.example.datn_sd_29.walkin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WalkInCheckInRequest {
    private List<Integer> tableIds;
    private Integer guestCount;
    private String customerName; // Optional customer name for walk-in
}
