package com.example.tour.payment.service.impl;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.booking.entity.Booking;
import com.example.tour.booking.repository.BookingRepository;
import com.example.tour.payment.constant.PaymentMethod;
import com.example.tour.payment.constant.TransactionStatus;
import com.example.tour.payment.dto.request.SepayWebhookRequest;
import com.example.tour.payment.entity.Payment;
import com.example.tour.payment.repository.PaymentRepository;
import com.example.tour.payment.service.SepayWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Trường hợp lý tưởng: khách chỉ quét QR (số tiền + nội dung = mã đơn cố định).
 * Khớp đơn PENDING + đủ tiền → lưu payment SUCCESS + xác nhận đơn.
 * Mọi trường hợp khác chỉ ghi log và bỏ qua (vẫn trả 200 để SePay không gửi lại).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SepayWebhookServiceImpl implements SepayWebhookService {

    private static final DateTimeFormatter SEPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public void handle(SepayWebhookRequest req) {
        if (!"in".equalsIgnoreCase(req.getTransferType())) return;

        String txnId = String.valueOf(req.getId());

        // SePay gửi lại cùng giao dịch (timeout / lỗi mạng) → bỏ qua
        if (paymentRepository.existsByMethodAndTransactionId(PaymentMethod.BANK_TRANSFER, txnId)) {
            log.info("[SePay] Bỏ qua giao dịch trùng id={}", txnId);
            return;
        }

        if (req.getCode() == null) {
            log.warn("[SePay] Giao dịch {} không có mã đơn, bỏ qua", txnId);
            return;
        }

        // lock dòng đơn để không đụng admin xác nhận/huỷ cùng lúc
        Booking booking = bookingRepository.findByBookingCodeForUpdate(req.getCode().trim().toUpperCase()).orElse(null);
        if (booking == null) {
            log.warn("[SePay] Giao dịch {}: không tìm thấy đơn {}", txnId, req.getCode());
            return;
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING || booking.getPaymentStatus() != PaymentStatus.PENDING) {
            log.warn("[SePay] Giao dịch {}: đơn {} đang {}/{}, bỏ qua", txnId, booking.getBookingCode(),
                    booking.getBookingStatus(), booking.getPaymentStatus());
            return;
        }
        if (req.getTransferAmount().compareTo(booking.getTotalAmount()) < 0) {
            log.warn("[SePay] Giao dịch {}: đơn {} chuyển thiếu ({} < {})", txnId, booking.getBookingCode(),
                    req.getTransferAmount(), booking.getTotalAmount());
            return;
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);   // dirty checking, không cần save

        paymentRepository.save(Payment.builder()
                .booking(booking)
                .transactionId(txnId)
                .amount(req.getTransferAmount())
                .method(PaymentMethod.BANK_TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .paymentDate(parseTime(req.getTransactionDate()))
                .gateway(req.getGateway())
                .referenceCode(req.getReferenceCode())
                .build());

        log.info("[SePay] Đơn {} đã thanh toán, txn={}", booking.getBookingCode(), txnId);
    }

    private LocalDateTime parseTime(String s) {
        try {
            return s == null ? LocalDateTime.now() : LocalDateTime.parse(s, SEPAY_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
