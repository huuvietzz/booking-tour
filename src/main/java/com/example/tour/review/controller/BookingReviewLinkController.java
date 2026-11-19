package com.example.tour.review.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.review.dto.request.ReviewRequest;
import com.example.tour.review.dto.response.GuestReviewContextResponse;
import com.example.tour.review.dto.response.ReviewResponse;
import com.example.tour.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Đánh giá qua link trong email "Mời đánh giá" — không cần đăng nhập (khách vãng lai).
 * Xác thực bằng token bí mật của đơn (paymentToken) trong link: user/review.html?code=BK...&token=...
 */
@RestController
@RequestMapping("/api/public/bookings/{code}/review")
@RequiredArgsConstructor
public class BookingReviewLinkController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<GuestReviewContextResponse>> get(@PathVariable String code,
                                                                       @RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chuyến đi thành công",
                reviewService.getReviewContextByToken(code, token)));
    }

    /** Chưa đánh giá → tạo, có rồi → sửa */
    @PutMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> save(@PathVariable String code,
                                                            @RequestParam String token,
                                                            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cảm ơn Quý khách đã đánh giá",
                reviewService.saveReviewByToken(code, token, request)));
    }
}
