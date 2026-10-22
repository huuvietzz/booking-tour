package com.example.tour.user.entity;

import com.example.tour.common.constant.Gender;
import com.example.tour.common.entity.BaseEntity;
import com.example.tour.user.constant.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User extends BaseEntity implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int userId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "address")
    private String address;

    @Column(name = "phone_number", unique = true, length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // ==========================================
    // Phần implements UserDetails — BẮT BUỘC
    // ==========================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        // Spring Security dùng field này để định danh user
        // Ta dùng email thay cho username
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Không có logic hết hạn tài khoản
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;  // bị khoá → LockedException
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Không có logic hết hạn mật khẩu
    }

    /** Chưa xác thực email thì chưa đăng nhập được. */
    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
}
