package com.auction.server.view;

<<<<<<< HEAD
=======
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RqLoginSignup {
<<<<<<< HEAD
=======
    private static final Logger logger = LoggerFactory.getLogger(RqLoginSignup.class);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
    @Autowired
    private HandleLoginSignup LoginSignup;
    public Optional<User> login(String username, String pass){
        return LoginSignup.findByUsernameAndPassword(username, pass);
    }
    public boolean signup(User newUser){
        if (!LoginSignup.existsByUsernameOrEmail(newUser.getUsername(), newUser.getEmail())){
<<<<<<< HEAD
            System.out.println("Đang lưu user với pass: " + newUser.getPassword());

            if (newUser.getPassword() == null) {
=======
            logger.info("Đang thêm user: {}", newUser.getUsername());
            if (newUser.getPassword() == null) {
                logger.info("Lỗi password user {} bị null", newUser.getUsername());
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                throw new RuntimeException("Lỗi: Password gửi lên bị null!");
            }

            LoginSignup.save(newUser);
<<<<<<< HEAD
            return false;
        }
=======
            logger.info("Đã thêm thành công user: {} vào DB", newUser.getUsername());
            return false;
        }
        logger.info("Username: {} hoặc Email: {} đã tồn tại trong DB", newUser.getUsername(), newUser.getEmail());
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
        return true;
    }
}
