package com.auction.server.repository;

import com.auction.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer> {
<<<<<<< HEAD
    List<User> findByRole(String role);
=======
    List<User> findByAccountType(String accountType);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
}