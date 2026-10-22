package com.example.tour.security.service;

import com.example.tour.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Tham số có thể là email hoặc số điện thoại (trang user cho phép cả hai).
     * JwtAuthenticationFilter luôn truyền email vì User.getUsername() trả email.
     */
    @Override
    public UserDetails loadUserByUsername(String emailOrPhone) throws UsernameNotFoundException {
        return userRepository.findByEmailOrPhoneNumber(emailOrPhone)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản: " + emailOrPhone));
    }
}

