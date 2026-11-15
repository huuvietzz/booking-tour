package com.example.tour.booking.service;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.booking.dto.request.*;
import com.example.tour.booking.dto.response.BookingCardResponse;
import com.example.tour.booking.dto.response.BookingResponse;
import com.example.tour.booking.dto.response.BookingAdminRowResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {

    // ===== USER =====
    BookingResponse createBooking(BookingRequest request, Integer userId);

    Page<BookingCardResponse> getMyBookings(Integer userId, BookingStatus bookingStatus, PaymentStatus paymentStatus, Pageable pageable);

    // ===== ADMIN =====
    Page<BookingAdminRowResponse> getBookingsForAdmin(BookingFilterRequest filter, Pageable pageable);

    BookingResponse getBookingByIdForAdmin(Integer bookingId);

    BookingResponse updateBooking(BookingUpdateRequest request, Integer bookingId);

    BookingResponse confirmBooking(Integer bookingId);

    BookingResponse cancelBooking(Integer bookingId);

    BookingResponse completeBooking(Integer bookingId);
}
