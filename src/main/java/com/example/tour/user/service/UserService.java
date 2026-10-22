package com.example.tour.user.service;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.common.service.FileStorageService;
import com.example.tour.common.util.SecurityUtils;
import com.example.tour.user.dto.request.ChangePasswordRequest;
import com.example.tour.user.dto.request.UpdateProfileRequest;
import com.example.tour.user.dto.response.MessageResponse;
import com.example.tour.user.dto.response.UserResponse;
import com.example.tour.user.entity.User;
import com.example.tour.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    /** Hồ sơ của chính người đang đăng nhập. */
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        // Đọc lại từ DB thay vì dùng thẳng principal, vì principal là bản
        // chụp lúc filter chạy, có thể cũ nếu vừa cập nhật ở tab khác.
        User user = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        User user = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // SĐT là UNIQUE: chỉ kiểm khi user thực sự đổi sang số mới
        if (!Objects.equals(user.getPhoneNumber(), request.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        // email, role, isActive, emailVerified KHÔNG nhận từ client

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateMyAvatar(MultipartFile file) {
        User user = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String oldUrl = user.getAvatarUrl();
        String newUrl = fileStorageService.storeAvatar(file);

        user.setAvatarUrl(newUrl);
        UserResponse response = toResponse(userRepository.save(user));

        cleanupAfterTransaction(oldUrl, newUrl);
        return response;
    }

    @Transactional
    public UserResponse deleteMyAvatar() {
        User user = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String oldUrl = user.getAvatarUrl();
        user.setAvatarUrl(null);
        UserResponse response = toResponse(userRepository.save(user));

        cleanupAfterTransaction(oldUrl, null);
        return response;
    }

    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest request) {
        User user = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Bắt buộc: không có bước này thì ai mượn được máy đang đăng nhập
        // là đổi mật khẩu chiếm tài khoản luôn.
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new MessageResponse("Đổi mật khẩu thành công.");
    }

    /**
     * Chỉ xoá file SAU KHI biết kết quả transaction:
     * - Commit thành công → xoá ảnh cũ
     * - Rollback → xoá ảnh mới vừa lưu (DB vẫn trỏ ảnh cũ, giữ nguyên)
     * Xoá ngay trong method thì nếu commit lỗi, DB trỏ tới ảnh cũ đã bị xoá → ảnh vỡ.
     */
    private void cleanupAfterTransaction(String oldUrl, String newUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    fileStorageService.deleteAvatar(oldUrl);
                } else {
                    fileStorageService.deleteAvatar(newUrl);
                }
            }
        });
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
