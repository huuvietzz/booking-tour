package com.example.tour.review.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.review.dto.response.PublicReviewResponse;
import com.example.tour.review.dto.response.TourReviewSummaryResponse;
import com.example.tour.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Trang chi tiết tour: điểm tổng hợp + danh sách đánh giá đang hiển thị */
@RestController
@RequestMapping("/api/public/tours/{tourId}/reviews")
@RequiredArgsConstructor
public class PublicReviewController {

    private final ReviewService reviewService;

    /** GET /api/public/tours/5/reviews?page=0&size=5&rating=5 */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PublicReviewResponse>>> list(
            @PathVariable Integer tourId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(ApiResponse.success("Lấy đánh giá thành công",
                reviewService.getTourReviews(tourId, rating, PageRequest.of(Math.max(page, 0), Math.max(size, 1)))));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TourReviewSummaryResponse>> summary(@PathVariable Integer tourId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy điểm đánh giá thành công", reviewService.getTourSummary(tourId)));
    }
}
