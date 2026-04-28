package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.model.Bidder;
import com.auction.server.view.ApiResponse;
import com.auction.server.view.EmailServer;
import com.auction.server.view.RqLoginSignup;
<<<<<<< HEAD
=======
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthLoginSignup {
<<<<<<< HEAD
=======
    private static final Logger logger = LoggerFactory.getLogger(AuthLoginSignup.class);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
    @Autowired
    private RqLoginSignup rq;
    @Autowired
    private EmailServer emailServer;

    @PostMapping("/login")
<<<<<<< HEAD
    public ApiResponse Login(@RequestBody Map<String, String> requests) {
=======
    public ApiResponse<?> Login(@RequestBody Map<String, String> requests) {
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
        String username = requests.get("username");
        String password = requests.get("password");
        Optional<User> res = rq.login(username, password);
        if (res.isPresent()) {
<<<<<<< HEAD
            return new ApiResponse<Optional<User>>(200, "Đăng nhập thành công", res);
        } else {
=======
            logger.info("User {} đăng nhập thành công", username);
            return new ApiResponse<Optional<User>>(200, "Đăng nhập thành công", res);
        }
        else {
            logger.error("User {} đăng nhập thất bại", username);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            return new ApiResponse<String>(100, "Đăng nhập thất bại", "");
        }
    }

    @PostMapping("/signup")
<<<<<<< HEAD
    public ApiResponse Signup(@RequestBody User newUser) {
        boolean check = rq.signup(newUser);
        System.out.println(newUser);
        System.out.println(newUser.getEmail() + " " + newUser.getFullname() + " " + newUser.getPassword());
        if (check == false) {
=======
    public ApiResponse<?> Signup(@RequestBody User newUser) {
        boolean check = rq.signup(newUser);
        System.out.println(newUser);
        System.out.println(newUser.getEmail() + " " + newUser.getFullname() + " " + newUser.getPassword());
        if (!check) {
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            String body = "Xin chào " + newUser.getFullname() + ",\n\n"
                    + "Tài khoản của bạn (" + newUser.getUsername() + ") đã được tạo thành công.\n"
                    + "Chúc bạn có những phiên đấu giá tuyệt vời!\n\n"
                    + "Trân trọng,\nBan Quản Trị.";
            emailServer.SendEmail(newUser.getEmail(), "Đăng ký tài khoản thành công", body);
<<<<<<< HEAD
            return new ApiResponse(200, "Đăng ký thành công", newUser);
        } else {
            return new ApiResponse(100, "Email hoặc Username đã tồn tại", newUser);
=======
            logger.info("User {} đăng ký thành công", newUser.getUsername());
            return new ApiResponse<User>(200, "Đăng ký thành công", newUser);
        }
        else {
            logger.error("Lỗi đăng ký, username: {} hoặc email: {} đã tồn tại", newUser.getUsername(), newUser.getEmail());
            return new ApiResponse<User>(100, "Email hoặc Username đã tồn tại", newUser);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
        }
    }
}
