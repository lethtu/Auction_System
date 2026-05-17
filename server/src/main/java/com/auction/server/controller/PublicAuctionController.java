package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.mapper.SessionResponseMapper;
import com.auction.server.model.AuctionSession;
import com.auction.server.repository.AuctionSessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/auctions")
public class PublicAuctionController {

    private final AuctionSessionRepository auctionSessionRepository;

    public PublicAuctionController(AuctionSessionRepository auctionSessionRepository) {
        this.auctionSessionRepository = Objects.requireNonNull(auctionSessionRepository, "auctionSessionRepository must not be null");
    }

    // Endpoint public cho phép mọi người xem danh sách phiên đấu giá trên màn hình chính.
    // Trả DTO thay vì entity để tránh lỗi serialize quan hệ JPA và để client đọc ổn định hơn.
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SessionResponseDTO>>> getAllSessions() {
        List<SessionResponseDTO> sessions = auctionSessionRepository.findAll()
                .stream()
                .map(SessionResponseMapper::toDTO)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Lấy toàn bộ phiên đấu giá thành công", sessions));
    }
}
