package com.auction.server.controller;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.repository.HandleLoginSignup;
import com.auction.server.view.EmailServer;
import com.auction.server.view.RqForgotPass;
import com.auction.server.model.User;
import com.auction.server.dto.ApiResponse;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class OTPGenerator {
    private static final String DATA = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static SecureRandom random = new SecureRandom();

    public static String generateOTP(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int index = random.nextInt(DATA.length());
            sb.append(DATA.charAt(index));
        }
        return sb.toString();
    }
}

@RestController
@RequestMapping("/api")
public class AuthForgotpass {
    private static final Logger logger = LoggerFactory.getLogger(AuthForgotpass.class);
    private Map<String, String> otpStorage = new ConcurrentHashMap<>();
    @Autowired
    private RqForgotPass forgotPass;
    @Autowired
    private EmailServer emailServer;
    @Autowired
    private HandleLoginSignup Save;

    @PostMapping("/forgot_pass")
    public ApiResponse<String> forgot_pass(@RequestBody Map<String, String> requests) {
        String email = requests.get("email");
        logger.info("Forgot password request received from {}", email);
        Optional<User> customer = forgotPass.forgot_pass(email);
        if (customer.isPresent()) {
            logger.info("Email {} exists in the system", email);
            logger.info("Generating OTP for {}", email);
            String newOTP = OTPGenerator.generateOTP(6);
            if (otpStorage.get(email) != null) {
                newOTP = otpStorage.get(email);
            } else {
                otpStorage.put(email, newOTP);
            }
            String body = "Hello " + customer.get().getFullname() + ",\n\n"
                    + "Here is your password recovery code: (" + newOTP + ").\n"
                    + "DO NOT SHARE THIS CODE WITH ANYONE\n\n"
                    + "Best regards,\nThe Admin Team.";
            emailServer.SendEmail(email, "Password Reset Verification Code", body);
            logger.info("Request from {} processed successfully", email);
            return new ApiResponse<String>(200, "Verification code sent", "");
        } else {
            logger.error("No account associated with {}", email);
            return new ApiResponse<String>(404, "No account is associated with this email", "");
        }
    }

    @PostMapping("/check_code")
    public ApiResponse<String> check(@RequestBody Map<String, String> requests) {
        String email = requests.get("email");
        String code = requests.get("code");
        String pass = requests.get("password");
        Optional<User> customer = forgotPass.forgot_pass(email);
//        Updated logic
        String saveOTP = otpStorage.getOrDefault(email, "");
        if (!saveOTP.isEmpty()) {
            if (saveOTP.equals(code)) {
                otpStorage.remove(email);
                if (customer.isPresent()) {
                    customer.get().setPassword(pass);
                    Save.save(customer.get());
                    logger.info("Verification and password change successful for: {}", email);
                    return new ApiResponse<String>(200, "Verification successful, password has been changed", "");
                }
                else{
                    logger.error("Customer not found");
                    return new ApiResponse<String>(404, "No account is associated with this email", "");
                }
            }
            else {
                logger.error("Code verification failed for: {}", email);
                return new ApiResponse<String>(400, "Invalid or expired code, please try again", "");
            }
        }
        else {
            logger.error("No account associated with: {}", email);
            return new ApiResponse<String>(404, "No account is associated with this email", "");
        }
    }
}