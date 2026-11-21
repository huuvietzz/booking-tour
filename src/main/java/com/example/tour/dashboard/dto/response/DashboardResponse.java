package com.example.tour.dashboard.dto.response;

import com.example.tour.dashboard.constant.DashboardPeriod;
import com.example.tour.report.dto.response.RevenueReportResponse;
import com.example.tour.report.dto.response.ScheduleOccupancyResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bảng điều khiển: 1 request vẽ cả trang.
 * Phần theo khoảng thời gian dùng lại đúng service của trang Báo cáo → số liệu 2 trang luôn khớp nhau.
 */
@Getter
@Builder
public class DashboardResponse {
    private DashboardPeriod period;
    private LocalDateTime generatedAt;

    // Theo khoảng (7 ngày / 30 ngày / 12 tháng)
    private RevenueReportResponse revenue;              // summary, previous, series, topTours (= tab Tổng quan)
    private DashboardRatingResponse rating;
    private List<DestinationStatResponse> topDestinations;

    // Ngay lúc này
    private DashboardTodoResponse todo;
    private List<RecentBookingResponse> recentBookings;
    private List<ScheduleOccupancyResponse> upcomingSchedules;
}
