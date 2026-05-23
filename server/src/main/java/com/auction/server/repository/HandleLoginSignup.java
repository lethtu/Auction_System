package com.auction.server.repository;

import com.auction.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HandleLoginSignup extends JpaRepository<User, Integer> {
    @Query("SELECT u FROM User u WHERE (u.username = :loginId OR u.email = :loginId) AND u.password = :password")
    Optional<User> findByUsernameOrEmailAndPassword(@Param("loginId") String loginId, @Param("password") String password);

    @Query("SELECT u FROM User u WHERE u.username = :loginId OR u.email = :loginId")
    Optional<User> findByUsernameOrEmail(@Param("loginId") String loginId);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByUsernameOrEmail(String username, String email);
}
