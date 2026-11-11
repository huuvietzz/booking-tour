package com.example.tour.user.service.impl;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.tour.dto.response.TourCardResponse;
import com.example.tour.tour.mapper.TourCardMapper;
import com.example.tour.tour.repository.TourRepository;
import com.example.tour.user.repository.FavoriteRepository;
import com.example.tour.user.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final TourRepository tourRepository;
    private final TourCardMapper tourCardMapper;

    @Override
    @Transactional
    public void addFavorite(int userId, int tourId) {
        // Chỉ thích được tour khách đang xem được. Không tồn tại / đã ẩn → 404, giống trang chi tiết.
        if (tourRepository.findPublicById(tourId).isEmpty()) {
            throw new AppException(ErrorCode.TOUR_NOT_FOUND);
        }
        favoriteRepository.insertIfAbsent(userId, tourId);
    }

    @Override
    @Transactional
    public void removeFavorite(int userId, int tourId) {
        // Không kiểm tra tour: tour đã bị ẩn thì khách vẫn phải bỏ thích được
        favoriteRepository.deleteByUserIdAndTourId(userId, tourId);
    }

    /** Cùng mapper với danh sách tour public → cùng dạng thẻ, không N+1 */
    @Override
    @Transactional(readOnly = true)
    public Page<TourCardResponse> getMyFavorites(int userId, Pageable pageable) {
        return tourCardMapper.toCardPage(favoriteRepository.findFavoriteTours(userId, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getMyFavoriteTourIds(int userId) {
        return favoriteRepository.findTourIdsByUserId(userId);
    }
}
