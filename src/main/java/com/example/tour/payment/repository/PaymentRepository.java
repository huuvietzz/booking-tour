package com.example.tour.payment.repository;

import com.example.tour.payment.constant.PaymentMethod;
import com.example.tour.payment.constant.TransactionStatus;
import com.example.tour.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer>, JpaSpecificationExecutor<Payment> {

    boolean existsByMethodAndTransactionId(PaymentMethod method, String transactionId);

    /** Danh sách: fetch booking luôn 1 query, tránh N+1 */
    @Override
    @EntityGraph(attributePaths = {"booking"})
    Page<Payment> findAll(Specification<Payment> spec, Pageable pageable);

    /** Chi tiết: lấy luôn đơn + lịch + tour */
    @EntityGraph(attributePaths = {"booking", "booking.schedule", "booking.schedule.tour"})
    Optional<Payment> findWithBookingByPaymentId(Integer paymentId);

    long countByTransactionStatus(TransactionStatus status);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.transactionStatus = :status")
    BigDecimal sumAmountByStatus(@Param("status") TransactionStatus status);

    // khoá dòng payment khi xử lý IPN/Return (IPN và Return đến cùng lúc không ghi đè nhau)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findForUpdateByMethodAndTransactionId(PaymentMethod method, String transactionId);

    // ReturnUrl chỉ đọc (khi confirm-on-return=false)
    @EntityGraph(attributePaths = {"booking"})
    Optional<Payment> findByMethodAndTransactionId(PaymentMethod method, String transactionId);

    /** UPDATE 1 câu cho tất cả dòng quá hạn. Set updatedAt tay vì UPDATE hàng loạt không chạy auditing */
    @Modifying
    @Query("""
           update Payment p set p.transactionStatus = :failed, p.updatedAt = :now
           where p.method = :method and p.transactionStatus = :pending and p.createdAt < :before
           """)
    int expirePending(@Param("method") PaymentMethod method,
                      @Param("pending") TransactionStatus pending,
                      @Param("failed") TransactionStatus failed,
                      @Param("before") LocalDateTime before,
                      @Param("now") LocalDateTime now);
}
