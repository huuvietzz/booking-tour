package com.example.tour.report.service.impl;

import com.example.tour.payment.constant.PaymentMethod;
import com.example.tour.payment.constant.TransactionStatus;
import com.example.tour.report.dto.response.*;
import com.example.tour.report.repository.PaymentReportRepository;
import com.example.tour.report.repository.PaymentReportRepository.DailyMethodRow;
import com.example.tour.report.service.PaymentReportService;
import com.example.tour.report.util.ReportRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.tour.report.repository.PaymentReportRepository.count;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentReportServiceImpl implements PaymentReportService {

    private final PaymentReportRepository paymentReportRepository;

    @Override
    public PaymentReportResponse getPaymentReport(LocalDate fromDate, LocalDate toDate) {
        ReportRange range = ReportRange.of(fromDate, toDate);
        List<DailyMethodRow> rows = paymentReportRepository.findDailyByMethod(range.start(), range.end());

        // Tổng theo phương thức — luôn trả đủ các phương thức để FE vẽ ổn định
        Map<PaymentMethod, long[]> txByMethod = new EnumMap<>(PaymentMethod.class);
        Map<PaymentMethod, BigDecimal> amountByMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod m : PaymentMethod.values()) {
            txByMethod.put(m, new long[1]);
            amountByMethod.put(m, BigDecimal.ZERO);
        }
        for (DailyMethodRow r : rows) {
            txByMethod.get(r.method())[0] += r.transactions();
            amountByMethod.merge(r.method(), r.amount(), BigDecimal::add);
        }
        BigDecimal total = amountByMethod.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalTx = txByMethod.values().stream().mapToLong(a -> a[0]).sum();

        List<PaymentMethodStatResponse> methods = Arrays.stream(PaymentMethod.values())
                .map(m -> PaymentMethodStatResponse.builder()
                        .method(m)
                        .transactions(txByMethod.get(m)[0])
                        .amount(amountByMethod.get(m))
                        .share(percent(amountByMethod.get(m), total))
                        .build())
                .toList();

        // Lần thử VNPAY: đo cổng có "chạy tốt" không (khách bỏ dở / lỗi thẻ nhiều → tỷ lệ thấp)
        List<Object[]> attempts = paymentReportRepository.countVnpayAttempts(range.start(), range.end());
        long ok = count(attempts, TransactionStatus.SUCCESS);
        long failed = count(attempts, TransactionStatus.FAILED);
        long pending = count(attempts, TransactionStatus.PENDING);

        return PaymentReportResponse.builder()
                .from(range.from()).to(range.to())
                .groupBy(range.groupBy())
                .totalAmount(total)
                .totalTransactions(totalTx)
                .methods(methods)
                .series(buildSeries(rows, range))
                .gateways(paymentReportRepository.findByGateway(range.start(), range.end()).stream()
                        .map(g -> PaymentGatewayStatResponse.builder()
                                .method(g.method())
                                .gateway(g.gateway() == null || g.gateway().isBlank() ? "Khác" : g.gateway())
                                .transactions(g.transactions())
                                .amount(g.amount())
                                .build())
                        .toList())
                .vnpayAttempts(ok + failed + pending)
                .vnpaySuccess(ok)
                .vnpayFailed(failed)
                .vnpayPending(pending)
                .vnpaySuccessRate(ok + failed == 0 ? 0 : Math.round(ok * 1000.0 / (ok + failed)) / 10.0)
                .build();
    }

    /** Đủ mọi ngày / tháng trong khoảng (không có giao dịch = 0) */
    private static List<PaymentPointResponse> buildSeries(List<DailyMethodRow> rows, ReportRange range) {
        Map<LocalDate, List<DailyMethodRow>> byBucket = rows.stream()
                .collect(Collectors.groupingBy(r -> range.bucketOf(r.date())));
        List<PaymentPointResponse> series = new ArrayList<>();
        for (LocalDate d = range.bucketOf(range.from()); !d.isAfter(range.to()); d = range.nextBucket(d)) {
            BigDecimal bank = BigDecimal.ZERO, vnpay = BigDecimal.ZERO;
            long tx = 0;
            for (DailyMethodRow r : byBucket.getOrDefault(d, List.of())) {
                tx += r.transactions();
                if (r.method() == PaymentMethod.VNPAY) vnpay = vnpay.add(r.amount());
                else bank = bank.add(r.amount());
            }
            series.add(PaymentPointResponse.builder().date(d).bankTransfer(bank).vnpay(vnpay).transactions(tx).build());
        }
        return series;
    }

    private static double percent(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) return 0;
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP).doubleValue();
    }
}
