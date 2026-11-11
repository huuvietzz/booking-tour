package com.example.tour.user.repository;

import com.example.tour.user.constant.Role;
import com.example.tour.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    /** Dùng cho đăng nhập trang user: gõ email hay số điện thoại đều được. */
    @Query("SELECT u FROM User u WHERE u.email = :input OR u.phoneNumber = :input")
    Optional<User> findByEmailOrPhoneNumber(@Param("input") String input);

    /** Tìm theo id NHƯNG phải đúng role: trang khách hàng không đụng được tài khoản nhân viên và ngược lại */
    Optional<User> findByUserIdAndRole(Integer userId, Role role);

    /** Tìm theo id, role phải nằm trong danh sách (ADMIN, STAFF) */
    Optional<User> findByUserIdAndRoleIn(Integer userId, Collection<Role> roles);

    /** Số tài khoản đang hoạt động của 1 role. Viết JPQL vì tên field "isActive" làm Spring Data đọc nhầm "Is" thành từ khoá */
    @Query("select count(u) from User u where u.role = :role and u.isActive = true")
    long countActiveByRole(@Param("role") Role role);
}
