package com.example.tour.payment.dto.response;

import com.example.tour.payment.constant.PaymentMethod;
import com.example.tour.payment.constant.TransactionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 1 dòng trong bảng Quản lý thanh toán */
@Getter
@Builder
public class PaymentAdminRowResponse {
    private Integer paymentId;
    private String referenceCode;        // Mã GD ngân hàng (FT...)
    private LocalDateTime paymentDate;
    private Integer bookingId;
    private String bookingCode;
    private String customerName;
    private BigDecimal amount;
    private PaymentMethod method;
    private String gateway;
    private TransactionStatus transactionStatus;
}
