package com.example.tour.review.entity;

import com.example.tour.booking.entity.Booking;
import com.example.tour.common.entity.BaseEntity;
import com.example.tour.tour.entity.Tour;
import com.example.tour.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // Mỗi booking chỉ được review 1 lần
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    // CHECK 1-5 nằm ở DB; validate thêm bằng @Min/@Max ở DTO request
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Builder.Default
    @Column(name = "is_approved")
    private Boolean is_approved = false;
}