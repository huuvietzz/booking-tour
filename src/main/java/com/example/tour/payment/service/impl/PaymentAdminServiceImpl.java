package com.example.tour.payment.service.impl;

import com.example.tour.booking.entity.Booking;
import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.payment.constant.TransactionStatus;
import com.example.tour.payment.dto.request.PaymentFilterRequest;
import com.example.tour.payment.dto.response.PaymentAdminRowResponse;
import com.example.tour.payment.dto.response.PaymentDetailResponse;
import com.example.tour.payment.dto.response.PaymentStatsResponse;
import com.example.tour.payment.entity.Payment;
import com.example.tour.payment.repository.PaymentRepository;
import com.example.tour.payment.service.PaymentAdminService;
import com.example.tour.payment.specification.PaymentSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentAdminServiceImpl implements PaymentAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    /** Chỉ cho sort các cột này (key FE gửi → field entity) */
    private static final Map<String, String> SORTS = Map.of(
            "paymentDate", "paymentDate",
            "amount", "amount"
    );

    private final PaymentRepository paymentRepository;

    @Override
    public Page<PaymentAdminRowResponse> getPayments(PaymentFilterRequest filter, int page, int size, String sort) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), buildSort(sort));
        return paymentRepository.findAll(PaymentSpecification.filter(filter), pageable)
                .map(PaymentAdminServiceImpl::toRow);
    }

    @Override
    public PaymentDetailResponse getPaymentDetail(Integer paymentId) {
        Payment p = paymentRepository.findWithBookingByPaymentId(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        Booking b = p.getBooking();

        return PaymentDetailResponse.builder()
                .paymentId(p.getPaymentId())
                .transactionId(p.getTransactionId())
                .referenceCode(p.getReferenceCode())
                .amount(p.getAmount())
                .method(p.getMethod())
                .gateway(p.getGateway())
                .transactionStatus(p.getTransactionStatus())
                .paymentDate(p.getPaymentDate())
                .bookingId(b.getBookingId())
                .bookingCode(b.getBookingCode())
                .bookingStatus(b.getBookingStatus())
                .paymentStatus(b.getPaymentStatus())
                .contactName(b.getContactName())
                .contactPhone(b.getContactPhone())
                .contactEmail(b.getContactEmail())
                .tourName(b.getSchedule().getTour().getTourName())
                .scheduleCode(b.getSchedule().getScheduleCode())
                .departureDate(b.getSchedule().getDepartureDate())
                .build();
    }

    @Override
    public PaymentStatsResponse getStats() {
        return PaymentStatsResponse.builder()
                .total(paymentRepository.count())
                .success(paymentRepository.countByTransactionStatus(TransactionStatus.SUCCESS))
                .pending(paymentRepository.countByTransactionStatus(TransactionStatus.PENDING))
                .failed(paymentRepository.countByTransactionStatus(TransactionStatus.FAILED))
                .successAmount(paymentRepository.sumAmountByStatus(TransactionStatus.SUCCESS))
                .build();
    }

    /** sort = "amount,asc" | "paymentDate,desc"; sai/thiếu → mới nhất trước */
    private Sort buildSort(String sort) {
        Sort fallback = Sort.by(Sort.Direction.DESC, "paymentDate").and(Sort.by(Sort.Direction.DESC, "paymentId"));
        if (!StringUtils.hasText(sort)) return fallback;

        String[] parts = sort.split(",");
        String field = SORTS.get(parts[0].trim());
        if (field == null) return fallback;

        Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field).and(Sort.by(Sort.Direction.DESC, "paymentId"));   // ổn định khi trùng giá trị
    }

    private static PaymentAdminRowResponse toRow(Payment p) {
        Booking b = p.getBooking();
        return PaymentAdminRowResponse.builder()
                .paymentId(p.getPaymentId())
                .referenceCode(p.getReferenceCode())
                .paymentDate(p.getPaymentDate())
                .bookingId(b.getBookingId())
                .bookingCode(b.getBookingCode())
                .customerName(b.getContactName())
                .amount(p.getAmount())
                .method(p.getMethod())
                .gateway(p.getGateway())
                .transactionStatus(p.getTransactionStatus())
                .build();
    }
}
