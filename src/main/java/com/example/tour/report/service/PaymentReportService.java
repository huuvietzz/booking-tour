package com.example.tour.report.service;

import com.example.tour.report.dto.response.PaymentReportResponse;

import java.time.LocalDate;

public interface PaymentReportService {
    PaymentReportResponse getPaymentReport(LocalDate from, LocalDate to);
}
