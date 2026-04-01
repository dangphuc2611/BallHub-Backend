package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.request.review.CreateReviewRequest;
import com.ballhub.ballhub_backend.dto.response.review.ProductReviewsResponse;
import com.ballhub.ballhub_backend.dto.response.review.ReviewItem;
import com.ballhub.ballhub_backend.security.CustomUserDetails;
import com.ballhub.ballhub_backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewService reviewService;

    /* ================= GET REVIEWS ================= */
    @GetMapping("/{id}/reviews")
    public Map<String, Object> getReviews(@PathVariable("id") Integer productId) {

        ProductReviewsResponse data = reviewService.getReviewsByProduct(productId);

        return Map.of("data", data);
    }

    /* ================= POST REVIEW ================= */
    @PostMapping("/{id}/reviews")
    public Map<String, Object> createReview(
            @PathVariable("id") Integer productId,
            @RequestBody CreateReviewRequest req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        if (user == null) {
            return Map.of("message", "Unauthorized");
        }

        ReviewItem data = reviewService.createReview(
                productId,
                user.getUserId(),
                req.getRating(),
                req.getComment()
        );

        return Map.of("data", data);
    }
}
