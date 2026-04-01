package com.auction.server.controller;

import com.auction.server.view.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuctionController {
    @GetMapping("/hello_world")
    public ApiResponse check(){
        ApiResponse item = new ApiResponse(200, "Test", "123");
        return item;
    }
}
