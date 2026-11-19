package com.example.tour.review.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.review.dto.request.ReviewFilterRequest;
import com.example.tour.review.dto.request.ReviewHideRequest;
import com.example.tour.review.dto.request.ReviewReplyRequest;
import com.example.tour.review.dto.response.ReviewAdminDetailResponse;
import com.example.tour.review.dto.response.ReviewAdminRowResponse;
import com.example.tour.review.dto.response.TourRatingResponse;
import com.example.tour.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin / nhân viên: xem, lọc, ẩn / hiện lại, phản hồi. Không xoá — đánh giá là dữ liệu của khách */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    /** GET /api/admin/reviews?page=0&size=10&sort=createdAt,desc&keyword=&rating=&hidden=&replied=&fromDate=&toDate= */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReviewAdminRowResponse>>> list(
            @ModelAttribute ReviewFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công",
                reviewService.getReviewsForAdmin(filter, page, size, sort)));
    }

    /** Điểm TB từng tour (cho ô lọc "Tour"), điểm thấp lên đầu */
    @GetMapping("/tour-ratings")
    public ResponseEntity<ApiResponse<List<TourRatingResponse>>> tourRatings() {
        return ResponseEntity.ok(ApiResponse.success("Lấy điểm đánh giá theo tour thành công", reviewService.getTourRatings()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewAdminDetailResponse>> detail(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đánh giá thành công", reviewService.getReviewDetail(id)));
    }

    @PatchMapping("/{id}/hide")
    public ResponseEntity<ApiResponse<ReviewAdminDetailResponse>> hide(@PathVariable Integer id,
                                                                      @Valid @RequestBody ReviewHideRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã ẩn đánh giá",
                reviewService.hideReview(id, request.getHiddenReason())));
    }

    @PatchMapping("/{id}/unhide")
    public ResponseEntity<ApiResponse<ReviewAdminDetailResponse>> unhide(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Đã hiện lại đánh giá", reviewService.unhideReview(id)));
    }

    @PutMapping("/{id}/response")
    public ResponseEntity<ApiResponse<ReviewAdminDetailResponse>> reply(@PathVariable Integer id,
                                                                       @Valid @RequestBody ReviewReplyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã lưu phản hồi",
                reviewService.replyReview(id, request.getAdminResponse())));
    }
}
