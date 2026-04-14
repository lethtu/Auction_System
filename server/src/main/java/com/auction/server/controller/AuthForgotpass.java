package com.auction.server.controller;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.Map;

import com.auction.server.repository.HandleLoginSignup;
import com.auction.server.view.EmailServer;
import com.auction.server.view.RqForgotPass;
import com.auction.server.model.User;
import com.auction.server.view.ApiResponse;
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
    private Map<String, String> otpStorage = new ConcurrentHashMap<>();
    @Autowired
    private RqForgotPass forgotPass;
    @Autowired
    private EmailServer emailServer;
    @Autowired
    private HandleLoginSignup Save;

    @PostMapping("/forgot_pass")
    public ApiResponse forgot_pass(@RequestBody Map<String, String> requests) {
        String email = requests.get("email");
        System.out.println("Yêu cầu quên mật khẩu từ: " + email);
        Optional<User> customer = forgotPass.forgot_pass(email);

        if (customer.isPresent()) {
            String newOTP = OTPGenerator.generateOTP(6);
            if (otpStorage.get(email) != null) {
                newOTP = otpStorage.get(email);
            } else {
                otpStorage.put(email, newOTP);
            }
            String body = "Xin chào " + customer.get().getFullname() + ",\n\n"
                    + "Đây là mã để bạn khôi phục lại mật khẩu: (" + newOTP + ").\n"
                    + "KHÔNG CUNG CẤP MÃ NÀY CHO BẤT CỨ AI\n\n"
                    + "Trân trọng,\nBan Quản Trị.";
            emailServer.SendEmail(email, "Mã xác thực để đổi mật khẩu", body);
            return new ApiResponse<String>(200, "Đã gửi mã xác nhận", "");
        } else {
            return new ApiResponse<String>(100, "Không có tài khoản nào liên kết với Email này", "");
        }
    }

    @PostMapping("/check_code")
    public ApiResponse check(@RequestBody Map<String, String> requests) {
        String email = requests.get("email");
        String code = requests.get("code");
        String pass = requests.get("password");
        Optional<User> customer = forgotPass.forgot_pass(email);
        if (customer.isPresent()) {
            String savedOTP = otpStorage.get(email);
            if (savedOTP != null && savedOTP.equals(code)) {
                otpStorage.remove(email);
                customer.get().setPassword(pass);
                Save.save(customer.get());
                return new ApiResponse<String>(200, "Xác thực thành công, đã thay đổi mật khẩu thành công", "");
            } else {
                return new ApiResponse<String>(100, "Code sai hoặc đã hết hạn, vui lòng kiểm tra lại", "");
            }
        } else {
            return new ApiResponse(100, "Không có tài khoản nào liên kết với Email này", "");
        }
    }
}