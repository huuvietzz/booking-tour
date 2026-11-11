package com.example.tour.user.repository;

import com.example.tour.tour.entity.Tour;
import com.example.tour.user.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {

    /**
     * Thêm yêu thích, đã có thì bỏ qua (idempotent).
     * Không dùng "existsBy... rồi save": bấm tim 2 lần thật nhanh / mở 2 tab → 2 request cùng qua bước exists,
     * request sau đụng unique_user_tour → lỗi 500. Ở đây MySQL tự xử lý trùng trong 1 câu lệnh.
     * ON DUPLICATE KEY chỉ bỏ qua lỗi TRÙNG KHOÁ; lỗi khác (khoá ngoại...) vẫn báo, khác INSERT IGNORE.
     */
    @Modifying
    @Query(value = """
            INSERT INTO favorites (user_id, tour_id, created_at)
            VALUES (:userId, :tourId, NOW())
            ON DUPLICATE KEY UPDATE favorite_id = favorite_id
            """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") int userId, @Param("tourId") int tourId);

    /** Bỏ yêu thích. Chưa thích thì xoá 0 dòng, không lỗi (idempotent). */
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.user.userId = :userId AND f.tour.tourId = :tourId")
    int deleteByUserIdAndTourId(@Param("userId") int userId, @Param("tourId") int tourId);

    /**
     * Trang "Tour yêu thích": mới thích xếp trước.
     * Chỉ lấy tour khách đang xem được (tour + danh mục + địa điểm đều đang hiển thị), giống findPublicById.
     * Tour bị ẩn thì bản ghi vẫn giữ → admin mở bán lại là tự hiện lại.
     * ORDER BY viết sẵn → controller truyền Pageable KHÔNG kèm Sort.
     * countQuery tự viết vì câu chính SELECT Tour nhưng đếm theo Favorite.
     */
    @Query(value = """
            SELECT t FROM Favorite f
            JOIN f.tour t
            WHERE f.user.userId = :userId
              AND t.isActive = true AND t.category.isActive = true AND t.location.isActive = true
            ORDER BY f.createdAt DESC, f.favoriteId DESC
            """,
            countQuery = """
            SELECT COUNT(f) FROM Favorite f
            JOIN f.tour t
            WHERE f.user.userId = :userId
              AND t.isActive = true AND t.category.isActive = true AND t.location.isActive = true
            """)
    Page<Tour> findFavoriteTours(@Param("userId") int userId, Pageable pageable);

    /** Id các tour đã thích → tours.html / index.html tô đỏ tim, 1 query cho cả trang */
    @Query("SELECT f.tour.tourId FROM Favorite f WHERE f.user.userId = :userId")
    List<Integer> findTourIdsByUserId(@Param("userId") int userId);

    /** Xoá khách hàng: xoá yêu thích trước, không thì vướng khoá ngoại favorites.user_id */
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.user.userId = :userId")
    int deleteByUserId(@Param("userId") int userId);

    /** Xoá cứng tour: xoá yêu thích trước, không thì vướng khoá ngoại favorites.tour_id */
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.tour.tourId = :tourId")
    int deleteByTourId(@Param("tourId") int tourId);
}
