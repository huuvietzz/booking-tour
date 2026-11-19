package com.example.tour.report.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.report.constant.ReportGroupBy;
import com.example.tour.report.dto.response.RevenuePointResponse;
import com.example.tour.report.dto.response.RevenueReportResponse;
import com.example.tour.report.dto.response.RevenueSummaryResponse;
import com.example.tour.report.dto.response.TopTourRevenueResponse;
import com.example.tour.report.repository.RevenueReportRepository;
import com.example.tour.report.repository.RevenueReportRepository.DailyRow;
import com.example.tour.report.service.RevenueReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RevenueReportServiceImpl implements RevenueReportService {

    private static final int MAX_DAYS = 366 * 3;        // tối đa 3 năm / lần xem
    private static final int DAILY_MAX_DAYS = 62;       // ≤ 2 tháng: biểu đồ theo ngày, dài hơn: theo tháng
    private static final int TOP_TOURS = 10;
    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter VN_MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

    private final RevenueReportRepository reportRepository;

    @Override
    public RevenueReportResponse getRevenueReport(LocalDate from, LocalDate to) {
        LocalDate[] range = normalize(from, to);
        from = range[0];
        to = range[1];
        LocalDate[] prev = previousPeriod(from, to);

        List<DailyRow> rows = reportRepository.findDailyStats(from, to);
        List<DailyRow> prevRows = reportRepository.findDailyStats(prev[0], prev[1]);

        RevenueSummaryResponse summary = summarize(rows);
        ReportGroupBy groupBy = ChronoUnit.DAYS.between(from, to) + 1 <= DAILY_MAX_DAYS ? ReportGroupBy.DAY : ReportGroupBy.MONTH;

        List<TopTourRevenueResponse> topTours = reportRepository.findTopTours(from, to, TOP_TOURS);
        topTours.forEach(t -> t.setShare(percent(t.getRevenue(), summary.getRevenue())));

        return RevenueReportResponse.builder()
                .from(from).to(to)
                .previousFrom(prev[0]).previousTo(prev[1])
                .groupBy(groupBy)
                .summary(summary)
                .previous(summarize(prevRows))
                .series(buildSeries(rows, from, to, groupBy))
                .topTours(topTours)
                .build();
    }

    // =====================================================================
    // KHOẢNG THỜI GIAN
    // =====================================================================
    /** Mặc định: đầu tháng này → hôm nay. from > to hoặc dài quá 3 năm → lỗi 400 */
    private static LocalDate[] normalize(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        if (to == null) to = today;
        if (from == null) from = to.withDayOfMonth(1);
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new AppException(ErrorCode.REPORT_INVALID_RANGE);
        }
        return new LocalDate[]{from, to};
    }

    /**
     * Kỳ so sánh — chọn sao cho con số "% so với kỳ trước" có ý nghĩa:
     *   - Từ 1/1 trong cùng năm (Năm nay, cả năm)  → cùng khoảng năm trước     (1/1–19/11/2026 ↔ 1/1–19/11/2025)
     *   - Từ ngày 1 của tháng (Tháng này, Quý này)  → lùi đúng số tháng        (1–19/11 ↔ 1–19/10; 1/10–31/12 ↔ 1/7–30/9)
     *   - Còn lại (7 ngày, 30 ngày, tuỳ chỉnh)      → khoảng cùng độ dài, ngay trước from
     */
    private static LocalDate[] previousPeriod(LocalDate from, LocalDate to) {
        if (from.getDayOfYear() == 1 && from.getYear() == to.getYear()) {
            return new LocalDate[]{from.minusYears(1), to.minusYears(1)};
        }
        if (from.getDayOfMonth() == 1) {
            long months = ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to)) + 1;
            return new LocalDate[]{from.minusMonths(months), to.minusMonths(months)};
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return new LocalDate[]{from.minusDays(days), from.minusDays(1)};
    }

    // =====================================================================
    // TỔNG HỢP
    // =====================================================================
    private static RevenueSummaryResponse summarize(List<DailyRow> rows) {
        long total = 0, paid = 0, guests = 0, pending = 0, cancelled = 0, refunded = 0;
        BigDecimal revenue = BigDecimal.ZERO, pendingAmount = BigDecimal.ZERO, refundAmount = BigDecimal.ZERO;
        for (DailyRow r : rows) {
            total += r.totalBookings();
            paid += r.paidBookings();
            guests += r.guests();
            pending += r.pendingBookings();
            cancelled += r.cancelledBookings();
            refunded += r.refundedBookings();
            revenue = revenue.add(r.revenue());
            pendingAmount = pendingAmount.add(r.pendingAmount());
            refundAmount = refundAmount.add(r.refundAmount());
        }
        return RevenueSummaryResponse.builder()
                .totalBookings(total)
                .paidBookings(paid)
                .revenue(revenue)
                .guests(guests)
                .averageOrderValue(paid == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(paid), 0, RoundingMode.HALF_UP))
                .pendingBookings(pending)
                .pendingAmount(pendingAmount)
                .cancelledBookings(cancelled)
                .refundedBookings(refunded)
                .refundAmount(refundAmount)
                .cancelRate(total == 0 ? 0 : round1(cancelled * 100.0 / total))
                .build();
    }

    /** Đủ MỌI ngày / tháng trong khoảng (ngày không có đơn = 0) → biểu đồ không bị "nhảy cóc" */
    private static List<RevenuePointResponse> buildSeries(List<DailyRow> rows, LocalDate from, LocalDate to, ReportGroupBy groupBy) {
        Function<LocalDate, LocalDate> bucketOf = groupBy == ReportGroupBy.DAY ? d -> d : d -> d.withDayOfMonth(1);
        Map<LocalDate, List<DailyRow>> byBucket = rows.stream().collect(Collectors.groupingBy(r -> bucketOf.apply(r.date())));

        List<RevenuePointResponse> series = new ArrayList<>();
        for (LocalDate d = bucketOf.apply(from); !d.isAfter(to);
             d = groupBy == ReportGroupBy.DAY ? d.plusDays(1) : d.plusMonths(1)) {
            RevenueSummaryResponse s = summarize(byBucket.getOrDefault(d, List.of()));
            series.add(RevenuePointResponse.builder()
                    .date(d)
                    .totalBookings(s.getTotalBookings())
                    .paidBookings(s.getPaidBookings())
                    .revenue(s.getRevenue())
                    .refundAmount(s.getRefundAmount())
                    .build());
        }
        return series;
    }

    private static double percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) return 0;
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP).doubleValue();
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    // =====================================================================
    // XUẤT EXCEL (EasyExcel, head + data dạng List → không cần class @ExcelProperty)
    // Tiền ghi dạng SỐ (không phải chuỗi "1.000.000 đ") → mở Excel cộng / lọc / vẽ biểu đồ được
    // =====================================================================
    @Override
    public byte[] exportRevenueReport(LocalDate from, LocalDate to) {
        RevenueReportResponse r = getRevenueReport(from, to);
        RevenueSummaryResponse s = r.getSummary();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (ExcelWriter writer = EasyExcel.write(out)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .build()) {

            // Sheet 1: Tổng quan
            List<List<Object>> overview = new ArrayList<>();
            overview.add(row("Từ ngày", r.getFrom().format(VN_DATE)));
            overview.add(row("Đến ngày", r.getTo().format(VN_DATE)));
            overview.add(row("Doanh thu (VNĐ)", s.getRevenue()));
            overview.add(row("Tổng số đơn đặt", s.getTotalBookings()));
            overview.add(row("Đơn đã thanh toán", s.getPaidBookings()));
            overview.add(row("Số khách (đơn đã thanh toán)", s.getGuests()));
            overview.add(row("Giá trị trung bình / đơn (VNĐ)", s.getAverageOrderValue()));
            overview.add(row("Đơn chờ thanh toán", s.getPendingBookings()));
            overview.add(row("Tiền chờ thanh toán (VNĐ)", s.getPendingAmount()));
            overview.add(row("Đơn đã huỷ", s.getCancelledBookings()));
            overview.add(row("Tỷ lệ huỷ (%)", s.getCancelRate()));
            overview.add(row("Đơn hoàn tiền", s.getRefundedBookings()));
            overview.add(row("Tiền hoàn (VNĐ)", s.getRefundAmount()));
            overview.add(row("Ghi chú", "Tính theo ngày đặt. Doanh thu chỉ gồm đơn đã thanh toán, không gồm đơn đã hoàn tiền."));
            writer.write(overview, sheet(0, "Tổng quan", "Chỉ số", "Giá trị"));

            // Sheet 2: Theo ngày / tháng
            boolean byDay = r.getGroupBy() == ReportGroupBy.DAY;
            List<List<Object>> series = new ArrayList<>();
            for (RevenuePointResponse p : r.getSeries()) {
                series.add(row(p.getDate().format(byDay ? VN_DATE : VN_MONTH),
                        p.getTotalBookings(), p.getPaidBookings(), p.getRevenue(), p.getRefundAmount()));
            }
            writer.write(series, sheet(1, byDay ? "Theo ngày" : "Theo tháng",
                    byDay ? "Ngày" : "Tháng", "Tổng đơn", "Đơn đã thanh toán", "Doanh thu (VNĐ)", "Hoàn tiền (VNĐ)"));

            // Sheet 3: Top tour
            List<List<Object>> tours = new ArrayList<>();
            int i = 1;
            for (TopTourRevenueResponse t : r.getTopTours()) {
                tours.add(row(i++, t.getTourCode(), t.getTourName(), t.getBookings(), t.getGuests(), t.getRevenue(), t.getShare()));
            }
            writer.write(tours, sheet(2, "Top tour",
                    "STT", "Mã tour", "Tên tour", "Lượt đặt", "Số khách", "Doanh thu (VNĐ)", "Tỷ trọng (%)"));
        }
        return out.toByteArray();
    }

    private static WriteSheet sheet(int no, String name, String... headers) {
        List<List<String>> head = Arrays.stream(headers).map(h -> List.of(h)).toList();
        return EasyExcel.writerSheet(no, name).head(head).build();
    }

    private static List<Object> row(Object... cells) {
        return new ArrayList<>(Arrays.asList(cells));
    }
}
