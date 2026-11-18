package com.example.tour.payment.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/** Payload SePay gửi sang (JSON). Tên field khớp đúng tài liệu SePay. */
@Getter @Setter @ToString
public class SepayWebhookRequest {
    private Long id;                 // ID giao dịch trên SePay -> dùng chống trùng
    private String gateway;          // "MBBank"
    private String transactionDate;  // "2026-11-17 21:57:00"
    private String accountNumber;
    private String subAccount;
    private String code;             // mã thanh toán SePay đã tách: "BK26111600018" (có thể null)
    private String content;          // nội dung CK gốc
    private String transferType;     // "in" | "out"
    private String description;
    private BigDecimal transferAmount;
    private String referenceCode;    // "FT26321557012718"
    private BigDecimal accumulated;
}
