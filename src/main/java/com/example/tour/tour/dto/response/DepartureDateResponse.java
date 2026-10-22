package com.example.tour.tour.dto.response;

import com.example.tour.tour.constant.ScheduleStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartureDateResponse {
    private Integer scheduleId;
    private LocalDate departureDate;
    private ScheduleStatus status;
}
