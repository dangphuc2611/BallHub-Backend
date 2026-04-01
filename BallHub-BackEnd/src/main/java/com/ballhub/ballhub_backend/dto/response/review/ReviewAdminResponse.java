package com.ballhub.ballhub_backend.dto.response.review;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewAdminResponse {
    private Integer reviewId;
    private Integer productId;
    private String productName;
    private Integer userId;
    private String fullName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private Boolean status;
}
