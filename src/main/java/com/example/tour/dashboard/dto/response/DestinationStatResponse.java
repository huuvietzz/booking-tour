package com.example.tour.dashboard.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DestinationStatResponse {
    private String name;
    private long guests;
    private long bookings;
    private BigDecimal revenue;
}
