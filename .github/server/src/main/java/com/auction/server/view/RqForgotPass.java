package com.auction.server.view;

import org.springframework.stereotype.Service;
import com.auction.server.model.User;
import com.auction.server.repository.HandleForgotPass;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@Service
public class RqForgotPass {
    @Autowired
    private HandleForgotPass forgotPass;
    public Optional<User> forgot_pass(String email){
        return forgotPass.findByEmail(email);
    }
}
