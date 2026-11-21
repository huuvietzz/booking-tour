package com.example.tour.report.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Lấp đầy gộp theo tour (mọi lịch của tour trong khoảng) */
@Getter
@Builder
public class TourOccupancyResponse {
    private Integer tourId;
    private String tourCode;
    private String tourName;
    private long schedules;
    private long totalSlots;
    private long bookedSlots;
    private double occupancyRate;
}
