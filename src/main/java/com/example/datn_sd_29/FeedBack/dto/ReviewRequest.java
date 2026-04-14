package com.example.datn_sd_29.feedback.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotBlank(message = "Vui lòng nhập tên")
    private String name;

    @NotNull(message = "Vui lòng chọn số sao")
    @Min(value = 1, message = "Số sao phải từ 1 đến 5")
    @Max(value = 5, message = "Số sao phải từ 1 đến 5")
    private Integer rating;

    @NotBlank(message = "Vui lòng nhập nội dung đánh giá")
    @Size(min = 5, max = 2000, message = "Nội dung từ 5 đến 2000 ký tự")
    private String content;

    private String visitType;
    private String tip;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer serviceScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer foodScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer valueScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer atmosphereScore;

    private String avatarUrl;
}