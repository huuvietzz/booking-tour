package com.example.tour.report.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Tổng hợp lấp đầy của các lịch khởi hành (không tính lịch đã huỷ) có NGÀY KHỞI HÀNH trong khoảng */
@Getter
@Builder
public class OccupancySummaryResponse {
    private long schedules;
    private long totalSlots;
    private long bookedSlots;        // total_slots - available_slots
    private double occupancyRate;    // bookedSlots / totalSlots (%)
    private long fullSchedules;      // kín chỗ (100%)
    private long lowSchedules;       // dưới 50%
}
