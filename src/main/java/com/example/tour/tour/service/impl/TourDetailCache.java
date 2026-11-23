package com.example.tour.tour.service.impl;

import com.example.tour.common.constant.CacheNames;
import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.tour.dto.response.TourResponse;
import com.example.tour.tour.entity.Tour;
import com.example.tour.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cache chi tiết tour phía khách (Redis, 15 phút).
 *
 * Tách thành bean riêng vì 2 lý do:
 *  1. Spring cache chạy qua proxy → TourServiceImpl gọi this.load() thì @Cacheable KHÔNG chạy;
 *     gọi sang bean khác mới chạy.
 *  2. getTourById còn tăng lượt xem mỗi lần mở → không cache cả hàm đó được, chỉ cache phần dữ liệu tour.
 *
 * Xoá cache: TourServiceImpl (sửa / ẩn / xoá tour), Category/LocationServiceImpl (đổi tên, ẩn),
 * TourCacheEvictor (đánh giá mới làm đổi điểm trung bình).
 */
@Component
@RequiredArgsConstructor
public class TourDetailCache {

    private final TourRepository tourRepository;

    @Cacheable(cacheNames = CacheNames.TOUR_DETAIL, key = "#tourId")
    @Transactional(readOnly = true)   // toTourResponse đọc ảnh, lịch trình (LAZY) → cần transaction
    public TourResponse load(int tourId) {
        Tour tour = tourRepository.findPublicById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));   // ném lỗi → không lưu vào cache
        return TourServiceImpl.toTourResponse(tour);
    }
}
