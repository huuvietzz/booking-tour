package com.example.tour.booking.dto.response;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class BookingCardResponse {
    private Integer bookingId;
    private String tourName;
    private String bookingCode;
    private String scheduleCode;
    private BigDecimal totalAmount;
    private LocalDateTime bookingDate;
    private PaymentStatus paymentStatus;
    private BookingStatus bookingStatus;
}
