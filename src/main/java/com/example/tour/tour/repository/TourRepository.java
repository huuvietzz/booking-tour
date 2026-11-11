package com.example.tour.tour.repository;

import com.example.tour.tour.entity.Tour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TourRepository extends JpaRepository<Tour, Integer>, JpaSpecificationExecutor<Tour> {
    /**
     * Danh sách có phân trang: nạp sẵn category + location trong cùng câu truy vấn,
     * tránh mỗi dòng lại SELECT category / location riêng (N+1).
     */
    @Override
    @EntityGraph(attributePaths = {"category", "location"})
    Page<Tour> findAll(Specification<Tour> spec, Pageable pageable);

    /**
     * Chi tiết phía khách: tour đang hiển thị, địa điểm đang hiển thị,
     * danh mục đang hiển thị.
     */
    @Query("SELECT t FROM Tour t " +
            "JOIN FETCH t.category c " +
            "JOIN FETCH t.location l " +
            "WHERE t.tourId = :id AND t.isActive = true " +
            "AND c.isActive = true AND l.isActive = true")
    Optional<Tour> findPublicById(@Param("id") Integer id);

    /** Tăng lượt xem ngay trong DB: an toàn khi nhiều khách mở trang cùng lúc */
    @Modifying
    @Query("UPDATE Tour t SET t.viewCount = t.viewCount + 1 WHERE t.tourId = :id")
    void incrementViewCount(@Param("id") Integer id);

    /** Số tour thuộc địa điểm (tính cả tour đang ẩn) — chặn xoá địa điểm */
    long countByLocation_LocationId(Integer locationId);

    /** Số tour thuộc danh mục (tính cả tour đang ẩn) — chặn xoá danh mục */
    long countByCategory_CategoryId(Integer categoryId);

    @Query("SELECT DISTINCT t.departureLocation FROM Tour t WHERE t.isActive = true ORDER BY t.departureLocation")
    List<String> findPublicDepartureLocations();

    /** Tất cả tour khách đang xem được, nạp sẵn địa điểm — chatbot chọn tour liên quan */
    @Query("SELECT t FROM Tour t " +
            "JOIN FETCH t.location l " +
            "JOIN t.category c " +
            "WHERE t.isActive = true AND l.isActive = true AND c.isActive = true")
    List<Tour> findAllPublicWithLocation();
}