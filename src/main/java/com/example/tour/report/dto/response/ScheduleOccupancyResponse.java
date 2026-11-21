package com.example.tour.report.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 1 lịch khởi hành. Constructor dùng trong JPQL select new */
@Getter
public class ScheduleOccupancyResponse {
    private final Integer scheduleId;
    private final String scheduleCode;
    private final Integer tourId;
    private final String tourCode;
    private final String tourName;
    private final LocalDate departureDate;
    private final int totalSlots;
    private final int bookedSlots;
    @Setter
    private double occupancyRate;

    public ScheduleOccupancyResponse(Integer scheduleId, String scheduleCode, Integer tourId, String tourCode, String tourName,
                                     LocalDate departureDate, Integer totalSlots, Integer availableSlots) {
        this.scheduleId = scheduleId;
        this.scheduleCode = scheduleCode;
        this.tourId = tourId;
        this.tourCode = tourCode;
        this.tourName = tourName;
        this.departureDate = departureDate;
        this.totalSlots = totalSlots == null ? 0 : totalSlots;
        int available = availableSlots == null ? 0 : availableSlots;
        this.bookedSlots = Math.max(this.totalSlots - available, 0);
    }
}
