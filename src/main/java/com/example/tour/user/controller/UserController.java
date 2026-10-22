package com.example.tour.user.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.user.dto.request.ChangePasswordRequest;
import com.example.tour.user.dto.request.UpdateProfileRequest;
import com.example.tour.user.dto.response.MessageResponse;
import com.example.tour.user.dto.response.UserResponse;
import com.example.tour.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Hồ sơ cá nhân. Mọi endpoint đều cần đăng nhập —
 * user lấy từ token, không nhận userId từ client.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/account-info")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        UserResponse data = userService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thành công", data));
    }

    @PutMapping("/account-info")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @RequestBody @Valid UpdateProfileRequest request) {
        UserResponse data = userService.updateMyProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", data));
    }

    @PostMapping(value = "/account-info/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        UserResponse data = userService.updateMyAvatar(file);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh đại diện thành công", data));
    }

    @DeleteMapping("/account-info/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> deleteAvatar() {
        UserResponse data = userService.deleteMyAvatar();
        return ResponseEntity.ok(ApiResponse.success("Đã xoá ảnh đại diện", data));
    }

    @PutMapping("/account-info/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request) {
        MessageResponse result = userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), null));
    }
}
