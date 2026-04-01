package com.auction.server.repository;

import com.auction.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HandleForgotPass extends JpaRepository<User, Integer>{
    Optional<User> findByEmail(String email);
}
