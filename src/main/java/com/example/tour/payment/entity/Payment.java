package com.example.tour.payment.entity;

import com.example.tour.booking.entity.Booking;
import com.example.tour.common.entity.BaseEntity;
import com.example.tour.payment.constant.PaymentMethod;
import com.example.tour.payment.constant.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payments_method_txn",
                columnNames = {"method", "transaction_id"}))
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // SePay: trường "id" của webhook; VNPAY: vnp_TransactionNo — dùng chống xử lý trùng
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false)
    private TransactionStatus transactionStatus;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "gateway", length = 50)
    private String gateway;            // MBBank, Vietcombank...

    @Column(name = "reference_code", length = 100)
    private String referenceCode;
}
