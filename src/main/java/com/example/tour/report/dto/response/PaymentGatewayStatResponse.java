package com.example.tour.report.dto.response;

import com.example.tour.payment.constant.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** Theo ngân hàng / cổng: MBBank (SePay), NCB / VCB... (VNPAY) */
@Getter
@Builder
public class PaymentGatewayStatResponse {
    private PaymentMethod method;
    private String gateway;
    private long transactions;
    private BigDecimal amount;
}
