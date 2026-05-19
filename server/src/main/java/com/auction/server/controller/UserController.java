package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.model.User;
import com.auction.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final int SUCCESS_STATUS = 200;
    private static final int ERROR_STATUS = 400;
    private static final String SUCCESS_MESSAGE = "Thành công";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.status(404).body(error("Không tìm thấy người dùng", null));
        }
        return ResponseEntity.ok(success("Lấy thông tin tài khoản thành công", user));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        try {
            User updatedUser = userService.updateProfile(id, request);
            return ResponseEntity.ok(success("Cập nhật thông tin tài khoản thành công", updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage(), null));
        }
    }

    private ApiResponse<List<User>> success(List<User> users) {
        return new ApiResponse<>(SUCCESS_STATUS, SUCCESS_MESSAGE, users);
    }

    private ApiResponse<User> success(String message, User user) {
        return new ApiResponse<>(SUCCESS_STATUS, message, user);
    }

    private ApiResponse<User> error(String message, User user) {
        return new ApiResponse<>(ERROR_STATUS, message, user);
    }
}
