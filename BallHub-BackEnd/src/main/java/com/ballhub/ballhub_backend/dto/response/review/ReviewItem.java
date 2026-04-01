package com.ballhub.ballhub_backend.dto.response.review;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewItem {
    private Integer reviewId;
    private Integer userId;
    private String fullName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
