package com.example.tour.user.service;

import com.example.tour.user.dto.request.StaffCreateRequest;
import com.example.tour.user.dto.request.StaffFilterRequest;
import com.example.tour.user.dto.request.StaffUpdateRequest;
import com.example.tour.user.dto.response.StaffResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StaffService {

    Page<StaffResponse> getStaffs(StaffFilterRequest filter, Pageable pageable);

    StaffResponse getStaff(Integer userId);

    StaffResponse createStaff(StaffCreateRequest request);

    StaffResponse updateStaff(Integer userId, StaffUpdateRequest request);

    StaffResponse setActive(Integer userId, boolean active);

    void deleteStaff(Integer userId);
}
