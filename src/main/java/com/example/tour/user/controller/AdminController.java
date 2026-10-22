package com.example.tour.user.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.user.dto.request.AdminUpdateProfileRequest;
import com.example.tour.user.dto.request.ChangePasswordRequest;
import com.example.tour.user.dto.response.AdminResponse;
import com.example.tour.user.dto.response.MessageResponse;
import com.example.tour.user.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/account-info")
    public ResponseEntity<ApiResponse<AdminResponse>> getMyProfile() {
        AdminResponse data = adminService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thành công", data));
    }

    @PutMapping("/account-info")
    public ResponseEntity<ApiResponse<AdminResponse>> updateMyProfile(
            @RequestBody @Valid AdminUpdateProfileRequest request) {
        AdminResponse data = adminService.updateMyProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", data));
    }

    @PostMapping(value = "/account-info/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        AdminResponse data = adminService.updateMyAvatar(file);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh đại diện thành công", data));
    }

    @DeleteMapping("/account-info/avatar")
    public ResponseEntity<ApiResponse<AdminResponse>> deleteAvatar() {
        AdminResponse data = adminService.deleteMyAvatar();
        return ResponseEntity.ok(ApiResponse.success("Đã xoá ảnh đại diện", data));
    }

    @PutMapping("/account-info/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request) {
        MessageResponse result = adminService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), null));
    }
}
