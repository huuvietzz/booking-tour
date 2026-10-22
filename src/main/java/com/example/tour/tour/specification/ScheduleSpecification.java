package com.example.tour.tour.specification;

import com.example.tour.tour.dto.request.ScheduleFilterRequest;
import com.example.tour.tour.entity.Schedule;
import com.example.tour.tour.entity.Tour;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ScheduleSpecification {

    public static Specification<Schedule> adminFilter(ScheduleFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Join<Schedule, Tour> tour = root.join("tour");

            if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                String like = "%" + f.getKeyword().trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("scheduleCode"), like),
                        cb.like(tour.get("tourCode"), like),
                        cb.like(tour.get("tourName"), like)));
            }
            if (f.getTourId() != null) {
                ps.add(cb.equal(tour.get("tourId"), f.getTourId()));
            }
            if (f.getStatus() != null) {
                ps.add(cb.equal(root.get("status"), f.getStatus()));
            }
            if (f.getFromDate() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("departureDate"), f.getFromDate()));
            }
            if (f.getToDate() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("departureDate"), f.getToDate()));
            }
            if (Boolean.TRUE.equals(f.getUpcomingOnly())) {
                ps.add(cb.greaterThan(root.get("departureDate"), LocalDate.now()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}