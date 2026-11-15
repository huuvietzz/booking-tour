//package com.example.tour.booking.job;
//
//import com.example.tour.booking.repository.BookingRepository;
//import com.example.tour.booking.service.BookingService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
///** Việc chạy nền của module đặt tour. Cần @EnableScheduling (ChatbotConfig đã bật) */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class BookingJob {
//
//    private final BookingRepository bookingRepository;
//    private final BookingService bookingService;
//
//    @Value("${booking.hold-minutes:60}")
//    private long holdMinutes;
//
//    /**
//     * Mỗi phút: huỷ đơn giữ chỗ quá hạn mà khách chưa báo thanh toán → trả chỗ, trả lượt mã KM.
//     * Mỗi đơn huỷ trong 1 transaction riêng (gọi qua bean service): 1 đơn lỗi không kéo cả mẻ rollback.
//     */
//    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
//    public void cancelExpiredHolds() {
//        List<Integer> ids = bookingRepository.findExpiredHoldIds(LocalDateTime.now().minusMinutes(holdMinutes));
//        int cancelled = 0;
//        for (Integer id : ids) {
//            try {
//                if (bookingService.cancelExpiredHold(id)) cancelled++;
//            } catch (Exception e) {
//                log.error("Không huỷ được đơn quá hạn #{}", id, e);
//            }
//        }
//        if (cancelled > 0) log.info("Đã huỷ {} đơn quá hạn thanh toán", cancelled);
//    }
//
//    /** 0h05 mỗi ngày: đơn đã xác nhận mà tour đã về → Hoàn thành */
//    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
//    public void completeFinishedTours() {
//        int n = bookingService.completeFinishedTours();
//        if (n > 0) log.info("Đã chuyển {} đơn sang Hoàn thành", n);
//    }
//}
