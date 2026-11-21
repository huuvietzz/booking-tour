package com.example.tour.dashboard.service.impl;

import com.example.tour.dashboard.constant.DashboardPeriod;
import com.example.tour.dashboard.dto.response.*;
import com.example.tour.dashboard.repository.DashboardRepository;
import com.example.tour.dashboard.service.DashboardService;
import com.example.tour.report.dto.response.RevenueReportResponse;
import com.example.tour.report.dto.response.ReviewReportResponse;
import com.example.tour.report.dto.response.ScheduleOccupancyResponse;
import com.example.tour.report.repository.TourReportRepository;
import com.example.tour.report.service.RevenueReportService;
import com.example.tour.report.service.ReviewReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final int RECENT_LIMIT = 6;
    private static final int UPCOMING_LIMIT = 5;
    private static final int DESTINATION_LIMIT = 6;
    private static final int LOW_UPCOMING_DAYS = 7;

    private final RevenueReportService revenueReportService;
    private final ReviewReportService reviewReportService;
    private final TourReportRepository tourReportRepository;
    private final DashboardRepository dashboardRepository;

    @Override
    public DashboardResponse getDashboard(DashboardPeriod period) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.fromDate(today);

        // Dùng lại báo cáo: số liệu Bảng điều khiển = trang Báo cáo cùng khoảng thời gian
        RevenueReportResponse revenue = revenueReportService.getRevenueReport(from, today);
        ReviewReportResponse reviews = reviewReportService.getReviewReport(from, today);

        // Điểm đến: xếp theo SỐ KHÁCH (thẻ "được đặt nhiều"), không theo doanh thu
        List<DestinationStatResponse> destinations = tourReportRepository
                .revenueByLocation(from.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                .map(r -> DestinationStatResponse.builder()
                        .name(r.label()).guests(r.guests()).bookings(r.bookings()).revenue(r.revenue()).build())
                .sorted(Comparator.comparingLong(DestinationStatResponse::getGuests).reversed())
                .limit(DESTINATION_LIMIT)
                .toList();

        List<ScheduleOccupancyResponse> upcoming = dashboardRepository.findUpcomingSchedules(today, UPCOMING_LIMIT);
        upcoming.forEach(s -> s.setOccupancyRate(s.getTotalSlots() == 0 ? 0
                : Math.round(s.getBookedSlots() * 1000.0 / s.getTotalSlots()) / 10.0));

        return DashboardResponse.builder()
                .period(period)
                .generatedAt(LocalDateTime.now())
                .revenue(revenue)
                .rating(DashboardRatingResponse.builder()
                        .totalReviews(reviews.getTotalReviews())
                        .averageRating(reviews.getAverageRating())
                        .previousTotalReviews(reviews.getPreviousTotalReviews())
                        .previousAverageRating(reviews.getPreviousAverageRating())
                        .build())
                .topDestinations(destinations)
                .todo(DashboardTodoResponse.builder()
                        .awaitingPayment(dashboardRepository.countAwaitingPayment())
                        .toComplete(dashboardRepository.countToComplete(today))
                        .unrepliedReviews(dashboardRepository.countUnrepliedReviews())
                        .lowUpcoming(dashboardRepository.countLowSchedules(today.plusDays(1), today.plusDays(LOW_UPCOMING_DAYS)))
                        .build())
                .recentBookings(dashboardRepository.findRecentBookings(RECENT_LIMIT))
                .upcomingSchedules(upcoming)
                .build();
    }
}
