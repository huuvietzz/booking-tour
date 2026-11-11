package com.example.tour.tour.mapper;

import com.example.tour.tour.dto.response.DepartureDateResponse;
import com.example.tour.tour.dto.response.TourCardResponse;
import com.example.tour.tour.entity.Tour;
import com.example.tour.tour.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Trang Tour → trang thẻ tour phía khách (kèm ngày khởi hành sắp tới).
 * Dùng CHUNG cho GET /api/public/tours (TourServiceImpl) và GET /api/favorites (FavoriteServiceImpl)
 * → 2 nơi luôn trả cùng 1 dạng, thêm field cho thẻ chỉ sửa 1 chỗ.
 */
@Component
@RequiredArgsConstructor
public class TourCardMapper {

    private final ScheduleRepository scheduleRepository;

    /** 1 câu truy vấn lấy ngày khởi hành của CẢ TRANG rồi chia theo tourId → không N+1 */
    public Page<TourCardResponse> toCardPage(Page<Tour> page) {
        List<Integer> tourIds = page.getContent().stream().map(Tour::getTourId).toList();
        Map<Integer, List<DepartureDateResponse>> datesByTour = tourIds.isEmpty()
                ? Map.of()
                : scheduleRepository.findUpcomingByTourIds(tourIds, LocalDate.now()).stream()
                .collect(Collectors.groupingBy(
                        s -> s.getTour().getTourId(),
                        Collectors.mapping(s -> DepartureDateResponse.builder()
                                .scheduleId(s.getScheduleId())
                                .departureDate(s.getDepartureDate())
                                .status(s.getStatus())
                                .build(), Collectors.toList())));

        return page.map(t -> toCard(t, datesByTour.getOrDefault(t.getTourId(), List.of())));
    }

    private TourCardResponse toCard(Tour t, List<DepartureDateResponse> dates) {
        return TourCardResponse.builder()
                .tourId(t.getTourId())
                .tourCode(t.getTourCode())
                .thumbnailUrl(t.getThumbnailUrl())
                .tourClass(t.getTourClass())
                .tourName(t.getTourName())
                .departureLocation(t.getDepartureLocation())
                .duration(t.getDuration())
                .basePrice(t.getBasePrice())
                .departureDates(dates)
                .build();
    }
}
