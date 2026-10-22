package com.example.tour.tour.service;

import com.example.tour.tour.dto.request.ScheduleFilterRequest;
import com.example.tour.tour.dto.request.ScheduleRequest;
import com.example.tour.tour.dto.response.ScheduleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ScheduleService {

    ScheduleResponse createSchedule(ScheduleRequest request);

    ScheduleResponse updateSchedule(int scheduleId, ScheduleRequest request);

    /** Huỷ lịch: chỉ khi không còn booking đang hiệu lực */
    ScheduleResponse cancelSchedule(int scheduleId);

    /** Mở lại lịch đã huỷ: chỉ khi chưa tới ngày khởi hành */
    ScheduleResponse reopenSchedule(int scheduleId);

    /** Xoá cứng: chỉ khi chưa từng có booking */
    void deleteSchedule(int scheduleId);

    Page<ScheduleResponse> getSchedulesForAdmin(ScheduleFilterRequest filter, Pageable pageable);

    ScheduleResponse getScheduleForAdmin(int scheduleId);

    /** Phía khách: lịch sắp tới, chưa huỷ, của một tour đang hiển thị */
    List<ScheduleResponse> getUpcomingSchedulesOfTour(int tourId);
}