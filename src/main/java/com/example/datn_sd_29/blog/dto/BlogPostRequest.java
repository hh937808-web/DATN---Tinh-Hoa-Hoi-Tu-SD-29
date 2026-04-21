package com.example.datn_sd_29.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlogPostRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 300, message = "Tiêu đề tối đa 300 ký tự")
    private String title;

    @Size(max = 500, message = "Tóm tắt tối đa 500 ký tự")
    private String summary;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private String thumbnailUrl;

    @Size(max = 100, message = "Danh mục tối đa 100 ký tự")
    private String category;

    private Boolean isPublished;
}
