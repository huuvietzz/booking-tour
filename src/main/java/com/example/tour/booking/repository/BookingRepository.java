package com.example.tour.booking.repository;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.booking.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Integer>, JpaSpecificationExecutor<Booking> {

    // ===== Đã có (module khác đang dùng) =====
    boolean existsBySchedule_Tour_TourId(Integer tourId);

    boolean existsBySchedule_ScheduleId(Integer scheduleScheduleId);

    long countBySchedule_ScheduleIdAndBookingStatusIn(Integer scheduleScheduleId, Collection<BookingStatus> bookingStatuses);

    boolean existsByUser_UserId(Integer userId);

    long countByUser_UserId(Integer userId);

    @Query("select b.user.userId, count(b) from Booking b where b.user.userId in :ids group by b.user.userId")
    List<Object[]> countByUserIds(@Param("ids") Collection<Integer> ids);

    // ===== Module booking =====

    /** Danh sách có phân trang: nạp sẵn lịch + tour trong cùng câu truy vấn (tránh N+1) */
    @Override
    @EntityGraph(attributePaths = {"schedule", "schedule.tour"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"schedule", "schedule.tour", "promotion", "user"})
    Optional<Booking> findWithDetailsByBookingId(Integer bookingId);

    @EntityGraph(attributePaths = {"schedule", "schedule.tour", "promotion"})
    Optional<Booking> findByBookingCode(String bookingCode);

    /**
     * Khoá dòng đơn khi đổi trạng thái. Job tự huỷ và admin xác nhận thanh toán có thể chạm cùng 1 đơn
     * cùng lúc → xếp hàng, người sau đọc lại trạng thái mới rồi mới quyết định.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingId = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Integer id);

    long countByBookingStatus(BookingStatus status);

    long countByPaymentStatus(PaymentStatus status);

    /** Sắp khởi hành: đơn đã xác nhận, ngày đi trong [from, to] */
    long countByBookingStatusAndSchedule_DepartureDateBetween(BookingStatus status, LocalDate from, LocalDate to);

    /**
     * Ghi đè findAll(spec, sort) của JpaSpecificationExecutor CHỈ để gắn EntityGraph.
     * Hàm phân trang findAll(spec, pageable) của trang danh sách KHÔNG bị ảnh hưởng.
     */
    @Override
    @EntityGraph(attributePaths = {"schedule", "schedule.tour", "promotion"})
    List<Booking> findAll(Specification<Booking> spec, Sort sort);

    // method khoá dòng theo mã đơn
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.bookingCode = :code")
    Optional<Booking> findByBookingCodeForUpdate(@Param("code") String code);

    /**
     * Đơn giữ chỗ quá hạn: còn PENDING, tạo trước mốc cutoff, và KHÔNG có giao dịch nào đang chờ / thành công
     * (khách đã bấm "Tôi đã thanh toán" thì không tự huỷ, chờ admin kiểm tra tiền).
     */
//    @Query("""
//            SELECT b.bookingId FROM Booking b
//            WHERE b.bookingStatus = com.example.tour.booking.constant.BookingStatus.PENDING
//              AND b.createdAt < :cutoff
//              AND NOT EXISTS (
//                  SELECT 1 FROM Payment p WHERE p.booking = b
//                  AND p.transactionStatus IN (
//                                             com.example.tour.payment.constant.TransactionStatus.SUCCESS))
//            """)
//    List<Integer> findExpiredHoldIds(@Param("cutoff") LocalDateTime cutoff);

    /** Tour đã về: CONFIRMED → COMPLETED (1 câu UPDATE cho tất cả) */
//    @Modifying(clearAutomatically = true, flushAutomatically = true)
//    @Query("""
//            UPDATE Booking b SET b.bookingStatus = com.example.tour.booking.constant.BookingStatus.COMPLETED
//            WHERE b.bookingStatus = com.example.tour.booking.constant.BookingStatus.CONFIRMED
//              AND b.schedule.scheduleId IN (SELECT s.scheduleId FROM Schedule s WHERE s.returnDate < :today)
//            """)
//    int completeFinishedTours(@Param("today") LocalDate today);
}
