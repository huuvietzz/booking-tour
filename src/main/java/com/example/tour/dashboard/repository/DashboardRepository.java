package com.example.tour.dashboard.repository;

import com.example.tour.dashboard.dto.response.RecentBookingResponse;
import com.example.tour.report.dto.response.ScheduleOccupancyResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DashboardRepository {

    private static final String BS = "com.example.tour.booking.constant.BookingStatus.";
    private static final String PS = "com.example.tour.booking.constant.PaymentStatus.";
    private static final String NOT_CANCELLED_SCHEDULE = "s.status <> com.example.tour.tour.constant.ScheduleStatus.CANCELLED";

    private final EntityManager em;

    public List<RecentBookingResponse> findRecentBookings(int limit) {
        return em.createQuery("""
                        select new com.example.tour.dashboard.dto.response.RecentBookingResponse(
                               b.bookingId, b.bookingCode, b.contactName, t.tourName, s.departureDate,
                               b.createdAt, b.totalAmount, b.bookingStatus, b.paymentStatus)
                        from Booking b join b.schedule s join s.tour t
                        order by b.createdAt desc, b.bookingId desc
                        """, RecentBookingResponse.class)
                .setMaxResults(limit)
                .getResultList();
    }

    /** Lịch khởi hành từ hôm nay trở đi, gần nhất trước */
    public List<ScheduleOccupancyResponse> findUpcomingSchedules(LocalDate today, int limit) {
        return em.createQuery("""
                        select new com.example.tour.report.dto.response.ScheduleOccupancyResponse(
                               s.scheduleId, s.scheduleCode, t.tourId, t.tourCode, t.tourName,
                               s.departureDate, s.totalSlots, s.availableSlots)
                        from Schedule s join s.tour t
                        where s.departureDate >= :today and %s
                        order by s.departureDate, s.scheduleId
                        """.formatted(NOT_CANCELLED_SCHEDULE), ScheduleOccupancyResponse.class)
                .setParameter("today", today)
                .setMaxResults(limit)
                .getResultList();
    }

    public long countAwaitingPayment() {
        return em.createQuery("select count(b) from Booking b where b.bookingStatus = " + BS + "CONFIRMED and b.paymentStatus = " + PS + "PENDING",
                Long.class).getSingleResult();
    }

    /** Đã thanh toán + tour đã về → chờ admin bấm "Hoàn thành" (BE cho hoàn thành từ ngày về) */
    public long countToComplete(LocalDate today) {
        return em.createQuery("""
                        select count(b) from Booking b join b.schedule s
                        where b.bookingStatus = %sCONFIRMED and b.paymentStatus = %sPAID and s.returnDate <= :today
                        """.formatted(BS, PS), Long.class)
                .setParameter("today", today)
                .getSingleResult();
    }

    public long countUnrepliedReviews() {
        return em.createQuery("""
                        select count(r) from Review r
                        where r.hidden = false and (r.adminResponse is null or trim(r.adminResponse) = '')
                        """, Long.class).getSingleResult();
    }

    /** Lịch khởi hành trong [from, to] đã bán dưới 50% chỗ: (tổng − còn trống) * 2 < tổng */
    public long countLowSchedules(LocalDate from, LocalDate to) {
        return em.createQuery("""
                        select count(s) from Schedule s
                        where s.departureDate >= :from and s.departureDate <= :to and %s
                          and (s.totalSlots - s.availableSlots) * 2 < s.totalSlots
                        """.formatted(NOT_CANCELLED_SCHEDULE), Long.class)
                .setParameter("from", from).setParameter("to", to)
                .getSingleResult();
    }
}
