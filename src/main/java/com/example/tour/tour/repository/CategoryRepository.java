package com.example.tour.tour.repository;

import com.example.tour.tour.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer>, JpaSpecificationExecutor<Category> {

    Optional<Category> findByCategoryIdAndIsActiveTrue(int categoryId);

    // Thứ tự hiển thị đã có danh mục ĐANG HOẠT ĐỘNG dùng chưa
    boolean existsByDisplayOrderAndIsActiveTrue(Integer displayOrder);

    // Như trên nhưng bỏ qua chính danh mục đang sửa
    boolean existsByDisplayOrderAndIsActiveTrueAndCategoryIdNot(Integer displayOrder, Integer categoryId);

    // Slug trùng (tính cả danh mục đã ẩn, vì cột slug UNIQUE trên toàn bảng)
    boolean existsBySlug(String slug);

    // Như trên nhưng bỏ qua chính danh mục đang sửa
    boolean existsBySlugAndCategoryIdNot(String slug, Integer categoryId);

    // Danh mục đang hiển thị, theo thứ tự (cho menu / bộ lọc phía khách)
    List<Category> findByIsActiveTrueOrderByDisplayOrderAsc();
}
