package com.example.tour.security.service;

import com.example.tour.user.constant.Role;
import com.example.tour.user.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtService là class Java thuần → new trực tiếp, không cần Spring.
 * @Value không chạy khi new → gán secretKey / expiration bằng ReflectionTestUtils.
 */
class JwtServiceTest {

    // Base64 của "test-secret-key-for-unit-test-only-1234567890" (45 byte = 360 bit ≥ 256 bit cho HS256).
    // KHÔNG dùng khoá thật trong application.properties.
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Qtb25seS0xMjM0NTY3ODkw";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 60_000L);   // 1 phút

        user = User.builder().userId(1).email("an@gmail.com").password("x")
                .role(Role.CUSTOMER).isActive(true).emailVerified(true).build();
    }

    @Test
    @DisplayName("Token sinh ra chứa email trong 'sub'")
    void tokenContainsEmail() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractEmail(token)).isEqualTo("an@gmail.com");
    }

    @Test
    @DisplayName("Token hợp lệ với đúng người dùng")
    void validForSameUser() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("Đổi email (sub khác) → token cũ KHÔNG còn hợp lệ")
    void invalidAfterEmailChange() {
        String token = jwtService.generateToken(user);
        User changed = User.builder().email("moi@gmail.com").password("x").role(Role.CUSTOMER)
                .isActive(true).emailVerified(true).build();
        assertThat(jwtService.isTokenValid(token, changed)).isFalse();
    }

    @Test
    @DisplayName("Token hết hạn → bị từ chối (thư viện ném ExpiredJwtException)")
    void expiredToken() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1_000L);   // hết hạn ngay khi tạo
        String token = jwtService.generateToken(user);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, user))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Token bị sửa phần chữ ký → bị từ chối")
    void tamperedToken() {
        String token = jwtService.generateToken(user);
        int dot = token.lastIndexOf('.');
        char first = token.charAt(dot + 1);
        // Đổi ký tự ĐẦU của chữ ký sang ký tự khác → chắc chắn chữ ký sai
        String tampered = token.substring(0, dot + 1) + (first == 'A' ? 'B' : 'A') + token.substring(dot + 2);

        assertThatThrownBy(() -> jwtService.extractEmail(tampered))
                .isInstanceOf(JwtException.class);
    }
}