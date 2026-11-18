package com.example.tour.booking.dto.request;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BookingFilterRequest {
    /** Tìm theo mã đơn, tên / email / SĐT người liên hệ */
    private String keyword;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private Integer tourId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureFrom;      // ngày khởi hành từ
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureTo;        // ngày khởi hành đến

    private BigDecimal minAmount;   // tổng tiền từ
    private BigDecimal maxAmount;   // tổng tiền đến
}
