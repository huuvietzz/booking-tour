package com.example.tour.tour.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/** Lịch "Ngày khởi hành" ở bộ lọc bên trái */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartureCalendarResponse {

    private List<Day> days;                  // ngày có tour trong tháng (ngày không có tour → không có trong list)
    private long totalUpcomingSchedules;     // Tổng số lịch khởi hành sắp tới"

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Day {
        private LocalDate date;
        private long tourCount;
    }
}