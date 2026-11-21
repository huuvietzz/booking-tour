package com.example.tour.dashboard.dto.response;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 1 dòng "Đơn đặt tour gần đây". Constructor dùng trong JPQL select new */
@Getter
@AllArgsConstructor
public class RecentBookingResponse {
    private Integer bookingId;
    private String bookingCode;
    private String contactName;
    private String tourName;
    private LocalDate departureDate;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
}
