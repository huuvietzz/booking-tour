package com.example.tour.review.dto.response;

import lombok.Getter;

/** Điểm trung bình + số đánh giá (đang hiển thị) của 1 tour — dùng cho ô lọc "Tour" ở trang admin */
@Getter
public class TourRatingResponse {
    private final Integer tourId;
    private final String tourName;
    private final double averageRating;   // làm tròn 1 chữ số
    private final long totalReviews;

    /** JPQL "select new ..." gọi constructor này: avg() trả Double, count() trả Long */
    public TourRatingResponse(Integer tourId, String tourName, Double averageRating, Long totalReviews) {
        this.tourId = tourId;
        this.tourName = tourName;
        this.averageRating = averageRating == null ? 0 : Math.round(averageRating * 10) / 10.0;
        this.totalReviews = totalReviews == null ? 0 : totalReviews;
    }
}
