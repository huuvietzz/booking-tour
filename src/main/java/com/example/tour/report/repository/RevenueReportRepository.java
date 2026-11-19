package com.example.tour.report.repository;

import com.example.tour.report.dto.response.TopTourRevenueResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Truy vấn thống kê trên bảng bookings, tính theo NGÀY ĐẶT (created_at).
 * Không phải JpaRepository vì không quản lý entity riêng nào — chỉ chạy câu tổng hợp (GROUP BY).
 *
 * Vì sao dùng bookings mà không dùng payments: admin "Xác nhận thanh toán" tay chỉ đổi payment_status,
 * không tạo dòng payments → tính từ payments sẽ thiếu tiền. payment_status của booking luôn đúng.
 */
@Repository
@RequiredArgsConstructor
public class RevenueReportRepository {

    private static final String PS = "com.example.tour.booking.constant.PaymentStatus.";
    private static final String BS = "com.example.tour.booking.constant.BookingStatus.";
    private static final String PAID     = "b.paymentStatus = " + PS + "PAID";
    private static final String REFUNDED = "b.paymentStatus = " + PS + "REFUNDED";
    private static final String PENDING  = "b.paymentStatus = " + PS + "PENDING and b.bookingStatus <> " + BS + "CANCELLED";
    private static final String GUESTS   = "(b.totalAdults + coalesce(b.totalChildren, 0) + coalesce(b.totalToddlers, 0))";

    private final EntityManager em;

    /** Số liệu từng ngày trong [from, to] — chỉ những ngày có đơn. 1 câu SQL cho cả khoảng. */
    public List<DailyRow> findDailyStats(LocalDate from, LocalDate to) {
        String jpql = """
                select cast(b.createdAt as LocalDate),
                       count(b),
                       sum(case when %1$s then 1 else 0 end),
                       sum(case when %1$s then b.totalAmount else 0 end),
                       sum(case when %1$s then %5$s else 0 end),
                       sum(case when %2$s then 1 else 0 end),
                       sum(case when %2$s then b.totalAmount else 0 end),
                       sum(case when b.bookingStatus = %4$sCANCELLED then 1 else 0 end),
                       sum(case when %3$s then 1 else 0 end),
                       sum(case when %3$s then b.totalAmount else 0 end)
                from Booking b
                where b.createdAt >= :start and b.createdAt < :end
                group by cast(b.createdAt as LocalDate)
                """.formatted(PAID, PENDING, REFUNDED, BS, GUESTS);

        List<Object[]> rows = em.createQuery(jpql, Object[].class)
                .setParameter("start", start(from))
                .setParameter("end", end(to))
                .getResultList();

        return rows.stream().map(r -> new DailyRow(
                toLocalDate(r[0]),
                toLong(r[1]), toLong(r[2]), toBig(r[3]), toLong(r[4]),
                toLong(r[5]), toBig(r[6]), toLong(r[7]), toLong(r[8]), toBig(r[9])
        )).toList();
    }

    /** Tour doanh thu cao nhất trong [from, to] (chỉ đơn đã thanh toán) */
    public List<TopTourRevenueResponse> findTopTours(LocalDate from, LocalDate to, int limit) {
        String jpql = """
                select new com.example.tour.report.dto.response.TopTourRevenueResponse(
                       t.tourId, t.tourCode, t.tourName, t.thumbnailUrl,
                       count(b), sum(%2$s), sum(b.totalAmount))
                from Booking b join b.schedule s join s.tour t
                where %1$s and b.createdAt >= :start and b.createdAt < :end
                group by t.tourId, t.tourCode, t.tourName, t.thumbnailUrl
                order by sum(b.totalAmount) desc, count(b) desc
                """.formatted(PAID, GUESTS);

        return em.createQuery(jpql, TopTourRevenueResponse.class)
                .setParameter("start", start(from))
                .setParameter("end", end(to))
                .setMaxResults(limit)
                .getResultList();
    }

    // [from 00:00, to+1 00:00) — không dùng between ... 23:59:59 để khỏi sót phần mili giây
    private static LocalDateTime start(LocalDate from) { return from.atStartOfDay(); }
    private static LocalDateTime end(LocalDate to)     { return to.plusDays(1).atStartOfDay(); }

    // Kiểu trả về của SUM/CAST khác nhau theo dialect → chuyển an toàn
    private static long toLong(Object o) { return o == null ? 0 : ((Number) o).longValue(); }

    private static BigDecimal toBig(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        return new BigDecimal(o.toString());
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof LocalDateTime dt) return dt.toLocalDate();
        return LocalDate.parse(o.toString().substring(0, 10));
    }

    /** Số liệu 1 ngày */
    public record DailyRow(LocalDate date,
                           long totalBookings, long paidBookings, BigDecimal revenue, long guests,
                           long pendingBookings, BigDecimal pendingAmount,
                           long cancelledBookings, long refundedBookings, BigDecimal refundAmount) {
    }
}
