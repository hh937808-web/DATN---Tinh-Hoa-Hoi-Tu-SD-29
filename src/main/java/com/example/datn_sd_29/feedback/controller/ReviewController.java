package com.example.datn_sd_29.feedback.controller;

import com.example.datn_sd_29.feedback.dto.ReviewRequest;
import com.example.datn_sd_29.feedback.dto.ReviewResponse;
import com.example.datn_sd_29.feedback.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ReviewController {

    private final ReviewService reviewService;

    // KHÁCH HÀNG
    @GetMapping
    public List<ReviewResponse> getApprovedReviews() {
        return reviewService.getApprovedReviews();
    }

    @PostMapping
    public ReviewResponse createReview(@Valid @RequestBody ReviewRequest request) {
        return reviewService.createReview(request);
    }

    // ADMIN
    @GetMapping("/admin")
    public List<ReviewResponse> getAllReviews(
            @RequestParam(required = false) String status
    ) {
        return reviewService.getAllReviews(status);
    }

    @PutMapping("/admin/{id}/approve")
    public ReviewResponse approveReview(@PathVariable Integer id) {
        return reviewService.approveReview(id);
    }

    @PutMapping("/admin/{id}/reject")
    public ReviewResponse rejectReview(@PathVariable Integer id) {
        return reviewService.rejectReview(id);
    }

    @DeleteMapping("/admin/{id}")
    public String deleteReview(@PathVariable Integer id) {
        reviewService.deleteReview(id);
        return "Xóa đánh giá thành công";
    }
}