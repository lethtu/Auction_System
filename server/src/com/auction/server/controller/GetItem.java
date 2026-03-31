package com.auction.server.controller;

import java.util.List;
import com.auction.server.model.item;
import com.auction.server.repository.ItemRepository;
import com.auction.server.view.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")

public class GetItem {
    @Autowired
    private ItemRepository items;

    @GetMapping("/get_item")
    public ApiResponse get_item(){
        List<item> hangDangBan = items.findByStatus("ACTIVE");
        ApiResponse<List<item>> response = new ApiResponse(200, "Thành công", hangDangBan);
        return response;
    }
}
