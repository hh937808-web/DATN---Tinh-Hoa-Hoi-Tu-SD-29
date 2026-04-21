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

    // Customer: chỉ lấy bài đã xuất bản
    public List<BlogPostResponse> getPublishedPosts() {
        return blogPostRepository.findByIsPublishedTrueOrderByPublishedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Customer: lấy theo danh mục
    public List<BlogPostResponse> getPublishedByCategory(String category) {
        return blogPostRepository.findByCategoryAndIsPublishedTrueOrderByPublishedAtDesc(category)
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
        BlogPost post = new BlogPost();
        post.setTitle(request.getTitle());
        post.setSummary(request.getSummary());
        post.setContent(request.getContent());
        post.setThumbnailUrl(request.getThumbnailUrl());
        post.setCategory(request.getCategory());
        post.setCreatedAt(Instant.now());
        post.setViewCount(0);

        // Lấy tên admin từ context
        String author = getAdminName();
        post.setAuthor(author);

        if (Boolean.TRUE.equals(request.getIsPublished())) {
            post.setIsPublished(true);
            post.setPublishedAt(Instant.now());
        } else {
            post.setIsPublished(false);
        }

        return new BlogPostResponse(blogPostRepository.save(post));
    }

    // Admin: cập nhật bài viết
    public BlogPostResponse updatePost(Integer id, BlogPostRequest request) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại"));

        post.setTitle(request.getTitle());
        post.setSummary(request.getSummary());
        post.setContent(request.getContent());
        post.setThumbnailUrl(request.getThumbnailUrl());
        post.setCategory(request.getCategory());
        post.setUpdatedAt(Instant.now());

        // Xử lý trạng thái xuất bản
        if (Boolean.TRUE.equals(request.getIsPublished()) && !Boolean.TRUE.equals(post.getIsPublished())) {
            post.setIsPublished(true);
            post.setPublishedAt(Instant.now());
        } else if (Boolean.FALSE.equals(request.getIsPublished())) {
            post.setIsPublished(false);
        }

        return new BlogPostResponse(blogPostRepository.save(post));
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
                .map(BlogPostResponse::new)
                .toList();
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
