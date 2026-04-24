package com.example.datn_sd_29.blog.scheduler;

import com.example.datn_sd_29.blog.entity.BlogPost;
import com.example.datn_sd_29.blog.repository.BlogPostRepository;
import com.example.datn_sd_29.media.entity.Image;
import com.example.datn_sd_29.media.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlogPostScheduler {

    private final BlogPostRepository blogPostRepository;
    private final ImageRepository imageRepository;

    // Số ngày giữ bài đã vô hiệu hóa trước khi xóa vĩnh viễn
    private static final int DISABLED_RETENTION_DAYS = 30;

    // Mỗi phút: đăng bài đến giờ + gỡ bài hết hạn
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processScheduledPosts() {
        Instant now = Instant.now();
        publishDuePosts(now);
        expireDuePosts(now);
    }

    // Hàng ngày lúc 3h sáng: dọn bài đã vô hiệu hóa > 30 ngày (hard delete)
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupDisabledPosts() {
        Instant threshold = Instant.now().minus(DISABLED_RETENTION_DAYS, ChronoUnit.DAYS);
        List<BlogPost> oldDisabled = blogPostRepository.findPostsDueForCleanup(threshold);
        if (oldDisabled.isEmpty()) return;

        for (BlogPost post : oldDisabled) {
            // Xóa ảnh trước để tránh FK violation
            List<Image> images = imageRepository.findByBlogPost_IdOrderByIsPrimaryDesc(post.getId());
            if (!images.isEmpty()) {
                imageRepository.deleteAll(images);
            }
        }
        blogPostRepository.deleteAll(oldDisabled);
        log.info("Dọn sạch {} bài đã vô hiệu hóa quá {} ngày", oldDisabled.size(), DISABLED_RETENTION_DAYS);
    }

    private void publishDuePosts(Instant now) {
        List<BlogPost> due = blogPostRepository.findPostsDueToPublish(now);
        if (due.isEmpty()) return;

        for (BlogPost post : due) {
            post.setIsPublished(true);
            post.setPublishedAt(now);
            post.setScheduledPublishAt(null);
            post.setUpdatedAt(now);
        }
        blogPostRepository.saveAll(due);
        log.info("Đã tự đăng {} bài viết đến giờ xuất bản", due.size());
    }

    private void expireDuePosts(Instant now) {
        List<BlogPost> expired = blogPostRepository.findPostsDueToExpire(now);
        if (expired.isEmpty()) return;

        for (BlogPost post : expired) {
            post.setIsPublished(false);
            post.setUpdatedAt(now);
        }
        blogPostRepository.saveAll(expired);
        log.info("Đã gỡ {} bài viết hết hạn", expired.size());
    }
}
