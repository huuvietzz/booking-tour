//package com.example.tour.booking.controller;
//
//import com.example.tour.booking.constant.BookingPaymentStatus;
//import com.example.tour.booking.constant.BookingStatus;
//import com.example.tour.booking.dto.request.BookingRequest;
//import com.example.tour.booking.dto.request.CancelBookingRequest;
//import com.example.tour.booking.dto.response.BookingDetailResponse;
//import com.example.tour.booking.dto.response.BookingResponse;
//import com.example.tour.booking.service.BookingService;
//import com.example.tour.common.dto.ApiResponse;
//import jakarta.servlet.http.HttpServletResponse;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//import java.time.LocalDate;
//
//@RestController
//@RequestMapping("/api")
//@RequiredArgsConstructor
//public class BookingController {
//    private final BookingService bookingService;
//
//    // Khách đặt tour
//    @PostMapping("/bookings")
//    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
//            @RequestParam int userId,
//            @RequestBody @Valid BookingRequest request) {
//        BookingResponse data = bookingService.createBooking(userId, request);
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ApiResponse.created("Đặt tour thành công", data));
//    }
//
//    // Thiếu: lấy tất cả đơn hàng cho user
//
//    @GetMapping("/admin/bookings")
//    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookings (
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "bookingDate") String sortBy,
//            @RequestParam(defaultValue = "desc") String sortDir,
//            @RequestParam(required = false) String keyword,
//            @RequestParam(required = false) BookingStatus bookingStatus,
//            @RequestParam(required = false) BookingPaymentStatus bookingPaymentStatus,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate fromDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
//    ) {
//        String actualSortBy = switch (sortBy) {
//            case "customerName" -> "user.fullName";
//            case "tourName" -> "tourSchedule.tour.tourName";
//            case "departureDate" -> "tourSchedule.departureDate";
//            case "finalAmount"   -> "finalAmount";
//            case "bookingCode"   -> "bookingCode";
//            default              -> "bookingDate";
//        };
//
//        Sort sort = sortDir.equalsIgnoreCase("asc")
//                ? Sort.by(actualSortBy).ascending()
//                : Sort.by(actualSortBy).descending();
//
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<BookingResponse> data = bookingService.getAllBookingsForAdmin(
//                keyword, bookingStatus, bookingPaymentStatus, fromDate, toDate, pageable);
//        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn đặt thành công", data));
//    }
//
//    @GetMapping("/admin/bookings/{bookingId}")
//    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingById(@PathVariable int bookingId) {
//        BookingDetailResponse data = bookingService.getBookingById(bookingId);
//        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn đặt thành công", data));
//    }
//
//    @PatchMapping("/admin/bookings/{bookingId}/confirm")
//    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(@PathVariable int bookingId) {
//        BookingResponse data = bookingService.confirmBooking(bookingId);
//        return ResponseEntity.ok(ApiResponse.success("Xác nhận đơn đặt thành công",data));
//    }
//
//    @PatchMapping("/admin/bookings/{bookingId}/complete")
//    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(@PathVariable int bookingId) {
//        BookingResponse data = bookingService.completeBooking(bookingId);
//        return ResponseEntity.ok(ApiResponse.success("Hoàn thành đơn đặt thành công", data));
//    }
//
//    @PatchMapping("/admin/bookings/{bookingId}/cancel")
//    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
//            @PathVariable int bookingId,
//            @RequestBody @Valid CancelBookingRequest request
//    ) {
//        BookingResponse data = bookingService.cancelBooking(bookingId, request.getReason());
//        return ResponseEntity.ok(ApiResponse.success("Hủy đơn đặt thành công", data));
//    }
//
//    // Xuất file excel
//    @GetMapping("/admin/bookings/export")
//    public void exportBookingsToExcel(
//            HttpServletResponse response,
//            @RequestParam(required = false) String keyword,
//            @RequestParam(required = false) BookingStatus bookingStatus,
//            @RequestParam(required = false) BookingPaymentStatus bookingPaymentStatus,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
//    ) throws IOException {
//        bookingService.exportBookingsToExcel(response, keyword, bookingStatus, bookingPaymentStatus, fromDate, toDate);
//    }
//
//    // Xuất danh sách hành khách của một đơn — dùng để in giao HDV
//    @GetMapping("/admin/bookings/{bookingId}/participants/export")
//    public void exportParticipantsToExcel(HttpServletResponse response,
//                                          @PathVariable int bookingId) throws IOException {
//        bookingService.exportParticipantsToExcel(response, bookingId);
//    }
//}
