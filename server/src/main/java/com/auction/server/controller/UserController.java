package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.model.User;
import com.auction.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final int SUCCESS_STATUS = 200;
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

    private ApiResponse<List<User>> success(List<User> users) {
        return new ApiResponse<>(SUCCESS_STATUS, SUCCESS_MESSAGE, users);
    }
}