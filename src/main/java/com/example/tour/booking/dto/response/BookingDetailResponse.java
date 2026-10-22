//package com.example.tour.booking.dto.response;
//
//import com.example.tour.booking.constant.*;
//import com.example.tour.common.constant.Gender;
//import com.example.tour.tour.constant.ScheduleStatus;
//import lombok.Builder;
//import lombok.Getter;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Getter
//@Builder
//public class BookingDetailResponse {
//    private int bookingId;
//    private String bookingCode;
//    private LocalDateTime bookingDate;
//    private BookingStatus bookingStatus;
//    private BookingPaymentStatus bookingPaymentStatus;
//
//    // ===== Khách đặt =====
//    private int userId;
//    private String customerName;
//    private String customerEmail;
//    private String customerPhone;
//
//    // ===== Chuyến đi =====
//    private int scheduleId;
//    private String tourCode;
//    private String tourName;
//    private LocalDate departureDate;
//    private LocalDate returnDate;
//    private int durationDays;
//    private int durationNights;
//    private ScheduleStatus scheduleStatus;
//
//    // ===== Số khách & giá chốt tại thời điểm đặt =====
//    private int totalAdults;
//    private int totalChildren;
//    private int totalInfants;
//    private BigDecimal adultPriceAtBooking;
//    private BigDecimal childPriceAtBooking;
//    private BigDecimal infantPriceAtBooking;
//
//    private BigDecimal singleRoomSurchargeAtBooking;
//    private int singleRoomCount;
//
//    // ===== Tiền =====
//    private BigDecimal totalAmount;
//    private BigDecimal discountAmount;
//    private BigDecimal pointsDiscountAmount;
//    private int pointsRedeemed;
//    private BigDecimal finalAmount;
//
//    private String specialRequests;
//    private String cancellationReason;
//    private LocalDateTime cancelledAt;
//    private LocalDateTime confirmedAt;
//    private LocalDateTime completedAt;
//
//    private List<ParticipantResponse> participants;
//
//    @Getter
//    @Builder
//    public static class ParticipantResponse {
//        private int participantId;
//        private String fullName;
//        private LocalDate dateOfBirth;
//        private Gender gender;
//        private String nationality;
//        private IdentityType identityType;
//        private String identityNumber;
//        private ParticipantType participantType;
//        private boolean primary;
//        private boolean singleRoom;
//        private DocumentStatus documentStatus;
//    }
//}