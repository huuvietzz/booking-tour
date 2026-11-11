package com.example.tour.user.service;

import com.example.tour.tour.dto.response.TourCardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FavoriteService {

    void addFavorite(int userId, int tourId);

    void removeFavorite(int userId, int tourId);

    Page<TourCardResponse> getMyFavorites(int userId, Pageable pageable);

    List<Integer> getMyFavoriteTourIds(int userId);
}
