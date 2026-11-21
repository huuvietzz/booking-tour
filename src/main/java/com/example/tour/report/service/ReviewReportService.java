package com.example.tour.report.service;

import com.example.tour.report.dto.response.ReviewReportResponse;

import java.time.LocalDate;

public interface ReviewReportService {
    ReviewReportResponse getReviewReport(LocalDate from, LocalDate to);
}
