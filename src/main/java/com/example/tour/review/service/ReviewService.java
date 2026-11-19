package com.example.tour.review.service;

import com.example.tour.review.dto.request.ReviewFilterRequest;
import com.example.tour.review.dto.request.ReviewRequest;
import com.example.tour.review.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {

    // ---------- khách (userId lấy từ token) ----------
    ReviewResponse createMyReview(Integer userId, Integer bookingId, ReviewRequest request);

    ReviewResponse updateMyReview(Integer userId, Integer bookingId, ReviewRequest request);

    // ---------- admin ----------
    Page<ReviewAdminRowResponse> getReviewsForAdmin(ReviewFilterRequest filter, int page, int size, String sort);

    ReviewAdminDetailResponse getReviewDetail(Integer reviewId);

    ReviewAdminDetailResponse hideReview(Integer reviewId, String reason);

    ReviewAdminDetailResponse unhideReview(Integer reviewId);

    ReviewAdminDetailResponse replyReview(Integer reviewId, String adminResponse);

    List<TourRatingResponse> getTourRatings();

    // ---------- qua link email (khách vãng lai / mọi khách), xác thực bằng token của đơn ----------
    GuestReviewContextResponse getReviewContextByToken(String bookingCode, String token);

    ReviewResponse saveReviewByToken(String bookingCode, String token, ReviewRequest request);

    // ---------- public ----------
    Page<PublicReviewResponse> getTourReviews(Integer tourId, Integer rating, Pageable pageable);

    TourReviewSummaryResponse getTourSummary(Integer tourId);
}
