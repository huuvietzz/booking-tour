package com.example.tour.review.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/** Điểm tổng hợp của 1 tour: 4.6 ★ (12 đánh giá) + số lượt từng mức sao */
@Getter
@Builder
public class TourReviewSummaryResponse {
    private double averageRating;
    private long totalReviews;
    /** {5: 8, 4: 3, 3: 1, 2: 0, 1: 0} */
    private Map<Integer, Long> ratingCounts;
}
