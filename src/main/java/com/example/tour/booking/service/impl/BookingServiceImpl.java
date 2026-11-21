package com.example.tour.booking.service.impl;

import com.example.tour.booking.constant.BookingConstants;
import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.ParticipantType;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.booking.dto.export.BookingExcelModel;
import com.example.tour.booking.dto.request.*;
import com.example.tour.booking.dto.response.*;
import com.example.tour.booking.entity.Booking;
import com.example.tour.booking.entity.Participant;
import com.example.tour.booking.event.BookingCompletedEvent;
import com.example.tour.booking.event.BookingCreatedEvent;
import com.example.tour.booking.repository.BookingRepository;
import com.example.tour.booking.service.BookingService;
import com.example.tour.booking.specification.BookingSpecification;
import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.promotion.entity.Promotion;
import com.example.tour.promotion.repository.PromotionRepository;
import com.example.tour.promotion.service.PromotionService;
import com.example.tour.review.dto.response.ReviewResponse;
import com.example.tour.review.entity.Review;
import com.example.tour.review.repository.ReviewRepository;
import com.example.tour.tour.constant.ScheduleStatus;
import com.example.tour.tour.entity.Schedule;
import com.example.tour.tour.entity.Tour;
import com.example.tour.tour.repository.ScheduleRepository;
import com.example.tour.tour.repository.TourRepository;
import com.example.tour.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter VN_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Giới hạn số dòng xuất 1 lần: tránh admin lỡ bấm xuất hàng trăm nghìn đơn làm treo server */
    private static final int EXPORT_LIMIT = 10_000;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final TourRepository tourRepository;
    private final PromotionRepository promotionRepository;
    private final ReviewRepository reviewRepository;
    private final PromotionService promotionService;
    private final ApplicationEventPublisher eventPublisher;

    // ========== USER ==========
    /** userId lấy từ token ở controller; null = khách đặt không đăng nhập */
    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, Integer userId) {
        // 1. Khoá dòng lịch: 2 người đặt cùng lúc phải xếp hàng → không bán quá số chỗ
        Schedule schedule = scheduleRepository.findByIdForUpdate(request.getScheduleId())
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        Tour tour = schedule.getTour();

        // Tour (cùng danh mục, địa điểm) phải đang hiển thị với khách
        tourRepository.findPublicById(tour.getTourId())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        // Lịch đã huỷ hoặc đã tới ngày đi thì không nhận đặt
        if (schedule.getStatus() == ScheduleStatus.CANCELLED
                || !schedule.getDepartureDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_AVAILABLE);
        }

        // 2. Hành khách: đúng số lượng từng loại, đúng tuổi, phòng đơn chỉ cho người lớn
        validateParticipants(request, schedule.getDepartureDate());

        // 3. Còn đủ chỗ không
        int seats = request.getTotalAdults() + request.getTotalChildren() + request.getTotalToddlers();
        if (schedule.getAvailableSlots() < seats) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_ENOUGH_SLOTS);
        }

        // 4. Giữ chỗ: trừ chỗ ngay (lịch đang bị khoá nên an toàn), hết chỗ thì FULL
        schedule.setAvailableSlots(schedule.getAvailableSlots() - seats);
        if (schedule.getAvailableSlots() == 0) {
            schedule.setStatus(ScheduleStatus.FULL);
        }

        // Khách đi 1 mình (1 người lớn, không trẻ em / trẻ nhỏ) → không có ai ở ghép → BẮT BUỘC phòng đơn
        boolean travelsAlone = request.getTotalAdults() == 1
                && request.getTotalChildren() == 0
                && request.getTotalToddlers() == 0;

        // 5. Tạm tính = giá từng loại × số lượng + phụ thu phòng đơn × số phòng
        int singleRooms = travelsAlone ? 1 : (int) request.getParticipants().stream()
                .filter(p -> Boolean.TRUE.equals(p.getSingleRoom()))
                .count();
        BigDecimal tempAmount = price(schedule.getAdultPrice(), request.getTotalAdults())
                .add(price(schedule.getChildPrice(), request.getTotalChildren()))
                .add(price(schedule.getToddlerPrice(), request.getTotalToddlers()))
                .add(price(schedule.getSingleRoomSupplement(), singleRooms));

        // 6. Mã khuyến mãi (không bắt buộc)
        Promotion promotion = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (request.getPromotionId() != null) {
            if (userId == null) {
                throw new AppException(ErrorCode.PROMOTION_LOGIN_REQUIRED);
            }
            Promotion p = promotionRepository.findById(request.getPromotionId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
            // Kiểm tra còn hạn, còn lượt, đúng tour, đủ đơn tối thiểu, chưa dùng quá số lần / khách
            promotion = promotionService.validateForBooking(p.getPromotionCode(), tour.getTourId(), tempAmount, userId);
            discount = promotionService.calculateDiscount(promotion, tempAmount);   // cố định hoặc %, có giới hạn
            promotionService.consume(promotion.getPromotionId());                   // lấy 1 lượt dùng
        }
        BigDecimal totalAmount = tempAmount.subtract(discount);

        // 7. Lưu đơn
        Booking booking = Booking.builder()
                .bookingCode("TMP-" + UUID.randomUUID())   // mã tạm để INSERT được (NOT NULL + UNIQUE)
                .user(userId == null ? null : userRepository.getReferenceById(userId))
                .schedule(schedule)
                .promotion(promotion)
                .contactName(request.getContactName().trim())
                .contactEmail(request.getContactEmail().trim().toLowerCase())
                .contactPhone(request.getContactPhone().trim())
                .contactAddress(trimToNull(request.getContactAddress()))
                .totalAdults(request.getTotalAdults())
                .totalChildren(request.getTotalChildren())
                .totalToddlers(request.getTotalToddlers())
                .tempAmount(tempAmount)
                .totalAmount(totalAmount)
                // Còn chỗ + đã giữ chỗ → đơn được XÁC NHẬN TỰ ĐỘNG; tiền theo dõi riêng ở paymentStatus
                .bookingStatus(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PENDING)
                .note(trimToNull(request.getNote()))
                .paymentToken(UUID.randomUUID().toString())     // 122 bit ngẫu nhiên, không đoán được
                .build();

        // Hành khách: addParticipant gắn 2 chiều (booking ↔ participant); cascade ALL tự lưu theo booking
        request.getParticipants().forEach(p -> booking.addParticipant(Participant.builder()
                .fullName(p.getFullName().trim())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .participantType(p.getParticipantType())
                .phoneNumber(trimToNull(p.getPhoneNumber()))
                // Đi 1 mình → luôn phòng đơn, dù request gửi gì
                .singleRoom(travelsAlone || Boolean.TRUE.equals(p.getSingleRoom()))
                .build()));

        Booking saved = bookingRepository.save(booking);

        // Mã thật sinh từ id (không bao giờ trùng): BK + ngày đặt + id → BK26111500017
        // Không cần save lại: entity đang được Hibernate theo dõi, commit tự UPDATE
        saved.setBookingCode("BK" + LocalDate.now().format(CODE_DATE) + String.format("%05d", saved.getBookingId()));

        BookingResponse response = toBookingResponse(saved);
        // Email gửi SAU KHI commit (BookingEmailListener): rollback thì không gửi
        eventPublisher.publishEvent(new BookingCreatedEvent(response));
        return response;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<BookingCardResponse> getMyBookings(Integer userId, BookingStatus bookingStatus,
                                                   PaymentStatus paymentStatus, Pageable pageable) {
        Page<Booking> page = bookingRepository.findAll(
                BookingSpecification.ofUser(userId, bookingStatus, paymentStatus), pageable);

        List<Integer> ids = page.getContent().stream().map(Booking::getBookingId).toList();
        Map<Integer, Review> reviews = ids.isEmpty() ? Map.of()
                : reviewRepository.findByBooking_BookingIdIn(ids).stream()
                .collect(Collectors.toMap(r -> r.getBooking().getBookingId(), r -> r));

        return page.map(b -> toBookingCardResponse(b, reviews.get(b.getBookingId())));
    }

    /** Chi tiết đơn của CHÍNH khách đang đăng nhập. Đơn của người khác → báo "không tìm thấy" (không lộ là có tồn tại) */
    @Override
    @Transactional(readOnly = true)
    public BookingResponse getMyBookingById(Integer userId, Integer bookingId) {
        Booking booking = bookingRepository.findWithDetailsByBookingId(bookingId)
                .filter(b -> isOwner(b, userId))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        BookingResponse response = toBookingResponse(booking);
        // Gắn đánh giá của khách (nếu có) để popup chi tiết hiện "Đánh giá của bạn" / nút "Đánh giá ngay"
        response.setReview(ReviewResponse.of(
                reviewRepository.findByBooking_BookingId(bookingId).orElse(null)));
        return response;
    }

    // ========== ADMIN ==========
    @Override
    @Transactional(readOnly = true)
    public Page<BookingAdminRowResponse> getBookingsForAdmin(BookingFilterRequest filter, Pageable pageable) {
        return bookingRepository.findAll(BookingSpecification.adminFilter(filter), pageable)
                .map(this::toAdminRowResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByIdForAdmin(Integer bookingId) {
        // Nạp sẵn lịch, tour, mã KM, người đặt trong 1 câu (toBookingResponse đọc hết các thứ này)
        Booking booking = bookingRepository.findWithDetailsByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        return toBookingResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(BookingUpdateRequest request, Integer bookingId) {
        Booking booking = lock(bookingId);

        // Đơn đã huỷ / hoàn thành là lịch sử, không sửa
        if (booking.getBookingStatus() != BookingStatus.PENDING
                && booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_INVALID_STATUS);
        }

        // 1. Thông tin liên hệ + ghi chú
        booking.setContactName(request.getContactName().trim());
        booking.setContactEmail(request.getContactEmail().trim().toLowerCase());
        booking.setContactPhone(request.getContactPhone().trim());
        booking.setContactAddress(trimToNull(request.getContactAddress()));
        booking.setNote(trimToNull(request.getNote()));

        // 2. Hành khách: phải gửi đúng ĐỦ những người đang có trong đơn (không thêm, không bớt)
        Map<Integer, Participant> current = booking.getParticipants().stream()
                .collect(Collectors.toMap(Participant::getParticipantId, p -> p));
        Set<Integer> sentIds = request.getParticipants().stream()
                .map(ParticipantUpdateRequest::getParticipantId)
                .collect(Collectors.toSet());
        if (sentIds.size() != request.getParticipants().size()   // gửi trùng id
                || !sentIds.equals(current.keySet())) {            // thiếu người / id của đơn khác
            throw new AppException(ErrorCode.INVALID_PARTICIPANT_COUNT);
        }

        LocalDate departure = booking.getSchedule().getDepartureDate();
        for (ParticipantUpdateRequest r : request.getParticipants()) {
            Participant p = current.get(r.getParticipantId());
            // Sửa ngày sinh vẫn phải đúng độ tuổi của loại khách (giá đã tính theo loại)
            checkAge(p.getParticipantType(), r.getDateOfBirth(), departure);

            p.setFullName(r.getFullName().trim());
            p.setDateOfBirth(r.getDateOfBirth());
            p.setGender(r.getGender());
            p.setPhoneNumber(trimToNull(r.getPhoneNumber()));
        }

        // Không cần save: booking + participants đang được Hibernate theo dõi, commit tự UPDATE
        return toBookingResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Integer bookingId) {
        Booking booking = lock(bookingId);
        // Đã huỷ / đã hoàn thành thì không huỷ nữa (tránh trả chỗ 2 lần)
        if (booking.getBookingStatus() != BookingStatus.PENDING
                && booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_CANCEL);
        }
        // Khách đã trả tiền mà đơn bị huỷ → phải hoàn tiền
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        cancelAndRelease(booking);
        return toBookingResponse(booking);
    }

    /**
     * Job gọi: đơn quá hạn giữ chỗ mà chưa thanh toán → Đã hủy, thanh toán giữ "Chưa thanh toán", trả chỗ + trả lượt mã KM.
     * Khoá dòng rồi KIỂM TRA LẠI: có thể khách vừa thanh toán xong ngay trước khi job chạy tới đơn này.
     * @return true nếu đã huỷ
     */
    @Override
    @Transactional
    public boolean expireUnpaidBooking(Integer bookingId) {
        Booking booking = lock(bookingId);
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED
                || booking.getPaymentStatus() != PaymentStatus.PENDING) {
            return false;
        }
        cancelAndRelease(booking);
        return true;
    }

    @Override
    @Transactional
    public BookingResponse completeBooking(Integer bookingId) {
        Booking booking = lock(bookingId);
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_INVALID_STATUS);
        }
        // Luồng mới: CONFIRMED có cả đơn chưa trả tiền → chưa thanh toán thì không cho hoàn thành
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new AppException(ErrorCode.BOOKING_NOT_PAID);
        }
        // Tour chưa khởi hành thì chưa thể hoàn thành
        if (booking.getSchedule().getDepartureDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.BOOKING_NOT_DEPARTED);
        }
        // Chỉ hoàn thành khi tour đã KẾT THÚC: từ ngày về trở đi
        // VD: đi 17/11 – về 20/11 → 17, 18, 19/11 (đang đi tour) bị chặn; từ 20/11 mới được
        Schedule schedule = booking.getSchedule();
        LocalDate endDate = schedule.getReturnDate() != null
                ? schedule.getReturnDate()
                : schedule.getDepartureDate();   // phòng lịch cũ chưa có ngày về
        if (LocalDate.now().isBefore(endDate)) {
            throw new AppException(ErrorCode.BOOKING_TOUR_NOT_FINISHED);
        }
        booking.setBookingStatus(BookingStatus.COMPLETED);
        Schedule s = booking.getSchedule();
        eventPublisher.publishEvent(new BookingCompletedEvent(
                booking.getBookingCode(), booking.getPaymentToken(),
                booking.getContactName(), booking.getContactEmail(),
                s.getTour().getTourName(), s.getDepartureDate(), s.getReturnDate()));
        return toBookingResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingStatsResponse getBookingStats(int upcomingDays) {
        LocalDate today = LocalDate.now();
        return BookingStatsResponse.builder()
                .total(bookingRepository.count())
                // Đơn đang giữ chỗ, chờ khách trả tiền (thay cho "Chờ xác nhận" cũ)
                .unpaid(bookingRepository.countByPaymentStatus(PaymentStatus.PENDING))
                .confirmed(bookingRepository.countByBookingStatus(BookingStatus.CONFIRMED))
                .paid(bookingRepository.countByPaymentStatus(PaymentStatus.PAID))
                // Sắp khởi hành: chỉ đếm đơn ĐÃ TRẢ TIỀN (đơn chưa trả có thể bị huỷ)
                .upcoming(bookingRepository.countByBookingStatusAndPaymentStatusAndSchedule_DepartureDateBetween(
                        BookingStatus.CONFIRMED, PaymentStatus.PAID, today, today.plusDays(upcomingDays)))
                .completed(bookingRepository.countByBookingStatus(BookingStatus.COMPLETED))
                .cancelled(bookingRepository.countByBookingStatus(BookingStatus.CANCELLED))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingExcelModel> exportBookings(BookingFilterRequest filter, Sort sort) {
        // Cùng bộ lọc với trang danh sách → file Excel khớp đúng những gì admin đang xem
        List<Booking> bookings = bookingRepository.findAll(BookingSpecification.adminFilter(filter), sort);
        if (bookings.size() > EXPORT_LIMIT) {
            throw new AppException(ErrorCode.EXPORT_TOO_MANY_ROWS);
        }

        List<BookingExcelModel> rows = new ArrayList<>(bookings.size());
        int stt = 1;
        for (Booking b : bookings) {
            Schedule s = b.getSchedule();
            BigDecimal temp = b.getTempAmount() != null ? b.getTempAmount() : b.getTotalAmount();
            rows.add(BookingExcelModel.builder()
                    .stt(stt++)
                    .bookingCode(b.getBookingCode())
                    .createdAt(b.getCreatedAt() != null ? b.getCreatedAt().format(VN_DATE_TIME) : "")
                    .contactName(b.getContactName())
                    .contactPhone(b.getContactPhone())
                    .contactEmail(b.getContactEmail())
                    .tourName(s.getTour().getTourName())
                    .scheduleCode(s.getScheduleCode())
                    .departureDate(s.getDepartureDate() != null ? s.getDepartureDate().format(VN_DATE) : "")
                    .returnDate(s.getReturnDate() != null ? s.getReturnDate().format(VN_DATE) : "")
                    .totalAdults(b.getTotalAdults())
                    .totalChildren(b.getTotalChildren())
                    .totalToddlers(b.getTotalToddlers())
                    .totalPeople(b.getTotalAdults() + b.getTotalChildren() + b.getTotalToddlers())
                    .tempAmount(temp)
                    .discountAmount(temp.subtract(b.getTotalAmount()))
                    .promotionCode(b.getPromotion() != null ? b.getPromotion().getPromotionCode() : "")
                    .totalAmount(b.getTotalAmount())
                    .bookingStatus(bookingStatusText(b.getBookingStatus()))
                    .paymentStatus(paymentStatusText(b.getPaymentStatus()))
                    .note(b.getNote())
                    .build());
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingPaymentStatusResponse getPaymentStatus(String bookingCode) {
        Booking b = bookingRepository.findByBookingCode(bookingCode.trim().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        return BookingPaymentStatusResponse.builder()
                .bookingCode(b.getBookingCode())
                .bookingStatus(b.getBookingStatus())
                .paymentStatus(b.getPaymentStatus())
                .build();
    }

    /** Trang thanh toán: đúng token HOẶC là chủ đơn. Sai → "không tìm thấy" */
    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingForPayment(String bookingCode, String token, Integer userId) {
        Booking booking = bookingRepository.findWithDetailsByBookingCode(bookingCode.trim().toUpperCase())
                .filter(b -> isOwner(b, userId) || tokenMatches(b.getPaymentToken(), token))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        return toBookingResponse(booking);
    }

    // ========== HỖ TRỢ ==========
    private void validateParticipants(BookingRequest request, LocalDate departure) {
        int adults = 0, children = 0, toddlers = 0;
        for (ParticipantRequest p : request.getParticipants()) {
            checkAge(p.getParticipantType(), p.getDateOfBirth(), departure);
            switch (p.getParticipantType()) {
                case ADULT -> adults++;
                case CHILD -> children++;
                case TODDLER -> toddlers++;
            }
            if (Boolean.TRUE.equals(p.getSingleRoom()) && p.getParticipantType() != ParticipantType.ADULT) {
                throw new AppException(ErrorCode.SINGLE_ROOM_ADULT_ONLY);
            }
        }
        if (adults != request.getTotalAdults() || children != request.getTotalChildren()
                || toddlers != request.getTotalToddlers()) {
            throw new AppException(ErrorCode.INVALID_PARTICIPANT_COUNT);
        }
    }

    /** Tuổi tính tại NGÀY KHỞI HÀNH: người lớn ≥ 12, trẻ em 5–11, trẻ nhỏ 2–4 */
    private static void checkAge(ParticipantType type, LocalDate dateOfBirth, LocalDate departure) {
        int age = Period.between(dateOfBirth, departure).getYears();
        boolean ok = switch (type) {
            case ADULT -> age >= 12;
            case CHILD -> age >= 5 && age <= 11;
            case TODDLER -> age >= 2 && age <= 4;
        };
        if (!ok) throw new AppException(ErrorCode.INVALID_PARTICIPANT_AGE);
    }

    /** Khoá dòng đơn: 2 admin thao tác cùng lúc thì người sau phải chờ, rồi đọc trạng thái MỚI */
    private Booking lock(Integer bookingId) {
        return bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    }

    /** Huỷ đơn + trả chỗ + trả 1 lượt dùng mã KM (dùng chung cho admin huỷ và job hết hạn) */
    private void cancelAndRelease(Booking booking) {
        booking.setBookingStatus(BookingStatus.CANCELLED);
        releaseSeats(booking);
        if (booking.getPromotion() != null) {
            promotionService.release(booking.getPromotion().getPromotionId());
        }
    }

    /** Trả chỗ cho lịch. Tour đã khởi hành thì chỗ không bán lại được → bỏ qua */
    private void releaseSeats(Booking booking) {
        Schedule schedule = scheduleRepository.findByIdForUpdate(booking.getSchedule().getScheduleId())
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        if (!schedule.getDepartureDate().isAfter(LocalDate.now())) return;

        int seats = booking.getTotalAdults() + booking.getTotalChildren() + booking.getTotalToddlers();
        schedule.setAvailableSlots(Math.min(schedule.getAvailableSlots() + seats, schedule.getTotalSlots()));
        // Đang FULL mà có chỗ lại → mở bán lại. Lịch admin đã huỷ (CANCELLED) thì giữ nguyên
        if (schedule.getStatus() == ScheduleStatus.FULL && schedule.getAvailableSlots() > 0) {
            schedule.setStatus(ScheduleStatus.AVAILABLE);
        }
    }

    /** So sánh constant-time: không lộ token qua thời gian phản hồi */
    private static boolean tokenMatches(String expected, String given) {
        return expected != null && given != null
                && java.security.MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                given.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Nhãn tiếng Việt — giống badge trên trang admin */
    private static String bookingStatusText(BookingStatus s) {
        if (s == null) return "";
        return switch (s) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case CANCELLED -> "Đã hủy";
            case COMPLETED -> "Hoàn thành";
        };
    }

    private static String paymentStatusText(PaymentStatus s) {
        if (s == null) return "";
        return switch (s) {
            case PENDING -> "Chưa thanh toán";
            case PAID -> "Đã thanh toán";
            case REFUNDED -> "Hoàn tiền";
        };
    }

    private static BigDecimal price(BigDecimal unitPrice, int quantity) {
        return unitPrice == null || quantity == 0 ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** getUserId() trả int (kiểu nguyên thuỷ) → so bằng ==; đơn khách vãng lai (user = null) không thuộc ai */
    private static boolean isOwner(Booking b, Integer userId) {
        return userId != null && b.getUser() != null && b.getUser().getUserId() == userId;
    }

    private BookingAdminRowResponse toAdminRowResponse(Booking b) {
        Schedule s = b.getSchedule();
        return BookingAdminRowResponse.builder()
                .bookingId(b.getBookingId())
                .bookingCode(b.getBookingCode())
                .contactName(b.getContactName())
                .contactPhone(b.getContactPhone())
                .tourName(s.getTour().getTourName())
                .scheduleCode(s.getScheduleCode())
                .departureDate(s.getDepartureDate())
                .returnDate(s.getReturnDate())
                .totalPeople(b.getTotalAdults() + b.getTotalChildren() + b.getTotalToddlers())
                .totalAmount(b.getTotalAmount())
                .bookingStatus(b.getBookingStatus())
                .paymentStatus(b.getPaymentStatus())
                .createdAt(b.getCreatedAt())
                .build();
    }

    private BookingCardResponse toBookingCardResponse(Booking b, Review review) {
        Schedule s = b.getSchedule();
        Tour t = s.getTour();
        return BookingCardResponse.builder()
                .bookingId(b.getBookingId())
                .bookingCode(b.getBookingCode())
                .bookingDate(b.getCreatedAt())
                .tourId(t.getTourId())
                .tourName(t.getTourName())
                .thumbnailUrl(t.getThumbnailUrl())
                .scheduleCode(s.getScheduleCode())
                .departureDate(s.getDepartureDate())
                .returnDate(s.getReturnDate())
                .totalAdults(b.getTotalAdults())
                .totalChildren(b.getTotalChildren())
                .totalToddlers(b.getTotalToddlers())
                .totalAmount(b.getTotalAmount())
                .paymentStatus(b.getPaymentStatus())
                .bookingStatus(b.getBookingStatus())
                .review(ReviewResponse.of(review))
                .build();
    }


    private BookingResponse toBookingResponse(Booking b) {
        Schedule s = b.getSchedule();
        Tour t = s.getTour();

        // Tạm tính null (đơn cũ chưa lưu) → coi như bằng tổng tiền, giảm giá = 0
        BigDecimal temp = b.getTempAmount() != null ? b.getTempAmount() : b.getTotalAmount();

        int singleRoomCount = (int) b.getParticipants().stream()
                .filter(p -> Boolean.TRUE.equals(p.getSingleRoom()))
                .count();

        return BookingResponse.builder()
                .bookingId(b.getBookingId())
                .bookingCode(b.getBookingCode())
                // Khách đặt không đăng nhập → user = null; đơn không dùng mã → promotion = null
                .userId(b.getUser() != null ? b.getUser().getUserId() : null)
                .promotionId(b.getPromotion() != null ? b.getPromotion().getPromotionId() : null)
                .promotionCode(b.getPromotion() != null ? b.getPromotion().getPromotionCode() : null)
                .contactName(b.getContactName())
                .contactPhone(b.getContactPhone())
                .contactEmail(b.getContactEmail())
                .contactAddress(b.getContactAddress())
                .totalAdults(b.getTotalAdults())
                .totalChildren(b.getTotalChildren())
                .totalToddlers(b.getTotalToddlers())
                .adultPrice(s.getAdultPrice())
                .childPrice(s.getChildPrice())
                .toddlerPrice(s.getToddlerPrice())
                .singleRoomSupplement(s.getSingleRoomSupplement())
                .singleRoomCount(singleRoomCount)
                .tempAmount(temp)
                .discountAmount(temp.subtract(b.getTotalAmount()))
                .totalAmount(b.getTotalAmount())
                .bookingStatus(b.getBookingStatus())
                .paymentStatus(b.getPaymentStatus())
                .note(b.getNote())
                .tourId(t.getTourId())
                .tourName(t.getTourName())
                .thumbnailUrl(t.getThumbnailUrl())
                .scheduleId(s.getScheduleId())
                .scheduleCode(s.getScheduleCode())
                .departureDate(s.getDepartureDate())
                .returnDate(s.getReturnDate())
                .transport(s.getTransport())
                .participants(b.getParticipants().stream()
                        // Người lớn → trẻ em → trẻ nhỏ (theo thứ tự khai báo trong enum)
                        .sorted(Comparator.comparing(p -> p.getParticipantType().ordinal()))
                        .map(p -> new BookingResponse.Participant(
                                p.getParticipantId(),
                                p.getFullName(),
                                p.getDateOfBirth(),
                                p.getGender(),
                                p.getParticipantType(),
                                p.getPhoneNumber(),
                                Boolean.TRUE.equals(p.getSingleRoom())))
                        .toList())
                .paymentToken(b.getPaymentToken())
                .paymentDeadline(b.getCreatedAt() != null
                        ? b.getCreatedAt().plusSeconds(BookingConstants.HOLD_SECONDS) : null)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}

