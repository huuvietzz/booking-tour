package com.example.tour.tour.dto.request;

import com.example.tour.tour.constant.ScheduleStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Lọc danh sách lịch (admin). Trường nào null → bỏ qua. */
@Getter
@Setter
@NoArgsConstructor
public class ScheduleFilterRequest {
    private String keyword;          // mã lịch, mã tour, tên tour
    private Integer tourId;
    private ScheduleStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;      // ngày khởi hành từ (yyyy-MM-dd)

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;        // ngày khởi hành đến

    private Boolean upcomingOnly;    // true → chỉ lịch chưa khởi hành
}