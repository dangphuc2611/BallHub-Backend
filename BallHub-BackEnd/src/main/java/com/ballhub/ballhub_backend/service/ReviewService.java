package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.reponse.review.ProductReviewsResponse;
import com.ballhub.ballhub_backend.dto.reponse.review.ReviewAdminResponse;
import com.ballhub.ballhub_backend.dto.reponse.review.ReviewItem;
import com.ballhub.ballhub_backend.entity.Review;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.ProductRepository;
import com.ballhub.ballhub_backend.repository.ReviewRepository;
import com.ballhub.ballhub_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    public ProductReviewsResponse getReviewsByProduct(Integer productId) {

        List<Review> reviews = reviewRepo.findByProductId(productId);

        Double avg = reviewRepo.getAvgRating(productId);
        Long total = reviewRepo.getTotalReviews(productId);

        List<ReviewItem> items = reviews.stream().map(r -> {
            String fullName = userRepo.findById(r.getUserId())
                    .map(u -> u.getFullName())
                    .orElse("User");

            return ReviewItem.builder()
                    .reviewId(r.getReviewId())
                    .userId(r.getUserId())
                    .fullName(fullName)
                    .rating(r.getRating())
                    .comment(r.getComment())
                    .createdAt(r.getCreatedAt())
                    .build();
        }).toList();

        return ProductReviewsResponse.builder()
                .avgRating(avg == null ? 0 : Math.round(avg * 10.0) / 10.0)
                .totalReviews(total == null ? 0 : total)
                .items(items)
                .build();
    }

    public ReviewItem createReview(Integer productId, Integer userId, Integer rating, String comment) {

        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("Rating phải từ 1 đến 5");
        }

        if (reviewRepo.existsByProductIdAndUserId(productId, userId)) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi");
        }

        String safeComment = (comment == null) ? null : comment.trim();

        Review saved = reviewRepo.save(
                Review.builder()
                        .productId(productId)
                        .userId(userId)
                        .rating(rating)
                        .comment(safeComment)
                        .status(true)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        String fullName = userRepo.findById(userId)
                .map(u -> u.getFullName())
                .orElse("User");

        return ReviewItem.builder()
                .reviewId(saved.getReviewId())
                .userId(saved.getUserId())
                .fullName(fullName)
                .rating(saved.getRating())
                .comment(saved.getComment())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /* ================= ADMIN ================= */
    @Transactional(readOnly = true)
    public Page<ReviewAdminResponse> getAllReviewsAdmin(Pageable pageable) {
        return reviewRepo.findAll(pageable).map(this::mapToAdminResponse);
    }

    public void deleteReview(Integer id) {
        if (!reviewRepo.existsById(id)) {
            throw new ResourceNotFoundException("Review không tồn tại");
        }
        reviewRepo.deleteById(id);
    }

    private ReviewAdminResponse mapToAdminResponse(Review r) {
        String fullName = userRepo.findById(r.getUserId())
                .map(u -> u.getFullName())
                .orElse("User #" + r.getUserId());

        String productName = productRepo.findById(r.getProductId())
                .map(p -> p.getProductName())
                .orElse("Product #" + r.getProductId());

        return ReviewAdminResponse.builder()
                .reviewId(r.getReviewId())
                .productId(r.getProductId())
                .productName(productName)
                .userId(r.getUserId())
                .fullName(fullName)
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .status(r.getStatus())
                .build();
    }
}
