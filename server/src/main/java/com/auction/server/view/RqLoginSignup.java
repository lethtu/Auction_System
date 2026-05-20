package com.auction.server.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.model.Bidder;
import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RqLoginSignup {
    private static final Logger logger = LoggerFactory.getLogger(RqLoginSignup.class);

    @Autowired
    private HandleLoginSignup loginSignup;

    public Optional<User> login(String username, String pass) {
        return loginSignup.findByUsernameOrEmailAndPassword(username, pass);
    }

    public boolean signup(User newUser) {
        if (!loginSignup.existsByUsernameOrEmail(newUser.getUsername(), newUser.getEmail())) {
            logger.info("Đang thêm user: {}", newUser.getUsername());
            if (newUser.getPassword() == null) {
                logger.info("Lỗi password user {} bị null", newUser.getUsername());
                throw new RuntimeException("Lỗi: Password gửi lên bị null!");
            }

            // Luôn tạo Bidder khi đăng ký để Hibernate ghi discriminator "BIDDER" vào DB
            Bidder bidder = new Bidder();
            bidder.setUsername(newUser.getUsername());
            bidder.setPassword(newUser.getPassword());
            bidder.setFullname(newUser.getFullname());
            bidder.setEmail(newUser.getEmail());
            bidder.setDob(newUser.getDob());
            bidder.setPlace_of_birth(newUser.getPlace_of_birth());
            bidder.setBalance(newUser.getBalance() != null ? newUser.getBalance() : java.math.BigDecimal.ZERO);

            loginSignup.save(bidder);
            logger.info("Đã thêm thành công user: {} vào DB với role BIDDER", newUser.getUsername());
            return false;
        }
        logger.info("Username: {} hoặc Email: {} đã tồn tại trong DB", newUser.getUsername(), newUser.getEmail());
        return true;
    }
}

