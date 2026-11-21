package com.example.tour.report.repository;

import com.example.tour.report.dto.response.ScheduleOccupancyResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TourReportRepository {

    private static final String PAID = "b.paymentStatus = com.example.tour.booking.constant.PaymentStatus.PAID";
    private static final String GUESTS = "(b.totalAdults + coalesce(b.totalChildren, 0) + coalesce(b.totalToddlers, 0))";

    private final EntityManager em;

    /** Lịch khởi hành (trừ lịch đã huỷ) có ngày khởi hành trong [from, to] */
    public List<ScheduleOccupancyResponse> findSchedules(LocalDate from, LocalDate to) {
        return em.createQuery("""
                        select new com.example.tour.report.dto.response.ScheduleOccupancyResponse(
                               s.scheduleId, s.scheduleCode, t.tourId, t.tourCode, t.tourName,
                               s.departureDate, s.totalSlots, s.availableSlots)
                        from Schedule s join s.tour t
                        where s.departureDate >= :from and s.departureDate <= :to
                          and s.status <> com.example.tour.tour.constant.ScheduleStatus.CANCELLED
                        order by s.departureDate
                        """, ScheduleOccupancyResponse.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    /** Doanh thu đơn đã thanh toán theo điểm đến: [id, tên, số đơn, số khách, doanh thu] */
    public List<BreakdownRow> revenueByLocation(LocalDateTime start, LocalDateTime end) {
        return breakdown("l.locationId", "l.locationName", "join t.location l", start, end);
    }

    public List<BreakdownRow> revenueByCategory(LocalDateTime start, LocalDateTime end) {
        return breakdown("c.categoryId", "c.categoryName", "join t.category c", start, end);
    }

    /** Dòng tour là enum → String.valueOf(enum) = tên enum (PREMIUM...), FE dịch sang tiếng Việt */
    public List<BreakdownRow> revenueByTourClass(LocalDateTime start, LocalDateTime end) {
        return breakdown("t.tourClass", "t.tourClass", "", start, end);
    }

    private List<BreakdownRow> breakdown(String key, String label, String join, LocalDateTime start, LocalDateTime end) {
        String jpql = """
                select %1$s, %2$s, count(b), sum(%4$s), sum(b.totalAmount)
                from Booking b join b.schedule s join s.tour t %3$s
                where %5$s and b.createdAt >= :start and b.createdAt < :end
                group by %1$s, %2$s
                order by sum(b.totalAmount) desc
                """.formatted(key, label, join, GUESTS, PAID);
        return em.createQuery(jpql, Object[].class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList().stream()
                .map(r -> new BreakdownRow(String.valueOf(r[0]), String.valueOf(r[1]),
                        toLong(r[2]), toLong(r[3]), toBig(r[4])))
                .toList();
    }

    private static long toLong(Object o) { return o == null ? 0 : ((Number) o).longValue(); }

    private static BigDecimal toBig(Object o) {
        if (o == null) return BigDecimal.ZERO;
        return o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
    }

    public record BreakdownRow(String key, String label, long bookings, long guests, BigDecimal revenue) {
    }
}
