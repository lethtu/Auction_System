package com.auction.server.controller;

import java.util.List;

import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.view.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.auction.server.model.AuctionSession;
@RestController
@RequestMapping("/api")

public class GetItem {
    @Autowired
    private AuctionSessionRepository items;

    @GetMapping("/get_item")
    public ApiResponse<List<AuctionSession>> get_item(){
        List<AuctionSession> hangDangBan = items.findByStatus("ACTIVE");
        ApiResponse<List<AuctionSession>> response = new ApiResponse<>(200, "Thành công", hangDangBan);
        return response;
    }
}
