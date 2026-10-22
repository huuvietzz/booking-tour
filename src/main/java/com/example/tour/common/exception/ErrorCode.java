package com.example.tour.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ========== AUTH ==========
    EMAIL_ALREADY_EXISTS(400, "Email đã được sử dụng", HttpStatus.BAD_REQUEST),
    EMAIL_CHANGE_NOT_ALLOWED(403, "Bạn không có quyền đổi email", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS(401, "Tài khoản hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(403, "Tài khoản đã bị khóa", HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_FOUND_OR_INACTIVE(404,"Tài khoản không tồn tại hoặc chưa được kích hoạt", HttpStatus.NOT_FOUND),
    EMAIL_NOT_VERIFIED(403, "Tài khoản chưa xác thực email. Vui lòng kiểm tra hộp thư.", HttpStatus.FORBIDDEN),
    UNAUTHORIZED(401, "Vui lòng đăng nhập", HttpStatus.UNAUTHORIZED),
    CURRENT_PASSWORD_INCORRECT(400, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    ACCOUNT_LOCKED(403, "Tài khoản đã bị khoá. Vui lòng liên hệ quản trị viên", HttpStatus.FORBIDDEN),

    // ========== TOKEN ==========
    INVALID_TOKEN(400, "Token không hợp lệ", HttpStatus.BAD_REQUEST),
    TOKEN_EXPIRED(400, "Token đã hết hạn", HttpStatus.BAD_REQUEST),
    TOKEN_ALREADY_USED(400, "Token đã được sử dụng", HttpStatus.BAD_REQUEST),
    PHONE_ALREADY_EXISTS(400, "Số điện thoại đã được sử dụng", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(400, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),

    // ========== USER ==========
    USER_NOT_FOUND(404, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),

    // ========== UPLOAD ẢNH ==========
    FILE_EMPTY(400, "Vui lòng chọn ảnh", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(400, "Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(413, "Ảnh không được vượt quá 2MB", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_UPLOAD_FAILED(500, "Không lưu được ảnh, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR),

    // ========== TOUR ==========
    TOUR_NOT_FOUND(404, "Không tìm thấy tour", HttpStatus.NOT_FOUND),
    TOUR_ALREADY_DELETED(400, "Tour đã bị xóa trước đó", HttpStatus.BAD_REQUEST),
    TOUR_HAS_BOOKINGS(400, "Tour đã có đơn đặt, không thể xoá", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(404, "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    LOCATION_NOT_FOUND(404, "Không tìm thấy địa điểm", HttpStatus.NOT_FOUND),
    DISPLAY_ORDER_ALREADY_EXISTS(400, "Thứ tự hiển thị đã tồn tại", HttpStatus.BAD_REQUEST),
    SLUG_ALREADY_EXISTS(400, "Slug đã tồn tại", HttpStatus.BAD_REQUEST),
    INVALID_SLUG(400, "Slug không hợp lệ", HttpStatus.BAD_REQUEST),
    GALLERY_LIMIT_EXCEEDED(400, "Thư viện ảnh tối đa 20 ảnh", HttpStatus.BAD_REQUEST),

    // ========== TOUR SCHEDULE ==========
    SCHEDULE_NOT_FOUND(404, "Không tìm thấy lịch khởi hành", HttpStatus.NOT_FOUND),
    SCHEDULE_DATE_EXISTS(409, "Tour đã có lịch khởi hành vào ngày này", HttpStatus.CONFLICT),
    INVALID_SCHEDULE_DATE(400, "Ngày khởi hành phải sau ngày hôm nay", HttpStatus.BAD_REQUEST),
    INVALID_RETURN_DATE(400, "Ngày về không được trước ngày khởi hành", HttpStatus.BAD_REQUEST),
    SCHEDULE_SLOTS_TOO_LOW(400, "Tổng số chỗ không được ít hơn số chỗ khách đã đặt", HttpStatus.BAD_REQUEST),
    SCHEDULE_LOCKED_BY_BOOKINGS(409, "Lịch đã có khách đặt, không thể đổi ngày hoặc giá", HttpStatus.CONFLICT),
    SCHEDULE_HAS_ACTIVE_BOOKINGS(409, "Lịch còn đơn đặt đang hiệu lực, hãy xử lý các đơn trước khi huỷ lịch", HttpStatus.CONFLICT),
    SCHEDULE_HAS_BOOKINGS(409, "Lịch đã có đơn đặt, không thể xoá. Hãy huỷ lịch thay vì xoá", HttpStatus.CONFLICT),


    // ========== BOOKING ==========
    BOOKING_NOT_FOUND(404, "Không tìm thấy đơn đặt tour", HttpStatus.NOT_FOUND),
    SCHEDULE_NOT_AVAILABLE(400, "Lịch khởi hành này không còn nhận đặt chỗ", HttpStatus.BAD_REQUEST),
    SCHEDULE_ALREADY_DEPARTED(400, "Lịch khởi hành đã qua, không thể đặt", HttpStatus.BAD_REQUEST),
    NOT_ENOUGH_SLOTS(400, "Số chỗ còn lại không đủ cho số khách yêu cầu", HttpStatus.BAD_REQUEST),
    INVALID_PARTICIPANT_COUNT(400, "Số hành khách khai báo không khớp với số lượng đã chọn", HttpStatus.BAD_REQUEST),
    PRIMARY_PARTICIPANT_REQUIRED(400, "Phải có đúng một người đại diện đoàn", HttpStatus.BAD_REQUEST),
    BOOKING_ALREADY_CANCELLED(400, "Đơn đặt này đã bị hủy trước đó", HttpStatus.BAD_REQUEST),
    BOOKING_ALREADY_CONFIRMED(400, "Đơn đặt này đã được xác nhận", HttpStatus.BAD_REQUEST),
    BOOKING_CANNOT_CANCEL(400, "Đơn đã hoàn thành, không thể hủy", HttpStatus.BAD_REQUEST),
    BOOKING_CANNOT_CONFIRM(400, "Chỉ xác nhận được đơn đang chờ xử lý", HttpStatus.BAD_REQUEST),
    BOOKING_CANNOT_COMPLETE(400, "Chỉ đơn đã xác nhận mới có thể chuyển sang hoàn thành", HttpStatus.BAD_REQUEST),
    DUPLICATE_BOOKING_ATTEMPT(400, "Bạn vừa đặt chuyến này. Vui lòng kiểm tra lại đơn đã đặt trước khi đặt tiếp", HttpStatus.BAD_REQUEST),
    DUPLICATE_ACTIVE_BOOKING(400, "Bạn đang có đơn đặt cho chuyến đi này. Vui lòng hoàn thành hoặc hủy đơn cũ trước khi đặt tiếp", HttpStatus.BAD_REQUEST),
    SINGLE_ROOM_ADULT_ONLY(400, "Phòng đơn chỉ áp dụng cho khách từ 12 tuổi trở lên", HttpStatus.BAD_REQUEST),

    // ========== SYSTEM ==========
    INTERNAL_SERVE_ERROR(500, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR)
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
