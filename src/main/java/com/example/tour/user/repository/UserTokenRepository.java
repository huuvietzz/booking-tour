package com.example.tour.user.repository;

import com.example.tour.user.constant.TokenType;
import com.example.tour.user.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<Token, Integer> {

    /** Luôn tra kèm tokenType để token xác thực email không dùng được cho reset password. */
    Optional<Token> findByTokenAndTokenType(String token, TokenType tokenType);

    void deleteByUser_UserIdAndTokenType(Integer userId, TokenType tokenType);
}
