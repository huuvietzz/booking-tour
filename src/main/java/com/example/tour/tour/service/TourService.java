package com.example.tour.tour.service;

import com.example.tour.tour.dto.request.TourFilterRequest;
import com.example.tour.tour.dto.request.TourRequest;
import com.example.tour.tour.dto.response.TourCardResponse;
import com.example.tour.tour.dto.response.TourResponse;
import com.example.tour.tour.dto.response.TourAdminRowResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TourService {

    TourResponse createTour(TourRequest request, MultipartFile thumbnail, List<MultipartFile> galleryImages);

    TourResponse updateTour(int tourId, TourRequest request, MultipartFile thumbnail,
                            boolean removeThumbnail, List<MultipartFile> galleryImages);

    /** Xoá cứng, chỉ khi tour chưa có booking nào */
    void deleteTour(int tourId);

    TourResponse changeStatus(int tourId, boolean active);

    Page<TourAdminRowResponse> getAllToursForAdmin(TourFilterRequest filter, Pageable pageable);

    TourResponse getTourByIdForAdmin(int tourId);

    Page<TourCardResponse> getAllTours(TourFilterRequest filter, Pageable pageable);

    /** Chi tiết phía khách: chỉ tour đang hiển thị, tăng lượt xem */
    TourResponse getTourById(int tourId);
}