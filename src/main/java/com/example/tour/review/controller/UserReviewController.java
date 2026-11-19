package com.example.tour.review.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.common.util.SecurityUtils;
import com.example.tour.review.dto.request.ReviewRequest;
import com.example.tour.review.dto.response.ReviewResponse;
import com.example.tour.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Khách viết / sửa đánh giá cho đơn ĐÃ HOÀN THÀNH của mình. userId luôn lấy từ token */
@RestController
@RequestMapping("/api/users/bookings/my/{bookingId}/review")
@RequiredArgsConstructor
public class UserReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@PathVariable Integer bookingId,
                                                              @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Cảm ơn bạn đã đánh giá",
                reviewService.createMyReview(SecurityUtils.getCurrentUserId(), bookingId, request)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> update(@PathVariable Integer bookingId,
                                                              @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật đánh giá",
                reviewService.updateMyReview(SecurityUtils.getCurrentUserId(), bookingId, request)));
    }
}
