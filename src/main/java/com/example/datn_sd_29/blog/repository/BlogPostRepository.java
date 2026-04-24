package com.example.datn_sd_29.blog.repository;

import com.example.datn_sd_29.blog.entity.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BlogPostRepository extends JpaRepository<BlogPost, Integer> {

    // Admin: bài còn hoạt động (chưa vô hiệu hóa)
    @Query("SELECT b FROM BlogPost b WHERE b.disabledAt IS NULL ORDER BY b.createdAt DESC")
    List<BlogPost> findAllActive();

    // Admin: bài đã bị vô hiệu hóa (thùng rác)
    @Query("SELECT b FROM BlogPost b WHERE b.disabledAt IS NOT NULL ORDER BY b.disabledAt DESC")
    List<BlogPost> findAllDisabled();

    // Admin: tìm kiếm (bỏ qua bài đã vô hiệu)
    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.disabledAt IS NULL
              AND LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY b.createdAt DESC
            """)
    List<BlogPost> searchActive(@Param("keyword") String keyword);

    // Customer: bài đã xuất bản, chưa hết hạn, chưa vô hiệu hóa
    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.isPublished = true
              AND b.disabledAt IS NULL
              AND (b.expiresAt IS NULL OR b.expiresAt > :now)
            ORDER BY b.publishedAt DESC
            """)
    List<BlogPost> findActivePublished(@Param("now") Instant now);

    // Customer: theo danh mục
    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.isPublished = true
              AND b.disabledAt IS NULL
              AND b.category = :category
              AND (b.expiresAt IS NULL OR b.expiresAt > :now)
            ORDER BY b.publishedAt DESC
            """)
    List<BlogPost> findActivePublishedByCategory(@Param("category") String category,
                                                 @Param("now") Instant now);

    // Scheduler: đến giờ đăng (bỏ qua bài vô hiệu)
    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.isPublished = false
              AND b.disabledAt IS NULL
              AND b.scheduledPublishAt IS NOT NULL
              AND b.scheduledPublishAt <= :now
            """)
    List<BlogPost> findPostsDueToPublish(@Param("now") Instant now);

    // Scheduler: đến hạn hết hạn
    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.isPublished = true
              AND b.disabledAt IS NULL
              AND b.expiresAt IS NOT NULL
              AND b.expiresAt <= :now
            """)
    List<BlogPost> findPostsDueToExpire(@Param("now") Instant now);

    // Scheduler cleanup: bài đã bị vô hiệu hóa quá N ngày → hard delete
    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.disabledAt IS NOT NULL
              AND b.disabledAt <= :threshold
            """)
    List<BlogPost> findPostsDueForCleanup(@Param("threshold") Instant threshold);
}
