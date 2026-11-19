package com.example.tour.report.service;

import com.example.tour.report.dto.response.RevenueReportResponse;

import java.time.LocalDate;

public interface RevenueReportService {

    RevenueReportResponse getRevenueReport(LocalDate from, LocalDate to);

    /** File .xlsx gồm 3 sheet: Tổng quan, Theo thời gian, Top tour */
    byte[] exportRevenueReport(LocalDate from, LocalDate to);
}
