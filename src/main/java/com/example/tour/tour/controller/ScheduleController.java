package com.example.tour.tour.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.tour.dto.request.ScheduleFilterRequest;
import com.example.tour.tour.dto.request.ScheduleRequest;
import com.example.tour.tour.dto.response.ScheduleResponse;
import com.example.tour.tour.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScheduleController {
    private static final Set<String> ADMIN_SORTS = Set.of("departureDate", "createdAt", "adultPrice", "availableSlots");
    private final ScheduleService scheduleService;

    @PostMapping("/admin/schedules")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(@RequestBody @Valid ScheduleRequest request) {
        ScheduleResponse data = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo lịch khởi hành thành công", data));
    }

    @PutMapping("/admin/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(@PathVariable Integer scheduleId,
                                                                        @RequestBody @Valid ScheduleRequest request) {
        ScheduleResponse data = scheduleService.updateSchedule(scheduleId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật lịch khởi hành thành công",data));
    }

    @PatchMapping("/admin/schedules/{scheduleId}/cancel")
    public ResponseEntity<ApiResponse<ScheduleResponse>> cancelSchedule(@PathVariable Integer scheduleId) {
        ScheduleResponse data = scheduleService.cancelSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Đã huỷ lịch khởi hành", data));
    }

    @PatchMapping("/admin/schedules/{scheduleId}/reopen")
    public ResponseEntity<ApiResponse<ScheduleResponse>> reopenSchedule(@PathVariable Integer scheduleId) {
        ScheduleResponse data = scheduleService.reopenSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Đã mở lại lịch khởi hành", data));
    }

    @DeleteMapping("/admin/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable Integer scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Xoá lịch khởi hành thành công", null));
    }

    @GetMapping("/admin/schedules")
    public ResponseEntity<ApiResponse<Page<ScheduleResponse>>> getSchedulesForAdmin(
            @ModelAttribute ScheduleFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "departureDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Page<ScheduleResponse> data = scheduleService.getAllSchedulesForAdmin(filter, buildPageable(page, size, sortBy, sortDir));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lịch khởi hành thành công", data));
    }

    @GetMapping("/admin/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getScheduleByIdForAdmin(@PathVariable Integer scheduleId) {
        ScheduleResponse data = scheduleService.getScheduleByIdForAdmin(scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết lịch khởi hành thành công", data));
    }

    /** Lịch sắp tới (chưa huỷ) của 1 tour — trang chi tiết tour / chọn ngày đặt tour */
    @GetMapping("/public/tours/{tourId}/schedules")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getUpComingSchedulesOfTour(@PathVariable Integer tourId) {
        List<ScheduleResponse> data = scheduleService.getUpcomingSchedulesOfTour(tourId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch khởi hành thành công", data));
    }

    private static Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
        if (!ADMIN_SORTS.contains(sortBy)) sortBy = "departureDate";
        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        sort = sort.and(Sort.by("scheduleId").descending());   // trùng giá trị → thứ tự vẫn cố định khi phân trang
        return PageRequest.of(page, size, sort);
    }
}