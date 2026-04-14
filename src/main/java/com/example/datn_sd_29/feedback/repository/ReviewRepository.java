package com.example.datn_sd_29.feedback.repository;



import com.example.datn_sd_29.feedback.entity.Review;
import com.example.datn_sd_29.feedback.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {


    List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);

        List<Review> findAllByOrderByCreatedAtDesc();
    }

