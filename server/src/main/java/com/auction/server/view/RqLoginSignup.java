package com.auction.server.view;

import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RqLoginSignup {
    @Autowired
    private HandleLoginSignup LoginSignup;
    public Optional<User> login(String username, String pass){
        return LoginSignup.findByUsernameAndPassword(username, pass);
    }
    public boolean signup(User newUser){
        if (!LoginSignup.existsByUsernameOrEmail(newUser.getUsername(), newUser.getEmail())){
            System.out.println("Đang lưu user với pass: " + newUser.getPassword());

            if (newUser.getPassword() == null) {
                throw new RuntimeException("Lỗi: Password gửi lên bị null!");
            }
            newUser.setRole("bidder");
            LoginSignup.save(newUser);
            return false;
        }
        return true;
    }
}
