package com.example.tour.promotion.specification;

import com.example.tour.promotion.dto.request.PromotionFilterRequest;
import com.example.tour.promotion.entity.Promotion;
import com.example.tour.tour.entity.Tour;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PromotionSpecification {

    private PromotionSpecification() {}

    /** Bộ lọc trang quản lý */
    public static Specification<Promotion> adminFilter(PromotionFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (f == null) return cb.conjunction();

            if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                String like = "%" + f.getKeyword().trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("promotionCode")), like),
                        cb.like(cb.lower(root.get("promotionName")), like)));
            }
            if (f.getDiscountType() != null) {
                ps.add(cb.equal(root.get("discountType"), f.getDiscountType()));
            }
            if (f.getIsActive() != null) {
                ps.add(cb.equal(root.get("isActive"), f.getIsActive()));
            }
            if (f.getTourId() != null) {
                ps.add(appliesToTour(root, query, cb, f.getTourId()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /** Mã khách đang dùng được (trang khách): bật, trong thời hạn, còn lượt, áp dụng cho tour (nếu truyền tourId) */
    public static Specification<Promotion> usable(Integer tourId) {
        return (root, query, cb) -> {
            LocalDateTime now = LocalDateTime.now();
            Path<Integer> quantity = root.get("quantity"), used = root.get("usedCount");
            Predicate p = cb.and(
                    cb.isTrue(root.get("isActive")),
                    cb.lessThanOrEqualTo(root.get("startDate"), now),
                    cb.greaterThanOrEqualTo(root.get("endDate"), now),
                    cb.or(cb.isNull(quantity), cb.lessThan(used, quantity)));   // NULL = không giới hạn lượt
            return tourId == null ? p : cb.and(p, appliesToTour(root, query, cb, tourId));
        };
    }

    // ----- Hỗ trợ -----

    /**
     * Áp dụng cho tour = KHÔNG giới hạn tour nào (danh sách rỗng) HOẶC danh sách có tour đó.
     * Dùng EXISTS chứ không JOIN thẳng: JOIN bảng nhiều-nhiều làm 1 mã lặp thành nhiều dòng → phân trang đếm sai.
     */
    private static Predicate appliesToTour(Root<Promotion> root, CriteriaQuery<?> query, CriteriaBuilder cb, Integer tourId) {
        Subquery<Integer> anyTour = query.subquery(Integer.class);
        Root<Promotion> p1 = anyTour.correlate(root);
        Join<Promotion, Tour> t1 = p1.join("tours");
        anyTour.select(t1.get("tourId"));

        Subquery<Integer> thisTour = query.subquery(Integer.class);
        Root<Promotion> p2 = thisTour.correlate(root);
        Join<Promotion, Tour> t2 = p2.join("tours");
        thisTour.select(t2.get("tourId")).where(cb.equal(t2.get("tourId"), tourId));

        return cb.or(cb.not(cb.exists(anyTour)), cb.exists(thisTour));
    }
}
