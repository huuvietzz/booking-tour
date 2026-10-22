package com.example.tour.tour.dto.response;

import com.example.tour.tour.constant.ScheduleStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    private Integer scheduleId;
    private String scheduleCode;

    private Integer tourId;
    private String tourCode;
    private String tourName;
    private String duration;

    private LocalDate departureDate;
    private LocalDate returnDate;

    private Integer totalSlots;
    private Integer availableSlots;
    private Integer bookedSlots;     // = totalSlots - availableSlots

    private BigDecimal adultPrice;
    private BigDecimal childPrice;
    private BigDecimal singleRoomSupplement;
    private String transport;

    private ScheduleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}