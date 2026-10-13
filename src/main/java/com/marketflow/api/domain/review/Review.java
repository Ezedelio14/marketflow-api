package com.marketflow.api.domain.review;

import com.marketflow.api.domain.common.BaseEntity;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "order_item_id", nullable = false, unique = true)
    private UUID orderItemId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "title")
    private String title;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReviewStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Review write(UUID productId, UUID userId, UUID orderItemId, int rating, String title, String comment) {
        if (rating < 1 || rating > 5) {
            throw new BusinessRuleException("INVALID_RATING", "Rating must be between 1 and 5");
        }
        Review review = new Review();
        review.productId = productId;
        review.userId = userId;
        review.orderItemId = orderItemId;
        review.rating = rating;
        review.title = title;
        review.comment = comment;
        review.status = ReviewStatus.PENDING;
        return review;
    }

    public void approve() {
        status = ReviewStatus.APPROVED;
    }

    public void reject() {
        status = ReviewStatus.REJECTED;
    }
}
