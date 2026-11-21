package com.example.tour.chatbot.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Truy vấn phụ cho chatbot bằng SQL thuần (NamedParameterJdbcTemplate, Spring Boot tự tạo sẵn).
 * Chỉ đọc vài cột cần cho prompt → nhẹ hơn nạp cả entity, và không dính LazyInitializationException
 * (ChatbotServiceImpl cố ý chạy ngoài transaction vì gọi AI rất lâu).
 */
@Repository
@RequiredArgsConstructor
public class ChatbotQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Danh mục + hạng của tour (tourClass là tên enum: BUDGET, STANDARD, GOOD_PRICE, PREMIUM) */
    public record TourMeta(String categoryName, String tourClass) {}

    public record ItineraryDay(int tourId, int dayNumber, String title, String description) {}

    /** tourId → danh mục + hạng, cho mọi tour (bảng nhỏ, 1 câu JOIN) */
    public Map<Integer, TourMeta> findTourMeta() {
        record Row(int id, TourMeta meta) {}
        return jdbc.query("""
                        SELECT t.tour_id, c.category_name, t.tour_class
                        FROM tours t JOIN categories c ON c.category_id = t.category_id
                        """, Map.of(),
                        (rs, i) -> new Row(rs.getInt(1), new TourMeta(rs.getString(2), rs.getString(3))))
                .stream().collect(Collectors.toMap(Row::id, Row::meta, (a, b) -> a));
    }

    /** id các tour có lịch khởi hành (chưa huỷ) trong khoảng ngày → dùng cho câu hỏi "tour tháng 12" */
    public Set<Integer> findTourIdsDepartingBetween(LocalDate from, LocalDate to) {
        return new HashSet<>(jdbc.queryForList("""
                        SELECT DISTINCT tour_id FROM schedules
                        WHERE departure_date BETWEEN :from AND :to
                          AND (status IS NULL OR status <> 'CANCELLED')
                        """, Map.of("from", from, "to", to), Integer.class));
    }

    /** Lịch trình từng ngày của các tour, sắp theo tour rồi theo ngày */
    public List<ItineraryDay> findItineraries(Collection<Integer> tourIds) {
        if (tourIds.isEmpty()) return List.of();   // IN () rỗng là lỗi SQL
        return jdbc.query("""
                        SELECT tour_id, day_number, title, description FROM itineraries
                        WHERE tour_id IN (:ids) ORDER BY tour_id, day_number
                        """, Map.of("ids", tourIds),
                (rs, i) -> new ItineraryDay(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4)));
    }
}
