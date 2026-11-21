package com.example.tour.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Tab "Đánh giá" — theo NGÀY KHÁCH GỬI đánh giá, chỉ đánh giá ĐANG HIỂN THỊ
 * (khớp những gì khách thấy trên trang tour; đánh giá admin ẩn vì spam/vi phạm không tính).
 */
@Getter
@Builder
public class ReviewReportResponse {
    private LocalDate from;
    private LocalDate to;
    private LocalDate previousFrom;
    private LocalDate previousTo;

    private long totalReviews;
    private long previousTotalReviews;
    private double averageRating;            // 0 khi chưa có đánh giá
    private double previousAverageRating;
    private double positiveRate;             // % 4–5 sao
    private double negativeRate;             // % 1–2 sao
    private long unreplied;                  // trong kỳ, chưa có phản hồi của VietTrip

    private List<StarCountResponse> distribution;            // luôn đủ 5 → 1 sao
    private int minReviews;                                  // ngưỡng để xếp hạng tour
    private List<TourReviewStatResponse> attentionTours;     // ≥ minReviews, % xấu cao → thấp (tối đa 10)
}
