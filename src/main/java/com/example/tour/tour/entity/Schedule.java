package com.example.tour.tour.entity;

import com.example.tour.common.entity.BaseEntity;
import com.example.tour.tour.constant.ScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_tour_departure",
                columnNames = {"tour_id", "departure_date"}
        )
)
@Setter @Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Integer scheduleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(name = "schedule_code", nullable = false, unique = true, length = 50)
    private String scheduleCode;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    /** Trừ slot phải bọc transaction + PESSIMISTIC_WRITE, nếu không sẽ oversell. */
    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots;

    @Column(name = "adult_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal adultPrice;

    @Column(name = "child_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal childPrice;

    @Column(name = "toddler_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal toddlerPrice;

    @Column(name = "single_room_supplement", nullable = false, precision = 15, scale = 0)
    private BigDecimal singleRoomSupplement;

    @Column(name = "transport", length = 100)
    private String transport;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ScheduleStatus status = ScheduleStatus.AVAILABLE;
}
