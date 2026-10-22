package com.example.tour.tour.entity;

import com.example.tour.common.entity.BaseEntity;
import com.example.tour.tour.constant.TourClass;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tours")
@Setter @Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tour extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tour_id")
    private Integer tourId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "tour_code", nullable = false, unique = true, length = 50)
    private String tourCode;

    @Column(name = "tour_name", nullable = false)
    private String tourName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "tour_class")
    private TourClass tourClass = TourClass.STANDARD;

    @Column(name = "departure_location", nullable = false, length = 100)
    private String departureLocation;

    @Column(name = "duration", nullable = false, length = 20)
    private String duration;

    @Column(name = "base_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal basePrice;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Builder.Default
    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "view_count")
    private Integer viewCount = 0;

    /** Denormalized: module review cập nhật, không tính lại lúc đọc. */
    @Builder.Default
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Builder.Default
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Itinerary> itineraries = new ArrayList<>();

    // Lịch khởi hành có booking tham chiếu -> KHÔNG cascade/orphanRemoval,
    // tránh xóa nhầm lịch đã có người đặt khi sửa tour.
    @Builder.Default
    @OneToMany(mappedBy = "tour")
    private List<Schedule> schedules = new ArrayList<>();
}
