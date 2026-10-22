package com.example.tour.tour.specification;

import com.example.tour.tour.dto.request.TourFilterRequest;
import com.example.tour.tour.entity.Category;
import com.example.tour.tour.entity.Location;
import com.example.tour.tour.entity.Tour;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TourSpecification {

    /** Admin: thấy cả tour đang ẩn; lọc thêm theo trạng thái nếu có */
    public static Specification<Tour> adminFilter(TourFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> ps = commonPredicates(f, root, cb);
            if (f.getIsActive() != null) {
                ps.add(cb.equal(root.get("isActive"), f.getIsActive()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /** Khách: chỉ tour đang hiển thị, thuộc địa điểm + danh mục đang hiển thị */
    public static Specification<Tour> publicFilter(TourFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> ps = commonPredicates(f, root, cb);

            ps.add(cb.isTrue(root.get("isActive")));

            Join<Tour, Location> location = root.join("location");
            ps.add(cb.isTrue(location.get("isActive")));

            Join<Tour, Category> category = root.join("category");
            ps.add(cb.isTrue(category.get("isActive")));

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static List<Predicate> commonPredicates(TourFilterRequest f, Root<Tour> root, CriteriaBuilder cb) {
        List<Predicate> ps = new ArrayList<>();

        if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
            String like = "%" + f.getKeyword().trim() + "%";
            ps.add(cb.or(
                    cb.like(root.get("tourName"), like),
                    cb.like(root.get("tourCode"), like)));
        }
        if (f.getCategoryId() != null) {
            ps.add(cb.equal(root.get("category").get("categoryId"), f.getCategoryId()));
        }
        if (f.getLocationId() != null) {
            ps.add(cb.equal(root.get("location").get("locationId"), f.getLocationId()));
        }
        if (f.getTourClass() != null) {
            ps.add(cb.equal(root.get("tourClass"), f.getTourClass()));
        }
        if (f.getIsFeatured() != null) {
            ps.add(cb.equal(root.get("isFeatured"), f.getIsFeatured()));
        }
        if (f.getMinPrice() != null) {
            ps.add(cb.greaterThanOrEqualTo(root.get("basePrice"), f.getMinPrice()));
        }
        if (f.getMaxPrice() != null) {
            ps.add(cb.lessThanOrEqualTo(root.get("basePrice"), f.getMaxPrice()));
        }

        return ps;
    }
}