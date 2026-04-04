package com.auction.server.repository;

import com.auction.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HandleLoginSignup extends JpaRepository<User, Integer> {
    Optional<User> findByUsernameAndPassword(String username, String password);
    boolean existsByUsernameOrEmail(String username, String email);
}
