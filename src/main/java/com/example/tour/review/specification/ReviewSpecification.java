package com.example.tour.review.specification;

import com.example.tour.review.dto.request.ReviewFilterRequest;
import com.example.tour.review.entity.Review;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ReviewSpecification {

    private ReviewSpecification() {}

    public static Specification<Review> adminFilter(ReviewFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Join<Object, Object> booking = root.join("booking");
            Join<Object, Object> tour = booking.join("schedule").join("tour");

            if (StringUtils.hasText(f.getKeyword())) {
                String kw = "%" + f.getKeyword().trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("comment")), kw),
                        cb.like(cb.lower(booking.get("bookingCode")), kw),
                        cb.like(cb.lower(booking.get("contactName")), kw),
                        cb.like(cb.lower(tour.get("tourName")), kw)
                ));
            }
            if (f.getRating() != null) {
                ps.add(cb.equal(root.get("rating"), f.getRating()));
            }
            if (f.getHidden() != null) {
                ps.add(cb.equal(root.get("hidden"), f.getHidden()));
            }
            if (f.getReplied() != null) {
                Predicate noReply = cb.or(cb.isNull(root.get("adminResponse")),
                        cb.equal(cb.trim(root.get("adminResponse")), ""));
                ps.add(f.getReplied() ? cb.not(noReply) : noReply);
            }
            if (f.getTourId() != null) {
                ps.add(cb.equal(tour.get("tourId"), f.getTourId()));
            }
            if (f.getFromDate() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.getFromDate().atStartOfDay()));
            }
            if (f.getToDate() != null) {   // < 00:00 hôm sau → tính trọn ngày cuối
                ps.add(cb.lessThan(root.get("createdAt"), f.getToDate().plusDays(1).atStartOfDay()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
