package com.auction.server.view;

import org.aspectj.apache.bcel.classfile.Module;
import org.springframework.stereotype.Service;
import com.auction.server.model.user;
import com.auction.server.repository.HandleForgotPass;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@Service
public class RqForgotPass {
    @Autowired
    private HandleForgotPass forgotPass;
    public Optional<user> forgot_pass(String email){
        return forgotPass.findByEmail(email);
    }
}
