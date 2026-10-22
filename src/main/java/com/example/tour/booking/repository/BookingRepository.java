package com.example.tour.booking.repository;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

public interface BookingRepository extends JpaRepository<Booking, Integer>, JpaSpecificationExecutor<Booking> {
    boolean existsBySchedule_Tour_TourId(Integer tourId);

    boolean existsBySchedule_ScheduleId(Integer scheduleScheduleId);

    double countBySchedule_ScheduleIdAndBookingStatusIn(Integer scheduleScheduleId, Collection<BookingStatus> bookingStatuses);
}
