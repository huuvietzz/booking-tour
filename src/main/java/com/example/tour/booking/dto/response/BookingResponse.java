//package com.example.tour.booking.dto.response;
//
//import com.example.tour.booking.constant.BookingPaymentStatus;
//import com.example.tour.booking.constant.BookingStatus;
//import lombok.Builder;
//import lombok.Getter;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//@Getter
//@Builder
//public class BookingResponse {
//    private int bookingId;
//    private String bookingCode;
//    private LocalDateTime bookingDate;
//
//    // Khách hàng
//    private int userId;
//    private String customerName;
//    private String customerPhone;
//
//    // Chuyến đi
//    private int scheduleId;
//    private String tourCode;
//    private String tourName;
//    private LocalDate departureDate;
//    private LocalDate returnDate;
//
//    // Số khách
//    private int totalAdults;
//    private int totalChildren;
//    private int totalInfants;
//
//    // Tiền
//    private BigDecimal totalAmount;
//    private BigDecimal discountAmount;
//    private BigDecimal finalAmount;
//
//    private BookingStatus bookingStatus;
//    private BookingPaymentStatus bookingPaymentStatus;
//}