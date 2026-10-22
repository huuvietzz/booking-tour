package com.example.tour.user.repository;

import com.example.tour.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    /** Dùng cho đăng nhập trang user: gõ email hay số điện thoại đều được. */
    @Query("SELECT u FROM User u WHERE u.email = :input OR u.phoneNumber = :input")
    Optional<User> findByEmailOrPhoneNumber(@Param("input") String input);
}
