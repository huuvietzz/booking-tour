package com.example.tour.report.service;

import com.example.tour.report.dto.response.TourReportResponse;

import java.time.LocalDate;

public interface TourReportService {
    TourReportResponse getTourReport(LocalDate from, LocalDate to);
}
