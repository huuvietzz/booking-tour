package com.example.tour.tour.specification;

import com.example.tour.tour.constant.ScheduleStatus;
import com.example.tour.tour.dto.request.PublicTourFilterRequest;
import com.example.tour.tour.dto.request.TourFilterRequest;
import com.example.tour.tour.entity.Category;
import com.example.tour.tour.entity.Location;
import com.example.tour.tour.entity.Schedule;
import com.example.tour.tour.entity.Tour;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TourSpecification {

    /** Tour không có lịch sắp tới xếp cuối khi sort theo ngày khởi hành gần nhất */
    private static final LocalDate NO_UPCOMING = LocalDate.of(9999, 12, 31);

    // =====================================================================
    // ADMIN: thấy cả tour đang ẩn
    // =====================================================================
    public static Specification<Tour> adminFilter(TourFilterRequest f) {
        return (root, query, cb) -> {
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
            if (f.getIsActive() != null) {
                ps.add(cb.equal(root.get("isActive"), f.getIsActive()));
            }
            addPriceRange(ps, root, cb, f.getMinPrice(), f.getMaxPrice());

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    // =====================================================================
    // KHÁCH: danh sách tour
    // =====================================================================
    public static Specification<Tour> publicFilter(PublicTourFilterRequest f, LocalDate today) {
        return (root, query, cb) -> {
            List<Predicate> ps = publicPredicates(f, root, query, cb, true);

            // Sắp theo ngày khởi hành gần nhất: không phải cột của Tour nên Sort.by(...) không làm được,
            // phải tự orderBy bằng subquery MIN(departureDate) các lịch sắp tới.
            // Bỏ qua ở câu COUNT mà Spring tự chạy để phân trang (COUNT không được có ORDER BY).
            boolean isCountQuery = Long.class.equals(query.getResultType());
            if (!isCountQuery && PublicTourFilterRequest.SORT_DEPARTURE.equals(f.getSort())) {
                Subquery<LocalDate> next = query.subquery(LocalDate.class);
                Root<Schedule> s = next.from(Schedule.class);
                next.select(cb.least(s.<LocalDate>get("departureDate"))).where(
                        cb.equal(s.get("tour"), root),
                        cb.greaterThan(s.get("departureDate"), today),
                        cb.notEqual(s.get("status"), ScheduleStatus.CANCELLED));

                query.orderBy(
                        cb.asc(cb.coalesce(next, NO_UPCOMING)),
                        cb.desc(root.get("tourId")));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /**
     * Điều kiện lọc tour phía khách, dùng chung cho:
     *  - danh sách tour (tour = Root<Tour>)
     *  - lịch đếm số tour mỗi ngày (tour = Join<Schedule, Tour>)
     *
     * @param includeDate false khi đếm cho lịch: lịch phải hiện số tour của MỌI ngày,
     *                    nếu lọc luôn theo ngày đang chọn thì các ngày khác sẽ về 0 hết
     */
    public static List<Predicate> publicPredicates(PublicTourFilterRequest f, From<?, Tour> tour,
                                                   CommonAbstractCriteria query, CriteriaBuilder cb,
                                                   boolean includeDate) {
        List<Predicate> ps = new ArrayList<>();

        ps.add(cb.isTrue(tour.get("isActive")));
        Join<Tour, Location> location = tour.join("location");
        ps.add(cb.isTrue(location.get("isActive")));
        Join<Tour, Category> category = tour.join("category");
        ps.add(cb.isTrue(category.get("isActive")));

        if (f.getCategoryId() != null) {
            ps.add(cb.equal(category.get("categoryId"), f.getCategoryId()));
        }
        if (f.getDepartureLocation() != null && !f.getDepartureLocation().isBlank()) {
            ps.add(cb.equal(tour.get("departureLocation"), f.getDepartureLocation().trim()));
        }
        if (f.getLocationIds() != null && !f.getLocationIds().isEmpty()) {
            ps.add(location.get("locationId").in(f.getLocationIds()));
        }
        if (f.getTourClasses() != null && !f.getTourClasses().isEmpty()) {
            ps.add(tour.get("tourClass").in(f.getTourClasses()));
        }
        if (f.getIsFeatured() != null) {
            ps.add(cb.equal(tour.get("isFeatured"), f.getIsFeatured()));
        }
        addPriceRange(ps, tour, cb, f.getMinPrice(), f.getMaxPrice());

        // Ngày khởi hành: tour phải CÓ lịch chưa huỷ đúng ngày đó.
        // Dùng EXISTS chứ không JOIN: JOIN sẽ nhân dòng tour lên → trùng tour, sai tổng số trang.
        if (includeDate && f.getDepartureDate() != null) {
            Subquery<Integer> sq = query.subquery(Integer.class);
            Root<Schedule> s = sq.from(Schedule.class);
            sq.select(s.get("scheduleId")).where(
                    cb.equal(s.get("tour"), tour),
                    cb.equal(s.get("departureDate"), f.getDepartureDate()),
                    cb.notEqual(s.get("status"), ScheduleStatus.CANCELLED));
            ps.add(cb.exists(sq));
        }
        return ps;
    }

    private static void addPriceRange(List<Predicate> ps, Path<Tour> tour, CriteriaBuilder cb,
                                      BigDecimal min, BigDecimal max) {
        if (min != null) {
            ps.add(cb.greaterThanOrEqualTo(tour.get("basePrice"), min));
        }
        if (max != null) {
            ps.add(cb.lessThanOrEqualTo(tour.get("basePrice"), max));
        }
    }
}