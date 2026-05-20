package com.auction.server.repository;

import com.auction.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    /**
     * Lọc user theo cột discriminator "role" dùng native SQL.
     * Không thể dùng derived query (findByRole) vì "role" là discriminator column, không phải Java field.
     */
    @Query(value = "SELECT * FROM users WHERE role = :role", nativeQuery = true)
    List<User> findAllByRole(@Param("role") String role);

    @Query(value = "SELECT COUNT(*) FROM users WHERE role = :role", nativeQuery = true)
    long countAllByRole(@Param("role") String role);

    /**
     * Cập nhật trực tiếp cột discriminator "role" trong DB.
     * Cần thiết vì Hibernate không cho phép thay đổi discriminator qua JPA entity thông thường.
     */
    @Modifying
    @Query(value = "UPDATE users SET role = :newRole WHERE id = :userId", nativeQuery = true)
    int updateRoleById(@Param("userId") Integer userId, @Param("newRole") String newRole);
}