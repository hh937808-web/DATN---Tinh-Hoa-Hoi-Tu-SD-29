package com.example.datn_sd_29.FeedBack.service;

import com.example.datn_sd_29.FeedBack.dto.ReviewRequest;
import com.example.datn_sd_29.FeedBack.dto.ReviewResponse;
import com.example.datn_sd_29.FeedBack.entity.Review;
import com.example.datn_sd_29.FeedBack.enums.ReviewStatus;
import com.example.datn_sd_29.FeedBack.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    // khách chỉ xem review đã duyệt
    public List<ReviewResponse> getApprovedReviews() {
        return reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.APPROVED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReviewResponse> getAllReviews(String status) {
        List<Review> reviews;

        if (status == null || status.isBlank()) {
            reviews = reviewRepository.findAllByOrderByCreatedAtDesc();
        } else {
            ReviewStatus reviewStatus = ReviewStatus.valueOf(status.toUpperCase());
            reviews = reviewRepository.findByStatusOrderByCreatedAtDesc(reviewStatus);
        }

        return reviews.stream()
                .map(this::toResponse)
                .toList();
    }
    // khách gửi đánh giá
    public ReviewResponse createReview(ReviewRequest request) {
        Review review = Review.builder()
                .name(request.getName().trim())
                .rating(request.getRating())
                .content(request.getContent().trim())
                .visitType(request.getVisitType())
                .tip(request.getTip())
                .serviceScore(request.getServiceScore())
                .foodScore(request.getFoodScore())
                .valueScore(request.getValueScore())
                .atmosphereScore(request.getAtmosphereScore())
                .avatarUrl(request.getAvatarUrl())
                .status(ReviewStatus.PENDING)
                .build();

        return toResponse(reviewRepository.save(review));
    }

    // admin duyệt
    public ReviewResponse approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        review.setStatus(ReviewStatus.APPROVED);
        return toResponse(reviewRepository.save(review));
    }

    // admin từ chối
    public ReviewResponse rejectReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        review.setStatus(ReviewStatus.REJECTED);
        return toResponse(reviewRepository.save(review));
    }

    // admin xóa
    public void deleteReview(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy đánh giá");
        }
        reviewRepository.deleteById(id);
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .name(review.getName())
                .rating(review.getRating())
                .content(review.getContent())
                .visitType(review.getVisitType())
                .tip(review.getTip())
                .serviceScore(review.getServiceScore())
                .foodScore(review.getFoodScore())
                .valueScore(review.getValueScore())
                .atmosphereScore(review.getAtmosphereScore())
                .avatarUrl(review.getAvatarUrl())
                .createdAt(review.getCreatedAt())
                .status(review.getStatus().name())
                .statusLabel(review.getStatus().getLabel())
                .build();
    }
}