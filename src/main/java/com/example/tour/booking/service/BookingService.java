package com.example.tour.booking.service;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.booking.dto.export.BookingExcelModel;
import com.example.tour.booking.dto.request.*;
import com.example.tour.booking.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface BookingService {

    // ===== USER =====
    BookingResponse createBooking(BookingRequest request, Integer userId);

    Page<BookingCardResponse> getMyBookings(Integer userId, BookingStatus bookingStatus, PaymentStatus paymentStatus, Pageable pageable);

    BookingResponse getMyBookingById(Integer userId, Integer bookingId);

    // ===== ADMIN =====
    Page<BookingAdminRowResponse> getBookingsForAdmin(BookingFilterRequest filter, Pageable pageable);

    BookingResponse getBookingByIdForAdmin(Integer bookingId);

    BookingResponse updateBooking(BookingUpdateRequest request, Integer bookingId);

    BookingResponse confirmBooking(Integer bookingId);

    BookingResponse cancelBooking(Integer bookingId);

    boolean expireUnpaidBooking(Integer bookingId);

    BookingResponse completeBooking(Integer bookingId);

    BookingStatsResponse getBookingStats(int upcomingDays);

    List<BookingExcelModel> exportBookings(BookingFilterRequest filter, Sort sort);

    BookingPaymentStatusResponse getPaymentStatus(String bookingCode);

    BookingResponse getBookingForPayment(String bookingCode, String token, Integer userId);
}
