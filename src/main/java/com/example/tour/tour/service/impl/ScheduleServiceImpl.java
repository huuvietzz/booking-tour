package com.example.tour.tour.service.impl;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.repository.BookingRepository;
import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.tour.constant.ScheduleStatus;
import com.example.tour.tour.dto.request.ScheduleFilterRequest;
import com.example.tour.tour.dto.request.ScheduleRequest;
import com.example.tour.tour.dto.response.ScheduleResponse;
import com.example.tour.tour.entity.Schedule;
import com.example.tour.tour.entity.Tour;
import com.example.tour.tour.repository.ScheduleRepository;
import com.example.tour.tour.repository.TourRepository;
import com.example.tour.tour.service.ScheduleService;
import com.example.tour.tour.specification.ScheduleSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    /** Booking còn hiệu lực: chưa huỷ, chưa hoàn thành → còn "giữ chỗ" trên lịch */
    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.PAID);

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern DURATION_DAYS = Pattern.compile("^\\s*(\\d+)\\s*N", Pattern.CASE_INSENSITIVE);

    private final ScheduleRepository scheduleRepository;
    private final TourRepository tourRepository;
    private final BookingRepository bookingRepository;

    // =====================================================================
    // THÊM
    // =====================================================================
    @Override
    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request) {
        Tour tour = tourRepository.findById(request.getTourId())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        LocalDate departure = request.getDepartureDate();
        LocalDate returnDate = resolveReturnDate(tour, departure);
        validateDates(departure, returnDate);

        if (scheduleRepository.existsByTour_TourIdAndDepartureDate(tour.getTourId(), departure)) {
            throw new AppException(ErrorCode.SCHEDULE_DATE_EXISTS);
        }

        Schedule schedule = Schedule.builder()
                .tour(tour)
                .scheduleCode(buildCode(tour, departure))
                .departureDate(departure)
                .returnDate(returnDate)
                .totalSlots(request.getTotalSlots())
                .availableSlots(request.getTotalSlots())
                .adultPrice(tour.getBasePrice())
                .childPrice(request.getChildPrice())
                .toddlerPrice(request.getToddlerPrice())
                .singleRoomSupplement(request.getSingleRoomSupplement())
                .transport(trimToNull(request.getTransport()))
                .status(ScheduleStatus.AVAILABLE)
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        return toResponse(savedSchedule);
    }

    // =====================================================================
    // SỬA
    // =====================================================================
    @Override
    @Transactional
    public ScheduleResponse updateSchedule(int scheduleId, ScheduleRequest request) {
        // Khoá dòng: tránh tính sai số chỗ đã đặt khi đúng lúc này có khách đang đặt
        Schedule schedule = scheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        Tour tour = schedule.getTour();   // lịch không đổi tour; request.tourId bị bỏ qua khi sửa

        int booked = schedule.getTotalSlots() - schedule.getAvailableSlots();
        boolean hasBookings = bookingRepository.existsBySchedule_ScheduleId((scheduleId));

        LocalDate departure = request.getDepartureDate();
        LocalDate returnDate = resolveReturnDate(tour, departure);

        boolean datesChanged = !departure.equals(schedule.getDepartureDate())
                || !returnDate.equals(schedule.getReturnDate());
        boolean pricesChanged = request.getChildPrice().compareTo(schedule.getChildPrice()) != 0
                || request.getToddlerPrice().compareTo(schedule.getToddlerPrice()) != 0
                || request.getSingleRoomSupplement().compareTo(schedule.getSingleRoomSupplement()) != 0;

        // Đã có khách đặt → ngày đi / ngày về / giá đã "chốt" với khách, không được đổi
        if (hasBookings && (datesChanged || pricesChanged)) {
            throw new AppException(ErrorCode.SCHEDULE_LOCKED_BY_BOOKINGS);
        }
        if (datesChanged) {
            validateDates(departure, returnDate);
            if (scheduleRepository.existsByTour_TourIdAndDepartureDateAndScheduleIdNot(
                    tour.getTourId(), departure, scheduleId)) {
                throw new AppException(ErrorCode.SCHEDULE_DATE_EXISTS);
            }
            schedule.setDepartureDate(departure);
            schedule.setReturnDate(returnDate);
            schedule.setScheduleCode(buildCode(tour, departure));   // chưa có booking nên đổi mã an toàn
        }
        // Tổng chỗ mới không được ít hơn số chỗ khách đã giữ
        if (request.getTotalSlots() < booked) {
            throw new AppException(ErrorCode.SCHEDULE_SLOTS_TOO_LOW);
        }
        schedule.setTotalSlots(request.getTotalSlots());
        schedule.setAvailableSlots(request.getTotalSlots() - booked);
        schedule.setChildPrice(request.getChildPrice());
        schedule.setToddlerPrice(request.getToddlerPrice());
        schedule.setSingleRoomSupplement(request.getSingleRoomSupplement());
        schedule.setTransport(trimToNull(request.getTransport()));
        if (schedule.getStatus() != ScheduleStatus.CANCELLED) {
            schedule.setStatus(slotStatus(schedule.getAvailableSlots()));
        }

        Schedule savedSchedule = scheduleRepository.save(schedule);
        return toResponse(savedSchedule);
    }

    // =====================================================================
    // HUỶ / MỞ LẠI
    // =====================================================================
    @Override
    @Transactional
    public ScheduleResponse cancelSchedule(int scheduleId) {
        Schedule schedule = findWithTour(scheduleId);
        if (schedule.getStatus() == ScheduleStatus.CANCELLED) return toResponse(schedule);

        // Còn khách đang giữ chỗ → admin phải xử lý (huỷ + hoàn tiền) các đơn đó trước
        if (bookingRepository.countBySchedule_ScheduleIdAndBookingStatusIn(scheduleId, ACTIVE_BOOKING_STATUSES) > 0) {
            throw new AppException(ErrorCode.SCHEDULE_HAS_ACTIVE_BOOKINGS);
        }
        schedule.setStatus(ScheduleStatus.CANCELLED);

        Schedule savedSchedule = scheduleRepository.save(schedule);
        return toResponse(savedSchedule);
    }

    @Override
    @Transactional
    public ScheduleResponse reopenSchedule(int scheduleId) {
        Schedule schedule = findWithTour(scheduleId);
        if (schedule.getStatus() != ScheduleStatus.CANCELLED) {
            return toResponse(schedule);
        }
        if (!schedule.getDepartureDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_SCHEDULE_DATE);   // lịch đã qua, không mở lại
        }
        schedule.setStatus(slotStatus(schedule.getAvailableSlots()));

        Schedule savedSchedule = scheduleRepository.save(schedule);
        return toResponse(savedSchedule);
    }

    // =====================================================================
    // XOÁ CỨNG
    // =====================================================================
    @Override
    @Transactional
    public void deleteSchedule(int scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        // Có booking (kể cả đã huỷ) → giữ lại để đơn hàng, thanh toán, đánh giá còn tham chiếu được
        if (bookingRepository.existsBySchedule_ScheduleId(scheduleId)) {
            throw new AppException(ErrorCode.SCHEDULE_HAS_BOOKINGS);
        }
        scheduleRepository.delete(schedule);
    }

    // =====================================================================
    // ĐỌC
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ScheduleResponse> getAllSchedulesForAdmin(ScheduleFilterRequest filter, Pageable pageable) {
        return scheduleRepository.findAll(ScheduleSpecification.adminFilter(filter), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleResponse getScheduleByIdForAdmin(int scheduleId) {
        Schedule schedule = findWithTour(scheduleId);
        return toResponse(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getUpcomingSchedulesOfTour(int tourId) {
        // Tour (cùng danh mục, địa điểm) phải đang hiển thị với khách
        tourRepository.findPublicById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        return scheduleRepository.findUpcomingByTourIds(List.of(tourId), LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    // =====================================================================
    // HỖ TRỢ
    // =====================================================================
    private Schedule findWithTour(int scheduleId) {
        return scheduleRepository.findWithTourByScheduleId(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    /** Ngày đi phải sau hôm nay; ngày về không trước ngày đi */
    private static void validateDates(LocalDate departure, LocalDate returnDate) {
        if (!departure.isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_SCHEDULE_DATE);
        }
        if (returnDate.isBefore(departure)) {
            throw new AppException(ErrorCode.INVALID_RETURN_DATE);
        }
    }

    /** Ngày về = ngày đi + (số ngày của tour - 1). "3N2Đ" → 3 ngày */
    private static LocalDate resolveReturnDate(Tour tour, LocalDate departure) {
        Matcher m = DURATION_DAYS.matcher(Objects.toString(tour.getDuration(), ""));
        int days = m.find() ? Math.max(Integer.parseInt(m.group(1)), 1) : 1;
        return departure.plusDays(days - 1L);
    }

    /** TOUR00012 + 13/10/2026 → TOUR00012-20261013 (không trùng nhờ UNIQUE tour + ngày đi) */
    private static String buildCode(Tour tour, LocalDate departure) {
        return tour.getTourCode() + "-" + departure.format(CODE_DATE);
    }

    private static ScheduleStatus slotStatus(int availableSlots) {
        return availableSlots <= 0 ? ScheduleStatus.FULL : ScheduleStatus.AVAILABLE;
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private ScheduleResponse toResponse(Schedule s) {
        Tour t = s.getTour();
        return ScheduleResponse.builder()
                .scheduleId(s.getScheduleId())
                .scheduleCode(s.getScheduleCode())
                .tourId(t.getTourId())
                .tourCode(t.getTourCode())
                .tourName(t.getTourName())
                .duration(t.getDuration())
                .departureDate(s.getDepartureDate())
                .returnDate(s.getReturnDate())
                .totalSlots(s.getTotalSlots())
                .availableSlots(s.getAvailableSlots())
                .bookedSlots(s.getTotalSlots() - s.getAvailableSlots())
                .adultPrice(s.getAdultPrice())
                .childPrice(s.getChildPrice())
                .toddlerPrice(s.getToddlerPrice())
                .singleRoomSupplement(s.getSingleRoomSupplement())
                .transport(s.getTransport())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
