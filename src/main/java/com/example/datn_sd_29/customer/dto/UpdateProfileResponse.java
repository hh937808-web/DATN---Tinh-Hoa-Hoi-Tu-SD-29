package com.example.datn_sd_29.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateProfileResponse {
    private String message;
    private String accessToken; // trả nếu email thay đổi
}
