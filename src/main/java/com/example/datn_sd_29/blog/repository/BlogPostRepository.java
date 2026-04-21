package com.example.datn_sd_29.blog.repository;

import com.example.datn_sd_29.blog.entity.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlogPostRepository extends JpaRepository<BlogPost, Integer> {

    List<BlogPost> findByIsPublishedTrueOrderByPublishedAtDesc();

    List<BlogPost> findAllByOrderByCreatedAtDesc();

    List<BlogPost> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);

    List<BlogPost> findByCategoryAndIsPublishedTrueOrderByPublishedAtDesc(String category);
}
