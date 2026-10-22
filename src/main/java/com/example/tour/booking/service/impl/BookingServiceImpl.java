//package com.example.tour.booking.service.impl;
//
//import com.alibaba.excel.EasyExcel;
//import com.example.tour.booking.constant.BookingPaymentStatus;
//import com.example.tour.booking.constant.BookingStatus;
//import com.example.tour.booking.dto.export.BookingExcelModel;
//import com.example.tour.booking.dto.export.ParticipantExcelModel;
//import com.example.tour.booking.dto.request.BookingParticipantRequest;
//import com.example.tour.booking.dto.request.BookingRequest;
//import com.example.tour.booking.dto.response.BookingDetailResponse;
//import com.example.tour.booking.dto.response.BookingResponse;
//import com.example.tour.booking.entity.Booking;
//import com.example.tour.booking.entity.BookingParticipant;
//import com.example.tour.booking.repository.BookingRepository;
//import com.example.tour.booking.service.BookingService;
//import com.example.tour.booking.specification.BookingSpecification;
//import com.example.tour.common.exception.AppException;
//import com.example.tour.common.exception.ErrorCode;
//import com.example.tour.tour.constant.ScheduleStatus;
//import com.example.tour.tour.entity.Tour;
//import com.example.tour.tour.entity.TourSchedule;
//import com.example.tour.tour.repository.TourScheduleRepository;
//import com.example.tour.user.entity.User;
//import com.example.tour.user.repository.UserRepository;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.concurrent.ThreadLocalRandom;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class BookingServiceImpl implements BookingService {
//
//    private final BookingRepository bookingRepository;
//    private final TourScheduleRepository tourScheduleRepository;
//    private final UserRepository userRepository;
//
//    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
//
//    @Override
//    public BookingResponse createBooking(int userId, BookingRequest request) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
//
//        TourSchedule schedule = tourScheduleRepository.findById(request.getScheduleId())
//                .orElseThrow(() -> new AppException(ErrorCode.TOUR_SCHEDULE_NOT_FOUND));
//
//        // 1. Lịch phải còn nhận đặt
//        if(schedule.getStatus() != ScheduleStatus.AVAILABLE) {
//            throw new AppException(ErrorCode.SCHEDULE_NOT_AVAILABLE);
//        }
//
//        // 2. Chưa khởi hành
//        if(schedule.getDepartureDate().isBefore(LocalDate.now())) {
//            throw new AppException(ErrorCode.SCHEDULE_ALREADY_DEPARTED);
//        }
//
//        // 3. Mỗi khách chỉ có một đơn còn hiệu lực cho mỗi chuyến đi
//        if (bookingRepository.existsActiveBookingForSchedule(userId, schedule.getScheduleId())) {
//            throw new AppException(ErrorCode.DUPLICATE_ACTIVE_BOOKING);
//        }
//
//        // 4. Số hành khách khai báo phải khớp số lượng đã chọn
//        int declared = request.getTotalAdults() + request.getTotalChildren() + request.getTotalInfants();
//        if(request.getParticipants().size() != declared) {
//            throw new AppException(ErrorCode.INVALID_PARTICIPANT_COUNT);
//        }
//
//        // 5. Đúng một người đại diện đoàn
//        long primaryCount = request.getParticipants().stream()
//                .filter(BookingParticipantRequest::isPrimary).count();
//        if(primaryCount != 1) {
//            throw new AppException(ErrorCode.PRIMARY_PARTICIPANT_REQUIRED);
//        }
//
//        // 6. Phòng đơn chỉ dành cho người lớn
//        boolean invalidSingleRoom = request.getParticipants().stream()
//                .anyMatch(p -> p.isSingleRoom() && p.getParticipantType() != ParticipantType.ADULT);
//        if (invalidSingleRoom) {
//            throw new AppException(ErrorCode.SINGLE_ROOM_ADULT_ONLY);
//        }
//
//        // 7. Còn đủ chỗ — em bé ngồi cùng người lớn nên không tính vào số chỗ
//        int seatsNeeded = request.getTotalAdults() + request.getTotalChildren();
//        if(schedule.getBookedSlots() + seatsNeeded > schedule.getAvailableSlots()) {
//            throw new AppException(ErrorCode.NOT_ENOUGH_SLOTS);
//        }
//
//        Tour tour = schedule.getTour();
//        BigDecimal adjustment = schedule.getPriceAdjustment() != null
//                ? schedule.getPriceAdjustment() : BigDecimal.ZERO;
//
//        BigDecimal adultPrice  = tour.getBasePrice().add(adjustment);
//        BigDecimal childPrice  = (tour.getChildPrice() != null ? tour.getChildPrice() : tour.getBasePrice())
//                .add(adjustment);
//        BigDecimal infantPrice = tour.getInfantPrice() != null ? tour.getInfantPrice() : BigDecimal.ZERO;
//
//        // Phụ thu phòng đơn — chốt giá tại thời điểm đặt, không cộng adjustment
//        BigDecimal singleRoomSurcharge = tour.getSingleRoomSurcharge() != null
//                ? tour.getSingleRoomSurcharge() : BigDecimal.ZERO;
//        int singleRoomCount = (int) request.getParticipants().stream()
//                .filter(BookingParticipantRequest::isSingleRoom).count();
//
//        BigDecimal totalAmount = adultPrice.multiply(BigDecimal.valueOf(request.getTotalAdults()))
//                .add(childPrice.multiply(BigDecimal.valueOf(request.getTotalChildren())))
//                .add(infantPrice.multiply(BigDecimal.valueOf(request.getTotalInfants())))
//                .add(singleRoomSurcharge.multiply(BigDecimal.valueOf(singleRoomCount)));
//
//        Booking booking = new Booking();
//        booking.setUser(user);
//        booking.setTourSchedule(schedule);
//        booking.setBookingCode(generateBookingCode());
//        booking.setTotalAdults(request.getTotalAdults());
//        booking.setTotalChildren(request.getTotalChildren());
//        booking.setTotalInfants(request.getTotalInfants());
//        booking.setAdultPriceAtBooking(adultPrice);
//        booking.setChildPriceAtBooking(childPrice);
//        booking.setInfantPriceAtBooking(infantPrice);
//        booking.setSingleRoomSurchargeAtBooking(singleRoomSurcharge);
//        booking.setSingleRoomCount(singleRoomCount);
//        booking.setTotalAmount(totalAmount);
//        booking.setDiscountAmount(BigDecimal.ZERO);
//        booking.setPointsDiscountAmount(BigDecimal.ZERO);
//        booking.setFinalAmount(totalAmount);   // chưa trừ khuyến mãi/điểm ở giai đoạn này
//        booking.setSpecialRequests(request.getSpecialRequests());
//        booking.setBookingStatus(BookingStatus.PENDING);
//        booking.setBookingPaymentStatus(BookingPaymentStatus.PENDING);
//
//        // Hành khách
//        Set<BookingParticipant> participants = new HashSet<>();
//        for(BookingParticipantRequest p : request.getParticipants()) {
//            BookingParticipant bp = new BookingParticipant();
//            bp.setBooking(booking);
//            bp.setFullName(p.getFullName().trim());
//            bp.setDateOfBirth(p.getDateOfBirth());
//            bp.setGender(p.getGender());
//            bp.setNationality(p.getNationality());
//            bp.setIdentityType(p.getIdentityType());
//            bp.setIdentityNumber(p.getIdentityNumber());
//            bp.setIdentityExpiryDate(p.getIdentityExpiryDate());
//            bp.setIdentityIssuePlace(p.getIdentityIssuePlace());
//            bp.setParticipantType(p.getParticipantType());
//            bp.setPrimary(p.isPrimary());
//            bp.setSingleRoom(p.isSingleRoom());
//            participants.add(bp);
//        }
//        booking.setParticipants(participants);
//
//        // Trừ chỗ, khóa lịch nếu vừa đủ
//        schedule.setBookedSlots(schedule.getBookedSlots() + seatsNeeded);
//        if(schedule.getBookedSlots() >= schedule.getAvailableSlots()) {
//            schedule.setStatus(ScheduleStatus.FULL);
//        }
//        tourScheduleRepository.save(schedule);
//
//        return mapToResponse(bookingRepository.save(booking));
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<BookingResponse> getAllBookingsForAdmin(String keyword, BookingStatus bookingStatus, BookingPaymentStatus bookingPaymentStatus, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
//        Specification<Booking> spec = BookingSpecification.filterForAdmin(
//                keyword, bookingStatus, bookingPaymentStatus, fromDate, toDate);
//        return bookingRepository.findAll(spec, pageable).map(this::mapToResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public BookingDetailResponse getBookingById(int bookingId) {
//        Booking booking = findById(bookingId);
//        return mapToDetailResponse(booking);
//    }
//
//    @Override
//    public BookingResponse confirmBooking(int bookingId) {
//        Booking booking = findById(bookingId);
//
//        if(booking.getBookingStatus() == BookingStatus.CONFIRMED) {
//            throw new AppException(ErrorCode.BOOKING_ALREADY_CONFIRMED);
//        }
//
//        if (booking.getBookingStatus() != BookingStatus.PENDING) {
//            throw new AppException(ErrorCode.BOOKING_CANNOT_CONFIRM);
//        }
//
//        booking.setBookingStatus(BookingStatus.CONFIRMED);
//        booking.setConfirmedAt(LocalDateTime.now());
//        return mapToResponse(bookingRepository.save(booking));
//    }
//
//    @Override
//    public BookingResponse completeBooking(int bookingId) {
//        Booking booking = findById(bookingId);
//
//        if(booking.getBookingStatus() != BookingStatus.CONFIRMED) {
//            throw new AppException(ErrorCode.BOOKING_CANNOT_COMPLETE);
//        }
//
//        // Chưa về mà đã đóng đơn là sai
//        if(booking.getTourSchedule().getReturnDate().isAfter(LocalDate.now())) {
//            throw new AppException(ErrorCode.SCHEDULE_NOT_FINISHED_YET);
//        }
//
//        booking.setBookingStatus(BookingStatus.COMPLETED);
//        booking.setCompletedAt(LocalDateTime.now());
//        return mapToResponse(bookingRepository.save(booking));
//    }
//
//    @Override
//    public BookingResponse cancelBooking(int bookingId, String reason) {
//        Booking booking = findById(bookingId);
//
//        if(booking.getBookingStatus() == BookingStatus.CANCELLED) {
//            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
//        }
//
//        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
//            throw new AppException(ErrorCode.BOOKING_CANNOT_CANCEL);
//        }
//
//        // Hoàn lại số chỗ cho lịch khởi hành
//        TourSchedule schedule = booking.getTourSchedule();
//        int seatsToReturn = booking.getTotalAdults() + booking.getTotalChildren();
//        schedule.setBookedSlots(Math.max(0, schedule.getBookedSlots() - seatsToReturn));
//
//        // Lịch đang hết chỗ thì mở lại (trừ khi đã bị hủy hẳn)
//        if (schedule.getStatus() == ScheduleStatus.FULL
//                && schedule.getBookedSlots() < schedule.getAvailableSlots()) {
//            schedule.setStatus(ScheduleStatus.AVAILABLE);
//        }
//        tourScheduleRepository.save(schedule);
//
//        booking.setBookingStatus(BookingStatus.CANCELLED);
//        booking.setCancellationReason(reason);
//        booking.setCancelledAt(LocalDateTime.now());
//
//        return mapToResponse(bookingRepository.save(booking));
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public void exportBookingsToExcel(HttpServletResponse response,
//                                      String keyword,
//                                      BookingStatus bookingStatus,
//                                      BookingPaymentStatus bookingPaymentStatus,
//                                      LocalDate fromDate,
//                                      LocalDate toDate) throws IOException {
//
//        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//        response.setCharacterEncoding("utf-8");
//        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
//        String fileName = URLEncoder.encode("Don_dat_tour_" + timestamp, StandardCharsets.UTF_8);
//        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
//
//        // Dùng lại đúng Specification của API danh sách
//        Specification<Booking> spec = BookingSpecification.filterForAdmin(
//                keyword, bookingStatus, bookingPaymentStatus, fromDate, toDate);
//        List<Booking> bookings = bookingRepository.findAll(spec);
//
//        DateTimeFormatter dateFmt     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
//
//        List<BookingExcelModel> excelData = bookings.stream().map(b -> {
//            TourSchedule schedule = b.getTourSchedule();
//            Tour tour = schedule.getTour();
//            User user = b.getUser();
//
//            return BookingExcelModel.builder()
//                    .bookingCode(b.getBookingCode())
//                    .bookingDate(b.getBookingDate() != null ? b.getBookingDate().format(dateTimeFmt) : "")
//
//                    .customerName(user.getFullName())
//                    .customerPhone(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
//                    .customerEmail(user.getEmail())
//
//                    .tourCode(tour.getTourCode())
//                    .tourName(tour.getTourName())
//                    .departureDate(schedule.getDepartureDate().format(dateFmt))
//                    .returnDate(schedule.getReturnDate().format(dateFmt))
//
//                    .totalAdults(b.getTotalAdults())
//                    .totalChildren(b.getTotalChildren())
//                    .totalInfants(b.getTotalInfants())
//
//                    .totalAmount(b.getTotalAmount())
//                    .discountAmount(b.getDiscountAmount())
//                    .finalAmount(b.getFinalAmount())
//
//                    .bookingStatus(switch (b.getBookingStatus()) {
//                        case PENDING   -> "Chờ xác nhận";
//                        case CONFIRMED -> "Đã xác nhận";
//                        case COMPLETED -> "Đã hoàn thành";
//                        case CANCELLED -> "Đã hủy";
//                    })
//                    .paymentStatus(switch (b.getBookingPaymentStatus()) {
//                        case PENDING  -> "Chưa thanh toán";
//                        case PAID     -> "Đã thanh toán";
//                        case PARTIAL  -> "Thanh toán một phần";
//                        case REFUNDED -> "Đã hoàn tiền";
//                        case FAILED   -> "Thanh toán thất bại";
//                    })
//                    .cancellationReason(b.getCancellationReason() != null ? b.getCancellationReason() : "")
//                    .build();
//        }).toList();
//
//        EasyExcel.write(response.getOutputStream(), BookingExcelModel.class)
//                .sheet("Đơn đặt tour")
//                .doWrite(excelData);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public void exportParticipantsToExcel(HttpServletResponse response, int bookingId) throws IOException {
//
//        Booking booking = bookingRepository.findWithParticipants(bookingId)
//                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
//
//        TourSchedule schedule = booking.getTourSchedule();
//        Tour tour = schedule.getTour();
//
//        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//        response.setCharacterEncoding("utf-8");
//
//        // Tên file mang mã đơn và mã tour để dễ nhận biết khi in nhiều đoàn
//        String rawName = String.format("Danh_sach_doan_%s_%s", booking.getBookingCode(), tour.getTourCode());
//        String fileName = URLEncoder.encode(rawName, StandardCharsets.UTF_8);
//        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
//
//        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//
//        List<BookingParticipant> sorted = (booking.getParticipants() == null
//                ? List.<BookingParticipant>of()
//                : new ArrayList<>(booking.getParticipants()));
//
//        // Đại diện đoàn lên đầu, sau đó người lớn → trẻ em → em bé, cuối cùng theo tên
//        sorted.sort(Comparator
//                .comparing((BookingParticipant p) -> p.isPrimary() ? 0 : 1)
//                .thenComparing(p -> p.getParticipantType().ordinal())
//                .thenComparing(BookingParticipant::getFullName));
//
//        List<ParticipantExcelModel> excelData = new ArrayList<>();
//        int i = 1;
//        for (BookingParticipant p : sorted) {
//            excelData.add(ParticipantExcelModel.builder()
//                    .index(i++)
//                    .fullName(p.getFullName())
//                    .dateOfBirth(p.getDateOfBirth() != null ? p.getDateOfBirth().format(dateFmt) : "")
//                    .gender(p.getGender() == null ? "" : switch (p.getGender()) {
//                        case MALE   -> "Nam";
//                        case FEMALE -> "Nữ";
//                        case OTHER  -> "Khác";
//                    })
//                    .nationality(p.getNationality() != null ? p.getNationality() : "")
//                    .participantType(switch (p.getParticipantType()) {
//                        case ADULT  -> "Người lớn";
//                        case CHILD  -> "Trẻ em";
//                        case INFANT -> "Em bé";
//                    })
//                    .identityType(p.getIdentityType() == IdentityType.CCCD ? "CCCD" : "Hộ chiếu")
//                    .identityNumber(p.getIdentityNumber() != null ? p.getIdentityNumber() : "")
//                    .identityExpiryDate(p.getIdentityExpiryDate() != null
//                            ? p.getIdentityExpiryDate().format(dateFmt) : "")
//                    .identityIssuePlace(p.getIdentityIssuePlace() != null ? p.getIdentityIssuePlace() : "")
//                    .primary(p.isPrimary() ? "X" : "")
//                    .documentStatus(switch (p.getDocumentStatus()) {
//                        case NOT_REQUIRED -> "Không yêu cầu";
//                        case WAITING      -> "Chờ nộp";
//                        case IN_REVIEW    -> "Đang duyệt";
//                        case COMPLETED    -> "Đã đủ";
//                        case REJECTED     -> "Bị từ chối";
//                    })
//                    .build());
//        }
//
//        String sheetName = "Đoàn " + booking.getBookingCode();
//        EasyExcel.write(response.getOutputStream(), ParticipantExcelModel.class)
//                .sheet(sheetName)
//                .doWrite(excelData);
//    }
//
//    // ==================== HELPER ====================
//    private Booking findById(int bookingId) {
//        return bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
//    }
//
//    /** Mã đơn dạng BK260814-K7M2, thử lại nếu trùng (xác suất rất thấp). */
//    private String generateBookingCode() {
//        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
//        String code;
//        do {
//            StringBuilder suffix = new StringBuilder();
//            for (int i = 0; i < 4; i++) {
//                suffix.append(CODE_CHARS.charAt(ThreadLocalRandom.current().nextInt(CODE_CHARS.length())));
//            }
//            code = "BK" + datePart + "-" + suffix;
//        } while(bookingRepository.existsByBookingCode(code));
//        return code;
//    }
//
//    private BookingResponse mapToResponse(Booking b) {
//        TourSchedule schedule = b.getTourSchedule();
//        Tour tour = schedule.getTour();
//        User user = b.getUser();
//
//        return BookingResponse.builder()
//                .bookingId(b.getBookingId())
//                .bookingCode(b.getBookingCode())
//                .bookingDate(b.getBookingDate())
//                .userId(user.getUserId())
//                .customerName(user.getFullName())
//                .customerPhone(user.getPhoneNumber())
//                .scheduleId(schedule.getScheduleId())
//                .tourCode(tour.getTourCode())
//                .tourName(tour.getTourName())
//                .departureDate(schedule.getDepartureDate())
//                .returnDate(schedule.getReturnDate())
//                .totalAdults(b.getTotalAdults())
//                .totalChildren(b.getTotalChildren())
//                .totalInfants(b.getTotalInfants())
//                .totalAmount(b.getTotalAmount())
//                .discountAmount(b.getDiscountAmount())
//                .finalAmount(b.getFinalAmount())
//                .bookingStatus(b.getBookingStatus())
//                .bookingPaymentStatus(b.getBookingPaymentStatus())
//                .build();
//    }
//
//    private BookingDetailResponse mapToDetailResponse(Booking b) {
//        TourSchedule schedule = b.getTourSchedule();
//        Tour tour = schedule.getTour();
//        User user = b.getUser();
//
//        List<BookingDetailResponse.ParticipantResponse> participants =
//                (b.getParticipants() == null ? Set.<BookingParticipant>of() : b.getParticipants())
//                        .stream()
//                        // Người đại diện đoàn hiển thị đầu tiên
//                        .sorted(Comparator.comparing((BookingParticipant p) -> p.isPrimary() ? 0 : 1)
//                                .thenComparing(BookingParticipant::getFullName))
//                        .map(p -> BookingDetailResponse.ParticipantResponse.builder()
//                                .participantId(p.getParticipantId())
//                                .fullName(p.getFullName())
//                                .dateOfBirth(p.getDateOfBirth())
//                                .gender(p.getGender())
//                                .nationality(p.getNationality())
//                                .identityType(p.getIdentityType())
//                                .identityNumber(p.getIdentityNumber())
//                                .participantType(p.getParticipantType())
//                                .primary(p.isPrimary())
//                                .singleRoom(p.isSingleRoom())
//                                .documentStatus(p.getDocumentStatus())
//                                .build())
//                        .toList();
//
//        return BookingDetailResponse.builder()
//                .bookingId(b.getBookingId())
//                .bookingCode(b.getBookingCode())
//                .bookingDate(b.getBookingDate())
//                .bookingStatus(b.getBookingStatus())
//                .bookingPaymentStatus(b.getBookingPaymentStatus())
//
//                .userId(user.getUserId())
//                .customerName(user.getFullName())
//                .customerEmail(user.getEmail())
//                .customerPhone(user.getPhoneNumber())
//
//                .scheduleId(schedule.getScheduleId())
//                .tourCode(tour.getTourCode())
//                .tourName(tour.getTourName())
//                .departureDate(schedule.getDepartureDate())
//                .returnDate(schedule.getReturnDate())
//                .durationDays(tour.getDurationDays())
//                .durationNights(tour.getDurationNights())
//                .scheduleStatus(schedule.getStatus())
//
//                .totalAdults(b.getTotalAdults())
//                .totalChildren(b.getTotalChildren())
//                .totalInfants(b.getTotalInfants())
//                .adultPriceAtBooking(b.getAdultPriceAtBooking())
//                .childPriceAtBooking(b.getChildPriceAtBooking())
//                .infantPriceAtBooking(b.getInfantPriceAtBooking())
//
//                .singleRoomSurchargeAtBooking(b.getSingleRoomSurchargeAtBooking())
//                .singleRoomCount(b.getSingleRoomCount())
//
//                .totalAmount(b.getTotalAmount())
//                .discountAmount(b.getDiscountAmount())
//                .pointsDiscountAmount(b.getPointsDiscountAmount())
//                .pointsRedeemed(b.getPointsRedeemed())
//                .finalAmount(b.getFinalAmount())
//
//                .specialRequests(b.getSpecialRequests())
//                .cancellationReason(b.getCancellationReason())
//                .cancelledAt(b.getCancelledAt())
//                .confirmedAt(b.getConfirmedAt())
//                .completedAt(b.getCompletedAt())
//
//                .participants(participants)
//                .build();
//    }
//}
