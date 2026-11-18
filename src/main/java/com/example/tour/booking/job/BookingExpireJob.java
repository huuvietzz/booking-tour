package com.example.tour.booking.job;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.booking.repository.BookingRepository;
import com.example.tour.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mỗi phút: đơn ĐÃ XÁC NHẬN nhưng CHƯA THANH TOÁN quá 1 giờ kể từ lúc đặt
 * → Đã hủy, thanh toán giữ "Chưa thanh toán", trả lại chỗ + lượt mã khuyến mãi.
 *
 * Không đặt @Transactional ở đây: mỗi đơn huỷ trong 1 transaction riêng (gọi qua bookingService),
 * 1 đơn lỗi không làm hỏng các đơn khác.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpireJob {

    /** Giữ chỗ 1 giờ — khớp HOLD_MINUTES ở payment.html, email và VnpayServiceImpl */
    public static final int HOLD_MINUTES = 60;

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void cancelExpiredBookings() {
        LocalDateTime before = LocalDateTime.now().minusMinutes(HOLD_MINUTES);
        List<Integer> ids = bookingRepository.findIdsByStatusCreatedBefore(
                BookingStatus.CONFIRMED, PaymentStatus.PENDING, before);

        int cancelled = 0;
        for (Integer id : ids) {
            try {
                if (bookingService.expireUnpaidBooking(id)) cancelled++;
            } catch (Exception e) {
                log.error("[BookingJob] Không huỷ được đơn id={}", id, e);
            }
        }
        if (cancelled > 0) log.info("[BookingJob] Đã huỷ {} đơn quá hạn thanh toán", cancelled);
    }
}
