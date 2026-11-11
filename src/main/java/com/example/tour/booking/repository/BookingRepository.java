package com.example.tour.booking.repository;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer>, JpaSpecificationExecutor<Booking> {
    boolean existsBySchedule_Tour_TourId(Integer tourId);

    boolean existsBySchedule_ScheduleId(Integer scheduleScheduleId);

    double countBySchedule_ScheduleIdAndBookingStatusIn(Integer scheduleScheduleId, Collection<BookingStatus> bookingStatuses);

    boolean existsByUser_UserId(Integer userId);

    long countByUser_UserId(Integer userId);

    /** Số đơn của nhiều khách trong 1 câu: mỗi dòng = [userId, số đơn] */
    @Query("select b.user.userId, count(b) from Booking b where b.user.userId in :ids group by b.user.userId")
    List<Object[]> countByUserIds(@Param("ids") Collection<Integer> ids);
}
