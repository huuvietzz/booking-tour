package com.example.tour.report.service.impl;

import com.example.tour.report.dto.response.*;
import com.example.tour.report.repository.TourReportRepository;
import com.example.tour.report.repository.TourReportRepository.BreakdownRow;
import com.example.tour.report.service.TourReportService;
import com.example.tour.report.util.ReportRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourReportServiceImpl implements TourReportService {

    private static final double LOW_RATE = 50;     // dưới 50% chỗ → "lấp đầy thấp"
    private static final int LOW_UPCOMING_LIMIT = 10;

    private final TourReportRepository tourReportRepository;

    @Override
    public TourReportResponse getTourReport(LocalDate fromDate, LocalDate toDate) {
        ReportRange range = ReportRange.of(fromDate, toDate);

        // ===== Lấp đầy: 1 câu lấy mọi lịch trong khoảng, tổng hợp trong Java =====
        List<ScheduleOccupancyResponse> schedules = tourReportRepository.findSchedules(range.from(), range.to());
        schedules.forEach(s -> s.setOccupancyRate(rate(s.getBookedSlots(), s.getTotalSlots())));

        long totalSlots = schedules.stream().mapToLong(ScheduleOccupancyResponse::getTotalSlots).sum();
        long bookedSlots = schedules.stream().mapToLong(ScheduleOccupancyResponse::getBookedSlots).sum();
        OccupancySummaryResponse occupancy = OccupancySummaryResponse.builder()
                .schedules(schedules.size())
                .totalSlots(totalSlots)
                .bookedSlots(bookedSlots)
                .occupancyRate(rate(bookedSlots, totalSlots))
                .fullSchedules(schedules.stream().filter(s -> s.getTotalSlots() > 0 && s.getBookedSlots() >= s.getTotalSlots()).count())
                .lowSchedules(schedules.stream().filter(s -> s.getOccupancyRate() < LOW_RATE).count())
                .build();

        // Gộp theo tour, giữ thứ tự xuất hiện rồi sắp lấp đầy thấp → cao (tour cần đẩy bán lên đầu)
        Map<Integer, List<ScheduleOccupancyResponse>> byTour = schedules.stream()
                .collect(Collectors.groupingBy(ScheduleOccupancyResponse::getTourId, LinkedHashMap::new, Collectors.toList()));
        List<TourOccupancyResponse> tourOccupancy = byTour.values().stream().map(list -> {
            ScheduleOccupancyResponse first = list.get(0);
            long total = list.stream().mapToLong(ScheduleOccupancyResponse::getTotalSlots).sum();
            long booked = list.stream().mapToLong(ScheduleOccupancyResponse::getBookedSlots).sum();
            return TourOccupancyResponse.builder()
                    .tourId(first.getTourId()).tourCode(first.getTourCode()).tourName(first.getTourName())
                    .schedules(list.size()).totalSlots(total).bookedSlots(booked)
                    .occupancyRate(rate(booked, total))
                    .build();
        }).sorted(Comparator.comparingDouble(TourOccupancyResponse::getOccupancyRate)).toList();

        // Lịch CHƯA khởi hành, ít khách nhất → còn kịp đẩy khuyến mãi
        LocalDate today = LocalDate.now();
        List<ScheduleOccupancyResponse> lowUpcoming = schedules.stream()
                .filter(s -> s.getDepartureDate().isAfter(today))
                .sorted(Comparator.comparingDouble(ScheduleOccupancyResponse::getOccupancyRate)
                        .thenComparing(ScheduleOccupancyResponse::getDepartureDate))
                .limit(LOW_UPCOMING_LIMIT)
                .toList();

        // ===== Doanh thu theo nhóm (ngày đặt, đơn đã thanh toán) =====
        return TourReportResponse.builder()
                .from(range.from()).to(range.to())
                .occupancy(occupancy)
                .tourOccupancy(tourOccupancy)
                .lowUpcomingSchedules(lowUpcoming)
                .byLocation(toItems(tourReportRepository.revenueByLocation(range.start(), range.end())))
                .byCategory(toItems(tourReportRepository.revenueByCategory(range.start(), range.end())))
                .byTourClass(toItems(tourReportRepository.revenueByTourClass(range.start(), range.end())))
                .build();
    }

    private static List<BreakdownItemResponse> toItems(List<BreakdownRow> rows) {
        BigDecimal total = rows.stream().map(BreakdownRow::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return rows.stream().map(r -> BreakdownItemResponse.builder()
                .key(r.key()).label(r.label())
                .bookings(r.bookings()).guests(r.guests()).revenue(r.revenue())
                .share(total.signum() == 0 ? 0 : r.revenue().multiply(BigDecimal.valueOf(100))
                        .divide(total, 1, RoundingMode.HALF_UP).doubleValue())
                .build()).toList();
    }

    private static double rate(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 1000.0 / whole) / 10.0;
    }
}
