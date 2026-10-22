package com.example.tour.tour.repository;

import com.example.tour.tour.constant.ScheduleStatus;
import com.example.tour.tour.entity.Schedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer>, JpaSpecificationExecutor<Schedule> {

    // ===== Có sẵn trong project =====

    List<Schedule> findByTour_TourIdAndStatusAndDepartureDateGreaterThanEqualOrderByDepartureDateAsc(
            Integer tourId, ScheduleStatus status, LocalDate date);

    /** Các chuyến chưa khởi hành, sắp theo ngày gần nhất — dùng đổ dropdown phân công */
    @EntityGraph(attributePaths = {"tour"})
    List<Schedule> findByDepartureDateGreaterThanEqualOrderByDepartureDateAsc(LocalDate fromDate);

    /** Lịch sắp tới (chưa huỷ) của nhiều tour, sắp theo ngày — dùng cho card tour phía khách */
    @Query("SELECT s FROM Schedule s " +
            "WHERE s.tour.tourId IN :tourIds " +
            "AND s.departureDate > :today " +
            "AND s.status <> com.example.tour.tour.constant.ScheduleStatus.CANCELLED " +
            "ORDER BY s.departureDate ASC")
    List<Schedule> findUpcomingByTourIds(@Param("tourIds") Collection<Integer> tourIds,
                                         @Param("today") LocalDate today);

    // ===== Thêm cho module lịch khởi hành =====

    /** Danh sách admin: nạp sẵn tour trong cùng câu truy vấn (tránh N+1) */
    @Override
    @EntityGraph(attributePaths = {"tour"})
    Page<Schedule> findAll(Specification<Schedule> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"tour"})
    Optional<Schedule> findWithTourByScheduleId(Integer scheduleId);

    /** Mỗi tour chỉ có 1 lịch / ngày khởi hành (UNIQUE tour_id + departure_date) */
    boolean existsByTour_TourIdAndDepartureDate(Integer tourId, LocalDate departureDate);

    boolean existsByTour_TourIdAndDepartureDateAndScheduleIdNot(Integer tourId, LocalDate departureDate, Integer scheduleId);

    /**
     * Khoá dòng lịch khi đặt chỗ (module booking dùng): 2 khách đặt cùng lúc
     * sẽ xếp hàng, không bán quá số chỗ trống.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Schedule s WHERE s.scheduleId = :id")
    Optional<Schedule> findByIdForUpdate(@Param("id") Integer id);
}