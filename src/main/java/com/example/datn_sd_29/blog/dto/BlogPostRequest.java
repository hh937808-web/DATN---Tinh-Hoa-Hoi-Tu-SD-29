package com.example.datn_sd_29.blog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

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

    // Thời gian tự đăng bài (null = đăng ngay hoặc đang ở bản nháp)
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "Asia/Ho_Chi_Minh")
    private Instant scheduledPublishAt;

    // Hạn kết thúc bài đăng (null = vô thời hạn, admin tự gỡ)
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "Asia/Ho_Chi_Minh")
    private Instant expiresAt;
}
