package com.auction.server.controller;

import java.util.List;
import com.auction.server.model.user;
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
    private UserRepository list_user;
    @GetMapping("/get_user")
    public ApiResponse get_user(){
        List<user> cac_user = list_user.findByRole("user");
        return new ApiResponse<List<user>>(200, "Thành công", cac_user);

    }
}
