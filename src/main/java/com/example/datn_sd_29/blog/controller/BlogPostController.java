package com.example.datn_sd_29.blog.controller;

import com.example.datn_sd_29.blog.dto.AIGenerateRequest;
import com.example.datn_sd_29.blog.dto.BlogPostRequest;
import com.example.datn_sd_29.blog.dto.BlogPostResponse;
import com.example.datn_sd_29.blog.service.BlogAIService;
import com.example.datn_sd_29.blog.service.BlogPostService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogPostController {

    private final BlogPostService blogPostService;
    private final BlogAIService blogAIService;

    // ===== ADMIN =====

    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> getAllPosts() {
        List<BlogPostResponse> posts = blogPostService.getAllPosts();
        return ResponseEntity.ok(Map.of("data", posts));
    }

    @PostMapping("/admin")
    public ResponseEntity<Map<String, Object>> createPost(@Valid @RequestBody BlogPostRequest request) {
        BlogPostResponse post = blogPostService.createPost(request);
        return ResponseEntity.ok(Map.of("data", post));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<Map<String, Object>> updatePost(
            @PathVariable Integer id,
            @Valid @RequestBody BlogPostRequest request
    ) {
        BlogPostResponse post = blogPostService.updatePost(id, request);
        return ResponseEntity.ok(Map.of("data", post));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable Integer id) {
        blogPostService.deletePost(id);
        return ResponseEntity.ok(Map.of("message", "Xóa bài viết thành công"));
    }

    @GetMapping("/admin/search")
    public ResponseEntity<Map<String, Object>> searchPosts(@RequestParam String keyword) {
        List<BlogPostResponse> posts = blogPostService.searchPosts(keyword);
        return ResponseEntity.ok(Map.of("data", posts));
    }

    @PostMapping("/admin/ai-generate")
    public void aiGenerate(@RequestBody AIGenerateRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        blogAIService.streamArticle(
                request.getTitle(),
                request.getSummary(),
                request.getCategory(),
                response.getOutputStream()
        );

        response.flushBuffer();
    }

    // ===== CUSTOMER (PUBLIC) =====

    @GetMapping("/published")
    public ResponseEntity<Map<String, Object>> getPublishedPosts() {
        List<BlogPostResponse> posts = blogPostService.getPublishedPosts();
        return ResponseEntity.ok(Map.of("data", posts));
    }

    @GetMapping("/published/category")
    public ResponseEntity<Map<String, Object>> getByCategory(@RequestParam String category) {
        List<BlogPostResponse> posts = blogPostService.getPublishedByCategory(category);
        return ResponseEntity.ok(Map.of("data", posts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPostDetail(@PathVariable Integer id) {
        BlogPostResponse post = blogPostService.getPostById(id);
        return ResponseEntity.ok(Map.of("data", post));
    }
}
