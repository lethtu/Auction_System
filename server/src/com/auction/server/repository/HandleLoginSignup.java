package com.auction.server.repository;

import com.auction.server.model.user;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HandleLoginSignup extends JpaRepository<user, Integer> {
    Optional<user> findByUsernameAndPassword(String username, String password);
    boolean existsByUsernameOrEmail(String username, String email);
}
