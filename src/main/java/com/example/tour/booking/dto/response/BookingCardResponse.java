package com.example.tour.booking.dto.response;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 1 thẻ đơn trong tab "Đơn đặt tour" của khách (profile.html) */
@Getter
@Builder
public class BookingCardResponse {
    private Integer bookingId;
    private String bookingCode;
    private LocalDateTime bookingDate;

    // Tour: tên + ảnh + link sang trang chi tiết tour
    private Integer tourId;
    private String tourName;
    private String thumbnailUrl;

    // Lịch khởi hành
    private String scheduleCode;
    private LocalDate departureDate;
    private LocalDate returnDate;

    // Số khách: "2 người lớn, 1 trẻ em"
    private Integer totalAdults;
    private Integer totalChildren;
    private Integer totalToddlers;

    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private BookingStatus bookingStatus;
}
