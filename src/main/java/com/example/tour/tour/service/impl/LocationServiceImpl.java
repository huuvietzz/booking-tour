package com.example.tour.tour.service.impl;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.common.service.FileStorageService;
import com.example.tour.tour.dto.request.LocationRequest;
import com.example.tour.tour.dto.response.LocationResponse;
import com.example.tour.tour.entity.Location;
import com.example.tour.tour.repository.LocationRepository;
import com.example.tour.tour.service.LocationService;
import com.example.tour.tour.specification.LocationSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final FileStorageService fileStorageService;

    private static final String IMAGE_FOLDER = "locations";

    @Override
    @Transactional
    public LocationResponse createLocation(LocationRequest request, MultipartFile image) {
        Location location = new Location();
        location.setLocationName(request.getLocationName().trim());
        location.setDescription(request.getDescription());
        location.setIsActive(request.getIsActive());

        // Có chọn ảnh mới lưu file
        String newUrl = null;
        if (image != null && !image.isEmpty()) {
            newUrl = fileStorageService.storeImage(image, IMAGE_FOLDER);
            location.setImageUrl(newUrl);
        }

        Location savedLocation = locationRepository.save(location);

        // Lỗi DB khi commit → xoá file vừa lưu; thành công → giữ nguyên
        fileStorageService.cleanupAfterTransaction(null, newUrl);
        return toLocationResponse(savedLocation);
    }

    @Override
    @Transactional
    public LocationResponse updateLocation(int locationId, LocationRequest request,
                                           MultipartFile image, boolean removeImage) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));

        location.setLocationName(request.getLocationName().trim());
        location.setDescription(request.getDescription());
        location.setIsActive(request.getIsActive());

        // ----- Ảnh -----
        String oldUrl   = location.getImageUrl();
        String newUrl   = null;
        String toDelete = null;   // ảnh cũ cần xoá SAU KHI commit thành công

        if (image != null && !image.isEmpty()) {
            newUrl = fileStorageService.storeImage(image, IMAGE_FOLDER);   // chọn ảnh mới → thay ảnh cũ
            location.setImageUrl(newUrl);
            toDelete = oldUrl;
        } else if (removeImage) {
            location.setImageUrl(null);                                     // bấm "Bỏ ảnh"
            toDelete = oldUrl;
        }
        // Không chọn ảnh, không xoá → giữ nguyên ảnh cũ

        Location savedLocation = locationRepository.save(location);

        fileStorageService.cleanupAfterTransaction(toDelete, newUrl);
        return toLocationResponse(savedLocation);
    }

    /**
     * Ẩn / hiện địa điểm (thay cho xoá).
     * Ẩn: không hiện cho khách nữa; tour, lịch khởi hành, đơn đã đặt giữ nguyên.
     */
    @Override
    @Transactional
    public LocationResponse changeStatus(int locationId, boolean active) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
        location.setIsActive(active);
        return toLocationResponse(locationRepository.save(location));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LocationResponse> getAllLocationsForAdmin(String keyword, Boolean isActive, Pageable pageable) {
        Specification<Location> spec = LocationSpecification.filter(keyword, isActive);
        return locationRepository.findAll(spec, pageable)
                .map(this::toLocationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocationByIdForAdmin(int locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
        return toLocationResponse(location);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations() {
        return locationRepository.findByIsActiveTrueOrderByLocationNameAsc()
                .stream()
                .map(this::toLocationResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocationById(int locationId) {
        // Khách chỉ xem được địa điểm đang hiển thị
        Location location = locationRepository.findByLocationIdAndIsActiveTrue(locationId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
        return toLocationResponse(location);
    }

    // Entity -> Response
    private LocationResponse toLocationResponse(Location location) {
        return LocationResponse.builder()
                .locationId(location.getLocationId())
                .locationName(location.getLocationName())
                .description(location.getDescription())
                .imageUrl(location.getImageUrl())
                .isActive(location.getIsActive())
                .createdAt(location.getCreatedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }
}