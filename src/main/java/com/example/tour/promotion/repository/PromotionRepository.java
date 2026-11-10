package com.example.tour.promotion.repository;

import com.example.tour.promotion.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Integer>, JpaSpecificationExecutor<Promotion> {

    Optional<Promotion> findByPromotionCode(String promotionCode);

    boolean existsByPromotionCode(String promotionCode);

    boolean existsByPromotionCodeAndPromotionIdNot(String promotionCode, Integer promotionId);

    /**
     * Tăng lượt dùng NGUYÊN TỬ — gọi trong transaction tạo đơn (BookingService).
     * Điều kiện nằm ngay trong câu UPDATE: 2 khách cùng lấy lượt cuối thì DB chỉ cho 1 câu khớp,
     * câu kia trả 0 dòng → báo hết lượt. Nếu đọc usedCount lên rồi +1 bằng Java thì cả 2 cùng đọc
     * được "còn 1 lượt" và cùng ghi thành công → vượt quantity.
     * @return 1 = lấy được lượt, 0 = hết lượt
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Promotion p set p.usedCount = p.usedCount + 1
            where p.promotionId = :id and (p.quantity is null or p.usedCount < p.quantity)
            """)
    int increaseUsedCount(@Param("id") Integer promotionId);

    /** Huỷ đơn có dùng mã → trả lại lượt */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Promotion p set p.usedCount = p.usedCount - 1 where p.promotionId = :id and p.usedCount > 0")
    int decreaseUsedCount(@Param("id") Integer promotionId);

    /**
     * Số đơn CÒN HIỆU LỰC của 1 khách đã dùng mã này (kiểm tra maxUsesPerUser).
     * ⚠ Viết theo giả định entity Booking có: promotion (ManyToOne), user (ManyToOne), status (enum có CANCELLED).
     *   Tên field khác thì sửa câu JPQL này — sai tên field thì ứng dụng báo lỗi ngay lúc khởi động.
     */
    /** Số đơn CÒN HIỆU LỰC của 1 khách đã dùng mã này (kiểm tra maxUsesPerUser). Đơn đã huỷ không tính */
    @Query("""
        select count(b) from Booking b
        where b.promotion.promotionId = :promotionId and b.user.userId = :userId
          and b.bookingStatus <> com.example.tour.booking.constant.BookingStatus.CANCELLED
        """)
    long countActiveUsesByUser(@Param("promotionId") Integer promotionId, @Param("userId") Integer userId);
}
