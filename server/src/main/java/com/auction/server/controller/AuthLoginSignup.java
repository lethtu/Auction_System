package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.UserResponseDTO;
import com.auction.server.view.EmailServer;
import com.auction.server.view.RqLoginSignup;
import com.auction.server.util.EmailTemplateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

import com.auction.server.util.SessionManager;

@RestController
@RequestMapping("/api")
public class AuthLoginSignup {
    private static final Logger logger = LoggerFactory.getLogger(AuthLoginSignup.class);
    @Autowired
    private RqLoginSignup rq;
    @Autowired
    private EmailServer emailServer;
    @Autowired
    private SessionManager sessionManager;

    @PostMapping("/login")
    public ApiResponse<?> Login(@RequestBody Map<String, String> requests) {
        String username = requests.get("username");
        String password = requests.get("password");
        Optional<User> res = rq.login(username, password);
        if (res.isPresent()) {
            User user = res.get();
            if (user.isBanned()) {
                logger.warn("User {} account is banned", username);
                throw new com.auction.server.exception.AuthenticationException("Account has been banned");
            }
            String token = sessionManager.createSession(user);
            user.setSessionToken(token);
            logger.info("User {} logged in successfully", username);
            return new ApiResponse<User>(200, "Login successful", user);
        }
        else {
            logger.error("User {} login failed", username);
            throw new com.auction.server.exception.AuthenticationException("Incorrect username or password");
        }
    }

    @PostMapping("/signup")
    public ApiResponse<?> Signup(@RequestBody User newUser) {
        logger.info("Signup request received for username={}, email={}",
                newUser.getUsername(), newUser.getEmail());
        Optional<User> signupResult = rq.signup(newUser);
        if (signupResult.isPresent()) {
            User savedUser = signupResult.get();
            String body = EmailTemplateBuilder.buildWelcomeEmail(savedUser.getFullname(), savedUser.getUsername());
            emailServer.SendEmail(savedUser.getEmail(), "Account Registration Successful", body);
            logger.info("User {} registered successfully", savedUser.getUsername());
            return ApiResponse.success("Registration successful",
                new UserResponseDTO(savedUser.getId(), savedUser.getUsername(),
                    savedUser.getFullname(), savedUser.getEmail(),
                    savedUser.getAccountType(), savedUser.getBalance(),
                    savedUser.isBanned()));
        }
        else {
            logger.error("Registration error, username: {} or email: {} already exists", newUser.getUsername(), newUser.getEmail());
            return ApiResponse.error(400, "Email or Username already exists");
        }
    }
}
