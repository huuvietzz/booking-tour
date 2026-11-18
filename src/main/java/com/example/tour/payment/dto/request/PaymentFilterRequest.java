package com.example.tour.payment.dto.request;

import com.example.tour.payment.constant.PaymentMethod;
import com.example.tour.payment.constant.TransactionStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PaymentFilterRequest {

    /** Mã đơn, tên/SĐT khách, mã tham chiếu FT..., mã giao dịch */
    private String keyword;

    private TransactionStatus transactionStatus;

    private PaymentMethod method;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}
