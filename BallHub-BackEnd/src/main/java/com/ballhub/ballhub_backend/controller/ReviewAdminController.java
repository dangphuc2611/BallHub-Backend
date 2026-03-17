package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.reponse.ApiResponse;
import com.ballhub.ballhub_backend.dto.reponse.PageResponse;
import com.ballhub.ballhub_backend.dto.reponse.review.ReviewAdminResponse;
import com.ballhub.ballhub_backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReviewAdminController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewAdminResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewAdminResponse> reviews = reviewService.getAllReviewsAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(reviews)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteReview(@PathVariable Integer id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công", null));
    }
}
