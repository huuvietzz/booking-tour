package com.example.tour.payment.dto.response;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.payment.constant.PaymentMethod;
import com.example.tour.payment.constant.TransactionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Popup "Xem chi tiết" giao dịch */
@Getter
@Builder
public class PaymentDetailResponse {

    // ---- Giao dịch ----
    private Integer paymentId;
    private String transactionId;        // ID giao dịch trên SePay
    private String referenceCode;
    private BigDecimal amount;
    private PaymentMethod method;
    private String gateway;
    private TransactionStatus transactionStatus;
    private LocalDateTime paymentDate;

    // ---- Đơn ----
    private Integer bookingId;
    private String bookingCode;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String tourName;
    private String scheduleCode;
    private LocalDate departureDate;
}
