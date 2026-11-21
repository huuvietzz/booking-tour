package com.example.tour.dashboard.constant;

import java.time.LocalDate;

/** Nút "7 ngày / 30 ngày / 12 tháng" trên Bảng điều khiển — luôn tính đến HÔM NAY */
public enum DashboardPeriod {
    WEEK, MONTH, YEAR;

    public LocalDate fromDate(LocalDate today) {
        return switch (this) {
            case WEEK -> today.minusDays(6);
            case MONTH -> today.minusDays(29);
            // 12 tháng gần nhất tính trọn tháng: 01/12/2025 → 20/11/2026 (biểu đồ 12 cột tháng)
            case YEAR -> today.withDayOfMonth(1).minusMonths(11);
        };
    }
}
