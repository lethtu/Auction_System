package com.auction.server.controller;

import com.auction.server.model.user;
import com.auction.server.model.bidder;
import com.auction.server.view.ApiResponse;
import com.auction.server.view.EmailServer;
import com.auction.server.view.RqLoginSignup;
import org.eclipse.angus.mail.imap.protocol.MailboxInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthLoginSignup {
    @Autowired
    private RqLoginSignup rq;
    @Autowired
    private EmailServer emailServer;
    @PostMapping("/login")
    public ApiResponse Login(@RequestBody Map<String, String> requests){
        String username = requests.get("username");
        String password = requests.get("password");
        Optional<user> res = rq.login(username, password);
        if (res.isPresent()){
            return new ApiResponse<Optional<user>>(200, "Đăng nhập thành công", res);
        }
        else{
            return new ApiResponse<String>(100, "Đăng nhập thất bại", "");
        }
    }
    @PostMapping("/signup")
    public ApiResponse Signup(@RequestBody user newUser){
        boolean check = rq.signup(newUser);
        System.out.println(newUser.in());
        System.out.println(newUser.getEmail() + " " + newUser.getFullname() + " " + newUser.getPassword());
        if (check == false){
            String body = "Xin chào " + newUser.getFullname() + ",\n\n"
                    + "Tài khoản của bạn (" + newUser.getUsername() + ") đã được tạo thành công.\n"
                    + "Chúc bạn có những phiên đấu giá tuyệt vời!\n\n"
                    + "Trân trọng,\nBan Quản Trị.";
            emailServer.SendEmail(newUser.getEmail(), "Đăng ký tài khoản thành công", body);
            return new ApiResponse(200, "Đăng ký thành công", newUser);
        }
        else{
            return new ApiResponse(100, "Email hoặc Username đã tồn tại", newUser);
        }
    }
}
