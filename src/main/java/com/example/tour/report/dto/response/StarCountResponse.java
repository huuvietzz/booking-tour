package com.example.tour.report.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StarCountResponse {
    private int star;        // 5 → 1
    private long count;
    private double percent;
}
