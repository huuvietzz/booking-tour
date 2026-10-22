package com.example.tour.tour.service;

import com.example.tour.tour.dto.request.LocationRequest;
import com.example.tour.tour.dto.response.LocationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LocationService {

    LocationResponse createLocation(LocationRequest request, MultipartFile image);

    LocationResponse updateLocation(int locationId, LocationRequest request,
                                    MultipartFile image, boolean removeImage);

    // Ẩn / hiện (thay cho xoá: tour có khoá ngoại location_id NOT NULL)
    LocationResponse changeStatus(int locationId, boolean active);

    Page<LocationResponse> getAllLocationsForAdmin(String keyword, Boolean isActive, Pageable pageable);

    LocationResponse getLocationByIdForAdmin(int locationId);

    List<LocationResponse> getAllLocations();

    LocationResponse getLocationById(int locationId);
}