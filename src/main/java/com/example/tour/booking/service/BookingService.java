//package com.example.tour.booking.service;
//
//import com.example.tour.booking.constant.BookingPaymentStatus;
//import com.example.tour.booking.constant.BookingStatus;
//import com.example.tour.booking.dto.request.BookingRequest;
//import com.example.tour.booking.dto.response.BookingDetailResponse;
//import com.example.tour.booking.dto.response.BookingResponse;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//
//import java.io.IOException;
//import java.time.LocalDate;
//
//public interface BookingService {
//    /**
//     * Khách đặt tour. Kiểm tra lịch còn hiệu lực, còn đủ chỗ, chốt giá
//     * tại thời điểm đặt, tạo danh sách hành khách và trừ số chỗ của lịch.
//     */
//    BookingResponse createBooking(int userId, BookingRequest request);
//
//    /** Danh sách đơn cho admin, có phân trang, lọc và tìm kiếm. */
//    Page<BookingResponse> getAllBookingsForAdmin(String keyword,
//                                                 BookingStatus bookingStatus,
//                                                 BookingPaymentStatus bookingPaymentStatus,
//                                                 LocalDate fromDate,
//                                                 LocalDate toDate,
//                                                 Pageable pageable);
//
//    /** Chi tiết một đơn, kèm danh sách hành khách. */
//    BookingDetailResponse getBookingById(int bookingId);
//
//    /** Admin xác nhận đơn: PENDING → CONFIRMED. */
//    BookingResponse confirmBooking(int bookingId);
//
//    /** Admin xác nhận hoàn thành đơn: CONFIRMED → COMPLETED.*/
//    BookingResponse completeBooking(int bookingId);
//
//    /**
//     * Hủy đơn và hoàn lại số chỗ cho lịch khởi hành.
//     * Lịch đang FULL sẽ được mở lại thành AVAILABLE.
//     */
//    BookingResponse cancelBooking(int bookingId, String reason);
//
//    /**
//     * Xuất danh sách đơn đặt ra Excel, dùng đúng bộ lọc đang áp dụng trên bảng.
//     * Không phân trang — xuất toàn bộ kết quả khớp bộ lọc.
//     */
//    void exportBookingsToExcel(HttpServletResponse response,
//                               String keyword,
//                               BookingStatus bookingStatus,
//                               BookingPaymentStatus bookingPaymentStatus,
//                               LocalDate fromDate,
//                               LocalDate toDate) throws IOException;
//
//
//    /**
//     * Xuất danh sách hành khách của một đơn ra Excel — dùng để in giao cho HDV.
//     * Người đại diện đoàn xếp đầu tiên.
//     */
//    void exportParticipantsToExcel(HttpServletResponse response, int bookingId) throws IOException;
//}
