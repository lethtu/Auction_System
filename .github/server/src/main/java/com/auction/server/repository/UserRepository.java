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
     * Filter users by discriminator "role" column using native SQL.
     * Cannot use derived query (findByRole) because "role" is a discriminator column, not a Java field.
     */
    @Query(value = "SELECT * FROM users WHERE role = :role", nativeQuery = true)
    List<User> findAllByRole(@Param("role") String role);

    @Query(value = "SELECT COUNT(*) FROM users WHERE role = :role", nativeQuery = true)
    long countAllByRole(@Param("role") String role);

    /**
     * Update discriminator "role" column directly in DB.
     * Required because Hibernate does not allow changing discriminator via normal JPA entity.
     */
    @Modifying
    @Query(value = "UPDATE users SET role = :newRole WHERE id = :userId", nativeQuery = true)
    int updateRoleById(@Param("userId") Integer userId, @Param("newRole") String newRole);
}