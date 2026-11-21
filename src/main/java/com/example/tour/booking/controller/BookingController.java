package com.example.tour.booking.controller;

import com.alibaba.excel.EasyExcel;
import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.booking.dto.export.BookingExcelModel;
import com.example.tour.booking.dto.request.BookingFilterRequest;
import com.example.tour.booking.dto.request.BookingRequest;
import com.example.tour.booking.dto.request.BookingUpdateRequest;
import com.example.tour.booking.dto.response.*;
import com.example.tour.booking.service.BookingPdfService;
import com.example.tour.booking.service.BookingService;
import com.example.tour.common.dto.ApiResponse;
import com.example.tour.common.util.SecurityUtils;
import com.example.tour.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
            "totalPeople", "totalPeople",
            "departureDate", "schedule.departureDate",
            "contactName", "contactName");

    private final BookingPdfService bookingPdfService;
    private final BookingService bookingService;

    // ===== KHÁCH =====
    @PostMapping("/public/bookings")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@RequestBody @Valid BookingRequest request,
                                                               @AuthenticationPrincipal User user) {
        BookingResponse data = bookingService.createBooking(request, user == null ? null : user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đặt tour thành công", data));
    }

    @GetMapping("/public/bookings/{code}/payment-status")
    public ResponseEntity<ApiResponse<BookingPaymentStatusResponse>> getPaymentStatus(@PathVariable String code) {
        BookingPaymentStatusResponse data = bookingService.getPaymentStatus(code);
        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái thanh toán thành công", data));
    }

    @GetMapping("/public/bookings/{code}/payment")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingForPayment(
            @PathVariable String code,
            @RequestParam(required = false) String token) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn thành công",
                bookingService.getBookingForPayment(code, token, SecurityUtils.getCurrentUserIdOrNull())));
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

    @GetMapping("/users/bookings/my/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getMyBookingById(@AuthenticationPrincipal User user,
                                                                         @PathVariable Integer bookingId) {
        BookingResponse data = bookingService.getMyBookingById(user.getUserId(), bookingId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn đặt thành công", data));
    }

    /** Khách tải phiếu PDF của đơn mình: kiểm tra chủ đơn TRƯỚC, rồi dùng lại service PDF của admin */
    @GetMapping("/users/bookings/my/{bookingId}/pdf")
    public ResponseEntity<byte[]> exportMyBookingPdf(@AuthenticationPrincipal User user,
                                                     @PathVariable Integer bookingId) {
        BookingResponse booking = bookingService.getMyBookingById(user.getUserId(), bookingId);   // không phải đơn của mình → 404
        byte[] pdf = bookingPdfService.exportBookingPdf(bookingId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("Phieu_dat_tour_" + booking.getBookingCode() + ".pdf", StandardCharsets.UTF_8).build().toString())
                .body(pdf);
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
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đơn đặt thành công", data));
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

    @GetMapping("/admin/bookings/stats")
    public ResponseEntity<ApiResponse<BookingStatsResponse>> getBookingStats(
            @RequestParam(defaultValue = "7") int upcomingDays) {
        BookingStatsResponse data = bookingService.getBookingStats(Math.min(Math.max(upcomingDays, 1), 90));
        return ResponseEntity.ok(ApiResponse.success("Lấy số liệu đơn đặt thành công", data));
    }

    /**
     * Xuất Excel theo ĐÚNG bộ lọc + thứ tự sắp xếp đang xem trên trang admin.
     * Trả về file (không bọc ApiResponse) → kiểu void, ghi thẳng vào response.
     */
    @GetMapping("/admin/bookings/export")
    public void exportBookings(BookingFilterRequest filter,
                               @RequestParam(defaultValue = "createdAt,desc") String sort,
                               HttpServletResponse response) throws IOException {
        // Lấy dữ liệu TRƯỚC khi ghi file: lỗi (vd quá nhiều dòng) vẫn trả JSON lỗi bình thường qua GlobalExceptionHandler
        List<BookingExcelModel> rows = bookingService.exportBookings(filter, buildSort(sort));

        String fileName = "Don_dat_tour_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));

        EasyExcel.write(response.getOutputStream(), BookingExcelModel.class)
                .autoCloseStream(false)          // để Spring tự đóng stream của response
                .sheet("Đơn đặt tour")
                .doWrite(rows);
    }

    /** Phiếu xác nhận đặt tour của 1 đơn (PDF). Không có đơn → JSON lỗi BOOKING_NOT_FOUND như bình thường */
    @GetMapping("/admin/bookings/{bookingId}/pdf")
    public ResponseEntity<byte[]> exportBookingPdf(@PathVariable Integer bookingId) {
        byte[] pdf = bookingPdfService.exportBookingPdf(bookingId);
        // FE tự đặt tên file theo mã đơn (Phieu_dat_tour_BK....pdf); tên ở đây chỉ để dự phòng
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("Phieu_dat_tour_" + bookingId + ".pdf", StandardCharsets.UTF_8).build().toString())
                .body(pdf);
    }


    // Sửa buildPageable dùng chung buildSort (trang danh sách + xuất Excel sắp xếp giống nhau)
    private static PageRequest buildPageable(int page, int size, String sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), buildSort(sort));
    }

    private static Sort buildSort(String sort) {
        String[] parts = sort.split(",");
        String field = SORTS.getOrDefault(parts[0].trim(), "createdAt");
        Sort.Direction dir = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field).and(Sort.by(Sort.Direction.DESC, "bookingId"));
    }
}
