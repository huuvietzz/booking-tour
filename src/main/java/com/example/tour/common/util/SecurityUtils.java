package com.example.tour.common.util;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Lấy user đang đăng nhập từ SecurityContext.
 * JwtAuthenticationFilter đã set principal là entity User (vì User implements UserDetails),
 * nên ép kiểu trực tiếp được, không cần query lại DB.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof User user)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Integer getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
}
