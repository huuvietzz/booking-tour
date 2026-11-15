package com.example.tour.booking.dto.response;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.ParticipantType;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.common.constant.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BookingResponse {
    private Integer bookingId;
    private String bookingCode;
    private Integer userId;
    private Integer promotionId;
    private String promotionCode;

    // Thông tin liên lạc
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String contactAddress;

    // Chi tiết chi phí
    private Integer totalAdults;
    private Integer totalChildren;
    private Integer totalToddlers;
    private BigDecimal adultPrice;
    private BigDecimal childPrice;
    private BigDecimal toddlerPrice;
    private BigDecimal singleRoomSupplement;
    private Integer singleRoomCount;
    private BigDecimal tempAmount; // Tạm tính (trước giảm giá)
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private String note;

    // Tóm tắt tour
    private Integer tourId;
    private String tourName;
    private String thumbnailUrl;
    private Integer scheduleId;
    private String scheduleCode;


    // Thông tin phương tiện
    private LocalDate departureDate;
    private LocalDate returnDate;
    private String transport;

    // Thông tin hành khách
    private List<Participant> participants;
    @Getter
    @AllArgsConstructor
    public static class Participant {
        private Integer participantId;
        private String fullName;
        private LocalDate dateOfBirth;
        private Gender gender;
        private ParticipantType participantType;
        private String phoneNumber;
        private boolean singleRoom;
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
