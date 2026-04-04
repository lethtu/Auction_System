package com.auction.server.controller;

import java.util.List;
import com.auction.server.model.User;
import com.auction.server.repository.UserRepository;
import com.auction.server.view.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository; // Đổi tên công cụ cho chuẩn xác

    @GetMapping("/get_user")
    public ApiResponse<List<User>> get_user() {
        // Dùng siêu năng lực findAll() gọi đệ tử tàng hình lấy toàn bộ danh sách
        List<User> users = userRepository.findAll();

        return new ApiResponse<>(200, "Thành công", users);
    }
}