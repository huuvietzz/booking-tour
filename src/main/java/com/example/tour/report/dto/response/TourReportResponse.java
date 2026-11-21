package com.example.tour.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** Tab "Tour & điểm đến" — 1 request đủ vẽ cả tab */
@Getter
@Builder
public class TourReportResponse {
    private LocalDate from;
    private LocalDate to;

    // Lấp đầy — theo NGÀY KHỞI HÀNH
    private OccupancySummaryResponse occupancy;
    private List<TourOccupancyResponse> tourOccupancy;              // mọi tour có lịch trong khoảng, lấp đầy thấp → cao
    private List<ScheduleOccupancyResponse> lowUpcomingSchedules;   // lịch CHƯA khởi hành lấp đầy thấp nhất (tối đa 10)

    // Doanh thu — theo NGÀY ĐẶT, đơn đã thanh toán (khớp tab Tổng quan)
    private List<BreakdownItemResponse> byLocation;
    private List<BreakdownItemResponse> byCategory;
    private List<BreakdownItemResponse> byTourClass;
}
