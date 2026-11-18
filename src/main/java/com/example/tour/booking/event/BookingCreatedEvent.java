package com.example.tour.booking.event;

import com.example.tour.booking.dto.response.BookingResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Bắn ra khi khách đặt tour thành công (đã giữ chỗ).
 * Mang theo BookingResponse đã dựng sẵn TRONG transaction → listener chạy luồng khác
 * không phải đọc entity lazy (tránh LazyInitializationException).
 */
@Getter
@AllArgsConstructor
public class BookingCreatedEvent {
    private final BookingResponse booking;
}
