package com.example.tour.payment.job;

import com.example.tour.payment.constant.PaymentMethod;
import com.example.tour.payment.constant.TransactionStatus;
import com.example.tour.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Lần thanh toán VNPAY bị bỏ dở (khách mở cổng VNPAY rồi tắt tab, không nhập OTP...) → không bao giờ có IPN
 * → dòng payments nằm mãi ở PENDING ("Đang chờ"). Job này chuyển chúng sang FAILED.
 *
 * Vì sao 20 phút: link VNPAY hết hạn sau 15 phút (vnp_ExpireDate) → quá 20 phút chắc chắn khách không trả được nữa,
 * dư 5 phút cho IPN về trễ.
 * Nếu sau đó VNPAY vẫn gửi IPN thành công (rất hiếm) → VnpayServiceImpl cho phép FAILED → SUCCESS, tiền không bị mất.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpireJob {

    private static final long EXPIRE_MINUTES = 20;

    private final PaymentRepository paymentRepository;

    @Scheduled(fixedDelay = 5 * 60_000, initialDelay = 60_000)   // 5 phút / lần, chạy lần đầu sau 1 phút
    @Transactional
    public void expirePendingVnpayPayments() {
        LocalDateTime now = LocalDateTime.now();
        int n = paymentRepository.expirePending(
                PaymentMethod.VNPAY, TransactionStatus.PENDING, TransactionStatus.FAILED,
                now.minusMinutes(EXPIRE_MINUTES), now);
        if (n > 0) log.info("[PaymentJob] Chuyển {} giao dịch VNPAY bỏ dở sang Thất bại", n);
    }
}

/*
* Chỉ áp dụng VNPAY: SePay chỉ lưu dòng khi tiền đã về (SUCCESS) nên không có PENDING.
Test nhanh: tạm đổi EXPIRE_MINUTES = 1 và fixedDelay = 30_000, mở cổng VNPAY rồi tắt tab,
chờ ~1–2 phút → trang Quản lý thanh toán dòng đó chuyển "Thất bại". Test xong nhớ đổi lại.
* */