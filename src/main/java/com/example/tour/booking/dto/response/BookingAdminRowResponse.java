package com.example.tour.booking.dto.response;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class BookingAdminRowResponse {
    private Integer bookingId;
    private String bookingCode;
    private String contactName;
    private String contactPhone;
    private String tourName;
    private String scheduleCode;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private Integer totalPeople;
    private BigDecimal totalAmount;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
}
