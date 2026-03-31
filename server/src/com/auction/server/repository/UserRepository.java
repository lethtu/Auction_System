package com.auction.server.repository;

import com.auction.server.model.user;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<user, String> {
    List<user> findByRole(String role);
}
