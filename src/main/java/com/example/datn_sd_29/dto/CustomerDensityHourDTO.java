package com.example.datn_sd_29.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDensityHourDTO {
    private Integer hour;
    private Long totalCustomers;
}
