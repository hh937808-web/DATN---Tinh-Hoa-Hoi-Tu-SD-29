package com.example.datn_sd_29.blog.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "BlogPost")
public class BlogPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_post_id", nullable = false)
    private Integer id;

    @Size(max = 300)
    @Nationalized
    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Size(max = 500)
    @Nationalized
    @Column(name = "summary", length = 500)
    private String summary;

    @Nationalized
    @Column(name = "content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "thumbnail_url", columnDefinition = "NVARCHAR(MAX)")
    private String thumbnailUrl;

    @Size(max = 100)
    @Nationalized
    @Column(name = "category", length = 100)
    private String category;

    @Size(max = 200)
    @Nationalized
    @Column(name = "author", length = 200)
    private String author;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "scheduled_publish_at")
    private Instant scheduledPublishAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
