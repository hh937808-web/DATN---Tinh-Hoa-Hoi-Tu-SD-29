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
            // Fallback: lấy ảnh đầu tiên nếu không có primary
            List<Image> allImages = imageRepository.findByBlogPost_IdOrderByIsPrimaryDesc(post.getId());
            if (!allImages.isEmpty()) {
                res.setThumbnailUrl(allImages.get(0).getImageUrl());
            }
        }
        return res;
    }

    // Admin: lấy tất cả bài viết
    public List<BlogPostResponse> getAllPosts() {
        return blogPostRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Customer: chỉ lấy bài đã xuất bản và chưa hết hạn
    public List<BlogPostResponse> getPublishedPosts() {
        return blogPostRepository.findActivePublished(Instant.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Customer: lấy theo danh mục
    public List<BlogPostResponse> getPublishedByCategory(String category) {
        return blogPostRepository.findActivePublishedByCategory(category, Instant.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Lấy chi tiết + tăng lượt xem
    public BlogPostResponse getPostById(Integer id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại"));
        post.setViewCount(post.getViewCount() + 1);
        blogPostRepository.save(post);
        return toResponse(post);
    }

    // Admin: tạo bài viết
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

        // Lấy tên admin từ context
        post.setAuthor(getAdminName());

        applyPublishState(post, request);

        return toResponse(blogPostRepository.save(post));
    }

    // Admin: cập nhật bài viết
    public BlogPostResponse updatePost(Integer id, BlogPostRequest request) {
        validateSchedule(request);

        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại"));

        post.setTitle(request.getTitle());
        post.setSummary(request.getSummary());
        post.setContent(request.getContent());
        post.setThumbnailUrl(request.getThumbnailUrl());
        post.setCategory(request.getCategory());
        post.setUpdatedAt(Instant.now());
        post.setExpiresAt(request.getExpiresAt());

        applyPublishState(post, request);

        return toResponse(blogPostRepository.save(post));
    }

    // Admin: xóa bài viết
    public void deletePost(Integer id) {
        if (!blogPostRepository.existsById(id)) {
            throw new IllegalArgumentException("Bài viết không tồn tại");
        }
        blogPostRepository.deleteById(id);
    }

    // Admin: tìm kiếm
    public List<BlogPostResponse> searchPosts(String keyword) {
        return blogPostRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Quyết định trạng thái xuất bản dựa trên request
    // - scheduledPublishAt ở tương lai → lưu lịch, để draft
    // - scheduledPublishAt null/quá khứ + isPublished=true → đăng ngay
    // - isPublished=false → draft
    private void applyPublishState(BlogPost post, BlogPostRequest request) {
        Instant now = Instant.now();
        Instant scheduled = request.getScheduledPublishAt();

        if (scheduled != null && scheduled.isAfter(now)) {
            // Lên lịch cho tương lai
            post.setScheduledPublishAt(scheduled);
            post.setIsPublished(false);
            // Giữ nguyên publishedAt cũ nếu có (để lưu lịch sử)
            return;
        }

        // Không lên lịch (hoặc lịch ở quá khứ) → xử lý theo isPublished
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

    private String getAdminName() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getPrincipal().toString();
            }
        } catch (Exception ignored) {}
        return "Admin";
    }
}
