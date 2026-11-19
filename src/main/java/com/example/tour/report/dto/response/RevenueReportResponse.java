package com.example.tour.report.dto.response;

import com.example.tour.report.constant.ReportGroupBy;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** Toàn bộ dữ liệu trang Báo cáo doanh thu — 1 request là đủ vẽ cả trang */
@Getter
@Builder
public class RevenueReportResponse {
    private LocalDate from;
    private LocalDate to;
    private LocalDate previousFrom;            // kỳ trước, cùng số ngày, ngay trước from → để tính % tăng/giảm
    private LocalDate previousTo;
    private ReportGroupBy groupBy;
    private RevenueSummaryResponse summary;
    private RevenueSummaryResponse previous;
    private List<RevenuePointResponse> series; // đủ mọi ngày / tháng trong khoảng, kể cả ngày = 0
    private List<TopTourRevenueResponse> topTours;
}
