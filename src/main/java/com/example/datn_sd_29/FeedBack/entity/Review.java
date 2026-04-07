package com.example.datn_sd_29.FeedBack.entity;

import com.example.datn_sd_29.FeedBack.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "visit_type", length = 100)
    private String visitType;

    @Column(name = "tip", length = 1000)
    private String tip;

    @Column(name = "service_score")
    private Integer serviceScore;

    @Column(name = "food_score")
    private Integer foodScore;

    @Column(name = "value_score")
    private Integer valueScore;

    @Column(name = "atmosphere_score")
    private Integer atmosphereScore;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ReviewStatus status;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReviewStatus.PENDING;
        }
    }
}