package com.example.datn_sd_29.blog.dto;

import com.example.datn_sd_29.blog.entity.BlogPost;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;

@Data
public class BlogPostResponse {
    private Integer id;
    private String title;
    private String summary;
    private String content;
    private String thumbnailUrl;
    private String category;
    private String author;
    private Boolean isPublished;
    private Integer viewCount;

    // Trạng thái tổng hợp: DRAFT | SCHEDULED | PUBLISHED | EXPIRED | DISABLED
    private String status;

    private String disabledBy;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant createdAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant updatedAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant publishedAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant scheduledPublishAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant expiresAt;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant disabledAt;

    public BlogPostResponse(BlogPost post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.summary = post.getSummary();
        this.content = post.getContent();
        this.thumbnailUrl = post.getThumbnailUrl();
        this.category = post.getCategory();
        this.author = post.getAuthor();
        this.isPublished = post.getIsPublished();
        this.viewCount = post.getViewCount();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        this.publishedAt = post.getPublishedAt();
        this.scheduledPublishAt = post.getScheduledPublishAt();
        this.expiresAt = post.getExpiresAt();
        this.disabledAt = post.getDisabledAt();
        this.disabledBy = post.getDisabledBy();
        this.status = computeStatus(post);
    }

    private static String computeStatus(BlogPost post) {
        if (post.getDisabledAt() != null) {
            return "DISABLED";
        }
        Instant now = Instant.now();
        if (post.getExpiresAt() != null && post.getExpiresAt().isBefore(now)) {
            return "EXPIRED";
        }
        if (Boolean.TRUE.equals(post.getIsPublished())) {
            return "PUBLISHED";
        }
        if (post.getScheduledPublishAt() != null && post.getScheduledPublishAt().isAfter(now)) {
            return "SCHEDULED";
        }
        return "DRAFT";
    }
}
