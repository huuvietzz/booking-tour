package com.example.tour.booking.entity;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.common.entity.BaseEntity;
import com.example.tour.payment.constant.TransactionStatus;
import com.example.tour.payment.entity.Payment;
import com.example.tour.promotion.entity.Promotion;
import com.example.tour.tour.entity.Schedule;
import com.example.tour.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @Column(name = "booking_code", nullable = false, unique = true, length = 50)
    private String bookingCode;

    // NULL khi khách đặt không đăng nhập
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    // ===== Thông tin liên lạc =====
    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "contact_address")
    private String contactAddress;

    // ===== Số lượng người =====
    @Builder.Default
    @Column(name = "total_adults", nullable = false)
    private Integer totalAdults = 1;

    @Builder.Default
    @Column(name = "total_children")
    private Integer totalChildren = 0;

    @Builder.Default
    @Column(name = "total_toddlers")
    private Integer totalToddlers = 0;

    @Column(name = "temp_amount", precision = 15, scale = 0)
    private BigDecimal tempAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal totalAmount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status")
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // Hành khách tạo cùng booking -> cascade
    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    // Payment có FK RESTRICT (lịch sử giao dịch) -> không cascade xóa
    @Builder.Default
    @OneToMany(mappedBy = "booking")
    @OrderBy("createdAt DESC")
    private List<Payment> payments = new ArrayList<>();

    public void addParticipant(Participant participant) {
        participants.add(participant);
        participant.setBooking(this);
    }

    public void removeParticipant(Participant participant) {
        participants.remove(participant);
        participant.setBooking(null);
    }
}

