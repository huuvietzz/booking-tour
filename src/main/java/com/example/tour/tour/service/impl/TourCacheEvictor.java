package com.example.tour.tour.service.impl;

import com.example.tour.common.constant.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Xoá cache chi tiết tour BẰNG CODE, cho chỗ không gắn @CacheEvict được
 * (hàm private, hoặc tourId không nằm trong tham số — vd ReviewServiceImpl.syncTourRating).
 * CacheManager đã bật transactionAware → lệnh xoá tự chờ transaction commit xong mới chạy.
 */
@Component
@RequiredArgsConstructor
public class TourCacheEvictor {

    private final CacheManager cacheManager;

    public void evictTourDetail(Integer tourId) {
        Cache cache = cacheManager.getCache(CacheNames.TOUR_DETAIL);
        if (cache != null && tourId != null) cache.evict(tourId);
    }
}
