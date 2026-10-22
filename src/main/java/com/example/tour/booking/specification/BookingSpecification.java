//package com.example.tour.booking.specification;
//
//import com.example.tour.booking.constant.BookingPaymentStatus;
//import com.example.tour.booking.constant.BookingStatus;
//import com.example.tour.booking.entity.Booking;
//import com.example.tour.tour.entity.Tour;
//import com.example.tour.tour.entity.TourSchedule;
//import com.example.tour.user.entity.User;
//import jakarta.persistence.criteria.Join;
//import jakarta.persistence.criteria.Predicate;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//
//public class BookingSpecification {
//    public static Specification<Booking> filterForAdmin(String keyword,
//                                                        BookingStatus bookingStatus,
//                                                        BookingPaymentStatus bookingPaymentStatus,
//                                                        LocalDate fromDate,
//                                                        LocalDate toDate) {
//        return (root, query, cb) -> {
//            List<Predicate> predicates = new ArrayList<>();
//
//            Join<Booking, User> userJoin = root.join("user");
//            Join<Booking, TourSchedule> scheduleJoin = root.join("tourSchedule");
//            Join<TourSchedule, Tour> tourJoin = scheduleJoin.join("tour");
//
//            if(bookingStatus != null) {
//                predicates.add(cb.equal(root.get("bookingStatus"), bookingStatus));
//            }
//
//            if(bookingPaymentStatus != null) {
//                predicates.add(cb.equal(root.get("bookingPaymentStatus"), bookingPaymentStatus));
//            }
//
//            // Lọc theo khoảng ngày khởi hành
//            if(fromDate != null) {
//                predicates.add(cb.greaterThanOrEqualTo(scheduleJoin.get("departureDate"), fromDate));
//            }
//
//            if(toDate != null) {
//                predicates.add(cb.lessThanOrEqualTo(scheduleJoin.get("departureDate"), toDate));
//            }
//
//            if(keyword != null && !keyword.trim().isEmpty()) {
//                String pattern = "%" + keyword.trim().toLowerCase() + "%";
//                predicates.add(cb.or(
//                   cb.like(cb.lower(root.get("bookingCode")), pattern),
//                        cb.like(cb.lower(userJoin.get("fullName")), pattern),
//                        cb.like(cb.lower(userJoin.get("email")), pattern),
//                        cb.like(userJoin.get("phoneNumber"), pattern),
//                        cb.like(cb.lower(tourJoin.get("tourName")), pattern),
//                        cb.like(cb.lower(tourJoin.get("tourCode")), pattern)
//                ));
//            }
//            return cb.and(predicates.toArray(new Predicate[0]));
//        };
//    }
//}
