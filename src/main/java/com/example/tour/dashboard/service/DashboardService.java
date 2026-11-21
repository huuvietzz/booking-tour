package com.example.tour.dashboard.service;

import com.example.tour.dashboard.constant.DashboardPeriod;
import com.example.tour.dashboard.dto.response.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboard(DashboardPeriod period);
}
