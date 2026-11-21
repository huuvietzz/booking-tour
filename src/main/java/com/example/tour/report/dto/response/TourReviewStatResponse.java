package com.example.tour.report.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Đánh giá của 1 tour trong kỳ */
@Getter
@Builder
public class TourReviewStatResponse {
    private Integer tourId;
    private String tourCode;
    private String tourName;
    private long reviews;
    private double averageRating;   // 1 chữ số thập phân
    private long negativeCount;     // 1–2 sao
    private double negativeRate;    // %
    private double positiveRate;    // % 4–5 sao
}
