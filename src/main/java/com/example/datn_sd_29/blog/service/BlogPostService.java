package com.example.datn_sd_29.blog.service;

import com.example.datn_sd_29.blog.dto.BlogPostRequest;
import com.example.datn_sd_29.blog.dto.BlogPostResponse;
import com.example.datn_sd_29.blog.entity.BlogPost;
import com.example.datn_sd_29.blog.repository.BlogPostRepository;
import com.example.datn_sd_29.media.entity.Image;
import com.example.datn_sd_29.media.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogPostService {

    private final BlogPostRepository blogPostRepository;
    private final ImageRepository imageRepository;

    private BlogPostResponse toResponse(BlogPost post) {
        BlogPostResponse res = new BlogPostResponse(post);
        // Load ảnh bìa (primary) từ Image table
        List<Image> primaryImages = imageRepository.findByBlogPost_IdAndIsPrimaryTrue(post.getId());
        if (!primaryImages.isEmpty()) {
            res.setThumbnailUrl(primaryImages.get(0).getImageUrl());
        } else {
            List<Image> allImages = imageRepository.findByBlogPost_IdOrderByIsPrimaryDesc(post.getId());
            if (!allImages.isEmpty()) {
                res.setThumbnailUrl(allImages.get(0).getImageUrl());
            }
        }
        return res;
    }

    // Admin: bài còn hoạt động (chưa vô hiệu hóa)
    public List<BlogPostResponse> getAllPosts() {
        return blogPostRepository.findAllActive().stream().map(this::toResponse).toList();
    }

    // Admin: bài đã vô hiệu hóa
    public List<BlogPostResponse> getDisabledPosts() {
        return blogPostRepository.findAllDisabled().stream().map(this::toResponse).toList();
    }

    // Customer: chỉ bài đã xuất bản, chưa hết hạn, chưa vô hiệu
    public List<BlogPostResponse> getPublishedPosts() {
        return blogPostRepository.findActivePublished(Instant.now()).stream().map(this::toResponse).toList();
    }

    public List<BlogPostResponse> getPublishedByCategory(String category) {
        return blogPostRepository.findActivePublishedByCategory(category, Instant.now())
                .stream().map(this::toResponse).toList();
    }

    public BlogPostResponse getPostById(Integer id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại"));
        // Bài đã vô hiệu hóa → không cho customer xem
        if (post.getDisabledAt() != null) {
            throw new IllegalArgumentException("Bài viết không khả dụng");
        }
        post.setViewCount(post.getViewCount() + 1);
        blogPostRepository.save(post);
        return toResponse(post);
    }

    public BlogPostResponse createPost(BlogPostRequest request) {
        validateSchedule(request);

        BlogPost post = new BlogPost();
        post.setTitle(request.getTitle());
        post.setSummary(request.getSummary());
        post.setContent(request.getContent());
        post.setThumbnailUrl(request.getThumbnailUrl());
        post.setCategory(request.getCategory());
        post.setCreatedAt(Instant.now());
        post.setViewCount(0);
        post.setExpiresAt(request.getExpiresAt());
        post.setAuthor(getCurrentUserName());

        applyPublishState(post, request);

        return toResponse(blogPostRepository.save(post));
    }

    public BlogPostResponse updatePost(Integer id, BlogPostRequest request) {
        validateSchedule(request);

        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại"));
        if (post.getDisabledAt() != null) {
            throw new IllegalArgumentException("Không thể chỉnh sửa bài đã vô hiệu hóa. Vui lòng kích hoạt lại trước.");
        }

        // AUDIT DIFF — snapshot BEFORE modification
        java.util.Map<String, Object> before = com.example.datn_sd_29.audit.util.AuditDiffUtil.snapshot(
                post,
                "title", "summary", "category", "content",
                "isPublished", "scheduledPublishAt", "expiresAt", "thumbnailUrl"
        );

        post.setTitle(request.getTitle());
        post.setSummary(request.getSummary());
        post.setContent(request.getContent());
        post.setThumbnailUrl(request.getThumbnailUrl());
        post.setCategory(request.getCategory());
        post.setUpdatedAt(Instant.now());
        post.setExpiresAt(request.getExpiresAt());

        applyPublishState(post, request);
        BlogPost saved = blogPostRepository.save(post);

        // AUDIT DIFF — snapshot AFTER save, tính diff và đẩy lên context
        java.util.Map<String, Object> after = com.example.datn_sd_29.audit.util.AuditDiffUtil.snapshot(
                saved,
                "title", "summary", "category", "content",
                "isPublished", "scheduledPublishAt", "expiresAt", "thumbnailUrl"
        );
        com.example.datn_sd_29.audit.context.AuditContext.setChanges(
                com.example.datn_sd_29.audit.util.AuditDiffUtil.diff(before, after, BLOG_FIELD_LABELS)
        );

        return toResponse(saved);
    }

    // Nhãn tiếng Việt cho các field của BlogPost (hiển thị trong audit diff)
    private static final java.util.Map<String, String> BLOG_FIELD_LABELS = java.util.Map.of(
            "title", "Tiêu đề",
            "summary", "Tóm tắt",
            "category", "Danh mục",
            "content", "Nội dung",
            "isPublished", "Trạng thái xuất bản",
            "scheduledPublishAt", "Thời gian lên lịch đăng",
            "expiresAt", "Hạn kết thúc",
            "thumbnailUrl", "Ảnh đại diện"
    );

    // ===== SOFT DELETE (Vô hiệu hóa) =====

    public BlogPostResponse disablePost(Integer id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại"));
        if (post.getDisabledAt() != null) {
            throw new IllegalArgumentException("Bài đã được vô hiệu hóa trước đó");
        }
        post.setDisabledAt(Instant.now());
        post.setDisabledBy(getCurrentUserName());
        post.setIsPublished(false); // gỡ khỏi customer ngay
        return toResponse(blogPostRepository.save(post));
    }

    public BlogPostResponse restorePost(Integer id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại"));
        if (post.getDisabledAt() == null) {
            throw new IllegalArgumentException("Bài chưa bị vô hiệu hóa");
        }
        post.setDisabledAt(null);
        post.setDisabledBy(null);
        post.setUpdatedAt(Instant.now());
        // Bài khôi phục về trạng thái DRAFT (admin tự quyết xuất bản lại)
        return toResponse(blogPostRepository.save(post));
    }

    // Hard delete — CHỈ cho phép với bài đã bị vô hiệu hóa + xác nhận từ admin
    public void permanentlyDeletePost(Integer id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại"));
        if (post.getDisabledAt() == null) {
            throw new IllegalArgumentException("Chỉ có thể xóa vĩnh viễn bài đã bị vô hiệu hóa");
        }
        // Xóa ảnh trước để tránh FK violation
        List<Image> images = imageRepository.findByBlogPost_IdOrderByIsPrimaryDesc(id);
        if (!images.isEmpty()) {
            imageRepository.deleteAll(images);
        }
        blogPostRepository.deleteById(id);
    }

    public List<BlogPostResponse> searchPosts(String keyword) {
        return blogPostRepository.searchActive(keyword).stream().map(this::toResponse).toList();
    }

    // ===== HELPERS =====

    private void applyPublishState(BlogPost post, BlogPostRequest request) {
        Instant now = Instant.now();
        Instant scheduled = request.getScheduledPublishAt();

        if (scheduled != null && scheduled.isAfter(now)) {
            post.setScheduledPublishAt(scheduled);
            post.setIsPublished(false);
            return;
        }

        post.setScheduledPublishAt(null);

        if (Boolean.TRUE.equals(request.getIsPublished())) {
            if (!Boolean.TRUE.equals(post.getIsPublished())) {
                post.setPublishedAt(now);
            }
            post.setIsPublished(true);
        } else {
            post.setIsPublished(false);
        }
    }

    private void validateSchedule(BlogPostRequest request) {
        Instant scheduled = request.getScheduledPublishAt();
        Instant expires = request.getExpiresAt();

        if (scheduled != null && expires != null && !expires.isAfter(scheduled)) {
            throw new IllegalArgumentException("Hạn kết thúc phải sau thời gian đăng");
        }
        if (scheduled == null && expires != null && !expires.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Hạn kết thúc phải ở tương lai");
        }
    }

    private String getCurrentUserName() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getPrincipal().toString();
            }
        } catch (Exception ignored) {}
        return "Admin";
    }
}
