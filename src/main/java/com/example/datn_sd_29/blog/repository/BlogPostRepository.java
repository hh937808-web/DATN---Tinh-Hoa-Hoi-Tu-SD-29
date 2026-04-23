package com.example.datn_sd_29.blog.repository;

import com.example.datn_sd_29.blog.entity.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BlogPostRepository extends JpaRepository<BlogPost, Integer> {

    List<BlogPost> findAllByOrderByCreatedAtDesc();

    List<BlogPost> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);

    // Customer: bài đã xuất bản và chưa hết hạn
    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.isPublished = true
              AND (b.expiresAt IS NULL OR b.expiresAt > :now)
            ORDER BY b.publishedAt DESC
            """)
    List<BlogPost> findActivePublished(@Param("now") Instant now);

    // Customer: bài đã xuất bản theo danh mục, chưa hết hạn
    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.isPublished = true
              AND b.category = :category
              AND (b.expiresAt IS NULL OR b.expiresAt > :now)
            ORDER BY b.publishedAt DESC
            """)
    List<BlogPost> findActivePublishedByCategory(@Param("category") String category,
                                                 @Param("now") Instant now);

    // Scheduler: tìm bài đang ở bản nháp nhưng đã đến giờ đăng
    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.isPublished = false
              AND b.scheduledPublishAt IS NOT NULL
              AND b.scheduledPublishAt <= :now
            """)
    List<BlogPost> findPostsDueToPublish(@Param("now") Instant now);

    // Scheduler: tìm bài đã xuất bản nhưng đã hết hạn
    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.isPublished = true
              AND b.expiresAt IS NOT NULL
              AND b.expiresAt <= :now
            """)
    List<BlogPost> findPostsDueToExpire(@Param("now") Instant now);
}
