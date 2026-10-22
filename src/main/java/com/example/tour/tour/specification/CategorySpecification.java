package com.example.tour.tour.specification;

import com.example.tour.tour.entity.Category;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CategorySpecification {

    /**
     * keyword : tìm theo tên danh mục (chứa chuỗi). null/rỗng → bỏ qua
     * isActive: true = Đã kích hoạt, false = Chưa kích hoạt, null → lấy tất cả
     */
    public static Specification<Category> filter(String keyword, Boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(root.get("categoryName"), "%" + keyword.trim() + "%"));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}