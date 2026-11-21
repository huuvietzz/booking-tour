package com.example.tour.report.service.impl;

import com.example.tour.report.dto.response.ReviewReportResponse;
import com.example.tour.report.dto.response.StarCountResponse;
import com.example.tour.report.dto.response.TourReviewStatResponse;
import com.example.tour.report.repository.ReviewReportRepository;
import com.example.tour.report.service.ReviewReportService;
import com.example.tour.report.util.ReportRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewReportServiceImpl implements ReviewReportService {

    /**
     * Tour cần ít nhất 3 đánh giá trong kỳ mới được xếp hạng:
     * 1 đánh giá 1 sao duy nhất = "100% xấu" → không công bằng, không đủ để kết luận.
     */
    private static final int MIN_REVIEWS = 3;
    private static final int ATTENTION_LIMIT = 10;

    private final ReviewReportRepository reviewReportRepository;

    @Override
    public ReviewReportResponse getReviewReport(LocalDate fromDate, LocalDate toDate) {
        ReportRange range = ReportRange.of(fromDate, toDate);
        ReportRange prev = range.previous();

        Map<Integer, Long> stars = reviewReportRepository.countByStar(range.start(), range.end());
        Map<Integer, Long> prevStars = reviewReportRepository.countByStar(prev.start(), prev.end());
        long total = sum(stars);

        List<StarCountResponse> distribution = new ArrayList<>();
        for (int star = 5; star >= 1; star--) {
            long c = stars.getOrDefault(star, 0L);
            distribution.add(StarCountResponse.builder().star(star).count(c).percent(rate(c, total)).build());
        }
        long positive = stars.getOrDefault(5, 0L) + stars.getOrDefault(4, 0L);
        long negative = stars.getOrDefault(2, 0L) + stars.getOrDefault(1, 0L);

        // Tour cần chú ý: % 1–2 sao cao nhất, bằng nhau thì điểm TB thấp hơn lên trước
        List<TourReviewStatResponse> attention = reviewReportRepository
                .statsByTour(range.start(), range.end(), MIN_REVIEWS).stream()
                .map(r -> {
                    long n = ((Number) r[3]).longValue();
                    long neg = r[5] == null ? 0 : ((Number) r[5]).longValue();
                    long pos = r[6] == null ? 0 : ((Number) r[6]).longValue();
                    return TourReviewStatResponse.builder()
                            .tourId((Integer) r[0]).tourCode((String) r[1]).tourName((String) r[2])
                            .reviews(n)
                            .averageRating(round1(((Number) r[4]).doubleValue()))
                            .negativeCount(neg)
                            .negativeRate(rate(neg, n))
                            .positiveRate(rate(pos, n))
                            .build();
                })
                .sorted(Comparator.comparingDouble(TourReviewStatResponse::getNegativeRate).reversed()
                        .thenComparingDouble(TourReviewStatResponse::getAverageRating))
                .limit(ATTENTION_LIMIT)
                .toList();

        return ReviewReportResponse.builder()
                .from(range.from()).to(range.to())
                .previousFrom(prev.from()).previousTo(prev.to())
                .totalReviews(total)
                .previousTotalReviews(sum(prevStars))
                .averageRating(average(stars))
                .previousAverageRating(average(prevStars))
                .positiveRate(rate(positive, total))
                .negativeRate(rate(negative, total))
                .unreplied(reviewReportRepository.countUnreplied(range.start(), range.end()))
                .distribution(distribution)
                .minReviews(MIN_REVIEWS)
                .attentionTours(attention)
                .build();
    }

    private static long sum(Map<Integer, Long> stars) {
        return stars.values().stream().mapToLong(Long::longValue).sum();
    }

    private static double average(Map<Integer, Long> stars) {
        long n = sum(stars);
        if (n == 0) return 0;
        long points = stars.entrySet().stream().mapToLong(e -> e.getKey() * e.getValue()).sum();
        return round1((double) points / n);
    }

    private static double rate(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 1000.0 / whole) / 10.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
