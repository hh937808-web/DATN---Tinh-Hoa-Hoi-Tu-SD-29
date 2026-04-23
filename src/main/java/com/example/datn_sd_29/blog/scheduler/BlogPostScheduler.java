package com.example.datn_sd_29.blog.scheduler;

import com.example.datn_sd_29.blog.entity.BlogPost;
import com.example.datn_sd_29.blog.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlogPostScheduler {

    private final BlogPostRepository blogPostRepository;

    // Chạy mỗi phút: đăng bài đã đến giờ và gỡ bài đã hết hạn
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processScheduledPosts() {
        Instant now = Instant.now();
        publishDuePosts(now);
        expireDuePosts(now);
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
