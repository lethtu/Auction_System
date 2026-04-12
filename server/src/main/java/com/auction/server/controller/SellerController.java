package com.auction.server.controller;

import com.auction.server.dto.AuctionRequestDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.Product;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createAuction(@RequestBody AuctionRequestDTO dto) {
        try {
            Product product = new Product();
            product.setName(dto.getProductName());
            product.setType(dto.getProductType());
            product.setImageUrl(dto.getImageUrl());
            product.setDescription(dto.getDescription());

            Product savedProduct = productRepository.save(product);

            AuctionSession session = new AuctionSession();
            session.setProduct(savedProduct);
            session.setStartingPrice(dto.getStartingPrice());
            session.setCurrentPrice(dto.getStartingPrice());
            session.setStepPrice(dto.getStepPrice());
            session.setStartTime(LocalDateTime.now());
            session.setEndTime(dto.getEndTime());
            session.setStatus("PENDING");

            auctionSessionRepository.save(session);

            return ResponseEntity.ok("Người bán đã gửi yêu cầu đấu giá thành công cho món: " + savedProduct.getName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/my-sessions/{sellerId}")
    public List<AuctionSession> viewMySessions(@PathVariable Integer sellerId) {
        return new ArrayList<>();
    }

    @DeleteMapping("/cancel-session/{sessionId}")
    public String cancelAuction(@PathVariable Integer sessionId) {
        return "Đã hủy phiên đấu giá có ID: " + sessionId;
    }
}