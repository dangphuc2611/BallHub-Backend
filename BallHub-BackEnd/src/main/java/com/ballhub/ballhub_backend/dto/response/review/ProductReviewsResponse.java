package com.ballhub.ballhub_backend.dto.response.review;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReviewsResponse {
    private double avgRating;
    private long totalReviews;
    private List<ReviewItem> items;
}
