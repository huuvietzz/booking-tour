package com.example.tour.booking.controller;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.booking.dto.request.BookingFilterRequest;
import com.example.tour.booking.dto.request.BookingRequest;
import com.example.tour.booking.dto.request.BookingUpdateRequest;
import com.example.tour.booking.dto.response.BookingAdminRowResponse;
import com.example.tour.booking.dto.response.BookingCardResponse;
import com.example.tour.booking.dto.response.BookingResponse;
import com.example.tour.booking.service.BookingService;
import com.example.tour.common.dto.ApiResponse;
import com.example.tour.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BookingController {

    /** Cột được phép sắp xếp: tên FE gửi → field entity (chặn sort theo field lạ gây lỗi 500) */
    private static final Map<String, String> SORTS = Map.of(
            "createdAt", "createdAt",
            "bookingCode", "bookingCode",
            "totalAmount", "totalAmount",
            "departureDate", "schedule.departureDate",
            "contactName", "contactName");

    private final BookingService bookingService;

    // ===== KHÁCH =====
    @PostMapping("/public/bookings")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@RequestBody @Valid BookingRequest request,
                                                               @AuthenticationPrincipal User user) {
        BookingResponse data = bookingService.createBooking(request, user == null ? null : user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đặt tour thành công", data));
    }

    // ===== USER =====
    @GetMapping("/users/bookings/my")
    public ResponseEntity<ApiResponse<Page<BookingCardResponse>>> getMyBookings(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) BookingStatus bookingStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "bookingId")));
        Page<BookingCardResponse> data = bookingService.getMyBookings(user.getUserId(), bookingStatus, paymentStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn đặt thành công", data));
    }

    // ======== ADMIN ========
    @GetMapping("/admin/bookings")
    public ResponseEntity<ApiResponse<Page<BookingAdminRowResponse>>> getBookingsForAdmin(
            BookingFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Page<BookingAdminRowResponse> data = bookingService.getBookingsForAdmin(filter, buildPageable(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn đặt thành công", data));
    }

    @GetMapping("/admin/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByIdForAdmin(@PathVariable Integer bookingId) {
        BookingResponse data = bookingService.getBookingByIdForAdmin(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn đặt thành công", data));
    }

    @PutMapping("/admin/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(@RequestBody @Valid BookingUpdateRequest request,
                                                                      @PathVariable Integer bookingId) {
        BookingResponse data = bookingService.updateBooking(request, bookingId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn đặt thành công", data));
    }

    @PatchMapping("/admin/bookings/{bookingId}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(@PathVariable Integer bookingId) {
        BookingResponse data = bookingService.confirmBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận thanh toán", data));
    }

    @PatchMapping("/admin/bookings/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Integer bookingId) {
        BookingResponse data = bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Huỷ đơn thành công", data));
    }

    @PatchMapping("/admin/bookings/{bookingId}/complete")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(@PathVariable Integer bookingId) {
        BookingResponse data = bookingService.completeBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Đã hoàn thành đơn", data));
    }

    private static PageRequest buildPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        String field = SORTS.getOrDefault(parts[0].trim(), "createdAt");
        Sort.Direction dir = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(dir, field).and(Sort.by(Sort.Direction.DESC, "bookingId")));
    }

}
