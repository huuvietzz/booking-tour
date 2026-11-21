package com.example.tour.dashboard.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.dashboard.constant.DashboardPeriod;
import com.example.tour.dashboard.dto.response.DashboardResponse;
import com.example.tour.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    // GET /api/admin/dashboard?period=WEEK|MONTH|YEAR (mặc định MONTH = 30 ngày)
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestParam(defaultValue = "MONTH") DashboardPeriod period) {
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu bảng điều khiển thành công",
                dashboardService.getDashboard(period)));
    }
}
