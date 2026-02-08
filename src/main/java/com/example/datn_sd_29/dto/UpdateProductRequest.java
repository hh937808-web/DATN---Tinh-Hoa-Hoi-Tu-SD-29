package com.example.datn_sd_29.dto;

import com.example.datn_sd_29.enums.ProductStatus;
import lombok.Data;

@Data
public class UpdateProductRequest {

    private ProductStatus availabilityStatus;
}
