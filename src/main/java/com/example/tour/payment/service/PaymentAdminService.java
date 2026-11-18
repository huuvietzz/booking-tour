package com.example.tour.payment.service;

import com.example.tour.payment.dto.request.PaymentFilterRequest;
import com.example.tour.payment.dto.response.PaymentAdminRowResponse;
import com.example.tour.payment.dto.response.PaymentDetailResponse;
import com.example.tour.payment.dto.response.PaymentStatsResponse;
import org.springframework.data.domain.Page;

public interface PaymentAdminService {

    Page<PaymentAdminRowResponse> getPayments(PaymentFilterRequest filter, int page, int size, String sort);

    PaymentDetailResponse getPaymentDetail(Integer paymentId);

    PaymentStatsResponse getStats();
}
