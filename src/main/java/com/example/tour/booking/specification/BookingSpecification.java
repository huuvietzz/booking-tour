package com.example.tour.booking.specification;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.booking.dto.request.BookingFilterRequest;
import com.example.tour.booking.entity.Booking;
import com.example.tour.payment.constant.TransactionStatus;
import com.example.tour.payment.entity.Payment;
import com.example.tour.tour.entity.Schedule;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookingSpecification {

    private BookingSpecification() {}

    public static Specification<Booking> adminFilter(BookingFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Join<Booking, Schedule> schedule = root.join("schedule");

            if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                String kw = "%" + f.getKeyword().trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("bookingCode")), kw),
                        cb.like(cb.lower(root.get("contactName")), kw),
                        cb.like(cb.lower(root.get("contactEmail")), kw),
                        cb.like(root.get("contactPhone"), kw)));
            }
            if (f.getBookingStatus() != null) ps.add(cb.equal(root.get("bookingStatus"), f.getBookingStatus()));
            if (f.getPaymentStatus() != null) ps.add(cb.equal(root.get("paymentStatus"), f.getPaymentStatus()));
            if (f.getTourId() != null) ps.add(cb.equal(schedule.get("tour").get("tourId"), f.getTourId()));
            if (f.getDepartureFrom() != null) ps.add(cb.greaterThanOrEqualTo(schedule.get("departureDate"), f.getDepartureFrom()));
            if (f.getDepartureTo() != null) ps.add(cb.lessThanOrEqualTo(schedule.get("departureDate"), f.getDepartureTo()));

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /** Đơn của 1 khách; 2 combobox để trống (null) = không lọc theo cột đó */
    public static Specification<Booking> ofUser(Integer userId, BookingStatus bookingStatus, PaymentStatus paymentStatus) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("user").get("userId"), userId));   // LUÔN lọc theo chủ đơn
            if (bookingStatus != null) ps.add(cb.equal(root.get("bookingStatus"), bookingStatus));
            if (paymentStatus != null) ps.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
