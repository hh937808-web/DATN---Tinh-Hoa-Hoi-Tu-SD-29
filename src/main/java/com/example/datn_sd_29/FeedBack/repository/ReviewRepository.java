package com.example.datn_sd_29.FeedBack.repository;



import com.example.datn_sd_29.FeedBack.entity.Review;
import com.example.datn_sd_29.FeedBack.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {


    List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);

        List<Review> findAllByOrderByCreatedAtDesc();
    }

