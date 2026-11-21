package com.example.tour.report.util;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.report.constant.ReportGroupBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/** Khoảng thời gian dùng chung cho các báo cáo: [from 00:00, to+1 00:00) */
public record ReportRange(LocalDate from, LocalDate to) {

    private static final int MAX_DAYS = 366 * 3;
    private static final int DAILY_MAX_DAYS = 62;

    /** Bỏ trống → đầu tháng này đến hôm nay. from > to hoặc quá 3 năm → 400 */
    public static ReportRange of(LocalDate from, LocalDate to) {
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.withDayOfMonth(1);
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new AppException(ErrorCode.REPORT_INVALID_RANGE);
        }
        return new ReportRange(from, to);
    }

    public LocalDateTime start() { return from.atStartOfDay(); }
    public LocalDateTime end()   { return to.plusDays(1).atStartOfDay(); }

    /** ≤ 62 ngày: theo ngày, dài hơn: theo tháng */
    public ReportGroupBy groupBy() {
        return ChronoUnit.DAYS.between(from, to) + 1 <= DAILY_MAX_DAYS ? ReportGroupBy.DAY : ReportGroupBy.MONTH;
    }

    /** Ngày đại diện của cột chứa ngày d (theo tháng → ngày 1) */
    public LocalDate bucketOf(LocalDate d) {
        return groupBy() == ReportGroupBy.DAY ? d : d.withDayOfMonth(1);
    }

    /**
     * Kỳ so sánh (giống tab Tổng quan): từ 1/1 cùng năm → cùng khoảng năm trước; từ ngày 1 của tháng → lùi đúng số tháng;
     * còn lại → khoảng cùng độ dài ngay trước from.
     */
    public ReportRange previous() {
        if (from.getDayOfYear() == 1 && from.getYear() == to.getYear()) {
            return new ReportRange(from.minusYears(1), to.minusYears(1));
        }
        if (from.getDayOfMonth() == 1) {
            long months = ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to)) + 1;
            return new ReportRange(from.minusMonths(months), to.minusMonths(months));
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return new ReportRange(from.minusDays(days), from.minusDays(1));
    }

    public LocalDate nextBucket(LocalDate d) {
        return groupBy() == ReportGroupBy.DAY ? d.plusDays(1) : d.plusMonths(1);
    }
}
