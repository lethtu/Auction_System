package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.dto.ApiResponse;
import com.auction.server.view.EmailServer;
import com.auction.server.view.RqLoginSignup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthLoginSignup {
    private static final Logger logger = LoggerFactory.getLogger(AuthLoginSignup.class);
    @Autowired
    private RqLoginSignup rq;
    @Autowired
    private EmailServer emailServer;

    @PostMapping("/login")
    public ApiResponse<?> Login(@RequestBody Map<String, String> requests) {
        String username = requests.get("username");
        String password = requests.get("password");
        Optional<User> res = rq.login(username, password);
        if (res.isPresent()) {
            if (res.get().isBanned()) {
                logger.warn("User {} bị khóa tài khoản", username);
                return new ApiResponse<String>(403, "Tài khoản đã bị khóa", "");
            }
            logger.info("User {} đăng nhập thành công", username);
            return new ApiResponse<Optional<User>>(200, "Đăng nhập thành công", res);
        }
        else {
            logger.error("User {} đăng nhập thất bại", username);
            return new ApiResponse<String>(100, "Đăng nhập thất bại", "");
        }
    }

    @PostMapping("/signup")
    public ApiResponse<?> Signup(@RequestBody User newUser) {
        boolean check = rq.signup(newUser);
        System.out.println(newUser);
        System.out.println(newUser.getEmail() + " " + newUser.getFullname() + " " + newUser.getPassword());
        if (!check) {
            String body = "Xin chào " + newUser.getFullname() + ",\n\n"
                    + "Tài khoản của bạn (" + newUser.getUsername() + ") đã được tạo thành công.\n"
                    + "Chúc bạn có những phiên đấu giá tuyệt vời!\n\n"
                    + "Trân trọng,\nBan Quản Trị.";
            emailServer.SendEmail(newUser.getEmail(), "Đăng ký tài khoản thành công", body);
            logger.info("User {} đăng ký thành công", newUser.getUsername());
            return new ApiResponse<User>(200, "Đăng ký thành công", newUser);
        }
        else {
            logger.error("Lỗi đăng ký, username: {} hoặc email: {} đã tồn tại", newUser.getUsername(), newUser.getEmail());
            return new ApiResponse<User>(100, "Email hoặc Username đã tồn tại", newUser);
        }
    }
}
