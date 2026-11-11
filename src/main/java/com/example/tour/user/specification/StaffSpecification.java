package com.example.tour.user.specification;

import com.example.tour.user.constant.Role;
import com.example.tour.user.dto.request.StaffFilterRequest;
import com.example.tour.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class StaffSpecification {

    private StaffSpecification() {}

    public static Specification<User> adminFilter(StaffFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            // Luôn chỉ ADMIN + STAFF: khách hàng không bao giờ lọt vào trang này
            ps.add(root.get("role").in(Role.ADMIN, Role.STAFF));
            if (f == null) return cb.and(ps.toArray(new Predicate[0]));

            if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                String like = "%" + f.getKeyword().trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(root.get("phoneNumber"), like)));
            }
            if (f.getRole() == Role.ADMIN || f.getRole() == Role.STAFF) ps.add(cb.equal(root.get("role"), f.getRole()));
            if (f.getIsActive() != null) ps.add(cb.equal(root.get("isActive"), f.getIsActive()));
            if (f.getGender() != null) ps.add(cb.equal(root.get("gender"), f.getGender()));
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
