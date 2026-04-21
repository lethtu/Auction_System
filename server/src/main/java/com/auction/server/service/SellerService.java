package com.auction.server.service;

import com.auction.server.dto.AuctionRequestDTO;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.Product;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ProductRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SellerService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuctionSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    private Seller getSellerById(Integer sellerId) {
        User user = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        if (!(user instanceof Seller)) {
            throw new RuntimeException("Người dùng này không phải seller");
        }

        return (Seller) user;
    }

    private void validateAuctionInput(AuctionRequestDTO dto) {
        if (dto.getProductName() == null || dto.getProductName().trim().isEmpty()) {
            throw new RuntimeException("Tên sản phẩm không được để trống");
        }

        if (dto.getProductType() == null || dto.getProductType().trim().isEmpty()) {
            throw new RuntimeException("Loại sản phẩm không được để trống");
        }

        if (dto.getDescription() != null && dto.getDescription().length() > 1000) {
            throw new RuntimeException("Mô tả không được quá 1000 ký tự");
        }

        if (dto.getStartingPrice() == null || dto.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá khởi điểm phải lớn hơn 0");
        }

        if (dto.getStepPrice() == null || dto.getStepPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Bước giá phải lớn hơn 0");
        }

        if (dto.getEndTime() == null || !dto.getEndTime().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Thời gian kết thúc phải ở tương lai");
        }
    }

    @Transactional
    public AuctionSession createAuction(AuctionRequestDTO dto) {
        validateAuctionInput(dto);
        Seller seller = getSellerById(dto.getSellerId());

        Product product = new Product();
        product.setName(dto.getProductName());
        product.setType(dto.getProductType());
        product.setImageUrl(dto.getImageUrl());
        product.setDescription(dto.getDescription());

        Product savedProduct = productRepository.save(product);

        AuctionSession session = new AuctionSession();
        session.setProduct(savedProduct);
        session.setSeller(seller);
        session.setStartingPrice(dto.getStartingPrice());
        session.setCurrentPrice(dto.getStartingPrice());
        session.setStepPrice(dto.getStepPrice());
        session.setStartTime(null);
        session.setEndTime(dto.getEndTime());
        session.setApprovedAt(null);
        session.setRejectedAt(null);
        session.setRejectReason(null);
        session.setApprovedByAdminId(null);
        session.setRejectedByAdminId(null);
        session.setStatus("PENDING");

        return sessionRepository.save(session);
    }

    public List<SessionResponseDTO> getMySessions(Integer sellerId, String status) {
        getSellerById(sellerId);

        List<AuctionSession> sessions = sessionRepository.findBySeller_Id(sellerId);

        if (status != null && !status.trim().isEmpty()) {
            String normalizedStatus = status.trim();

            sessions = sessions.stream()
                    .filter(session -> session.getStatus() != null
                            && session.getStatus().equalsIgnoreCase(normalizedStatus))
                    .toList();
        }

        return sessions.stream()
                .map(this::mapToSessionResponseDTO)
                .toList();
    }

    public SessionResponseDTO getSessionDetail(Integer sessionId, Integer sellerId) {
        Seller seller = getSellerById(sellerId);

        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại"));

        if (!session.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Bạn không có quyền xem phiên này");
        }

        return mapToSessionResponseDTO(session);
    }

    @Transactional
    public AuctionSession updatePendingSession(Integer sessionId, Integer sellerId, AuctionRequestDTO dto) {
        validateAuctionInput(dto);
        Seller seller = getSellerById(sellerId);

        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại"));

        if (!session.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Bạn không có quyền sửa phiên này");
        }

        if (!"PENDING".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Chỉ được sửa phiên đang chờ duyệt");
        }

        Product product = session.getProduct();
        product.setName(dto.getProductName());
        product.setType(dto.getProductType());
        product.setImageUrl(dto.getImageUrl());
        product.setDescription(dto.getDescription());
        productRepository.save(product);

        session.setStartingPrice(dto.getStartingPrice());
        session.setCurrentPrice(dto.getStartingPrice());
        session.setStepPrice(dto.getStepPrice());
        session.setEndTime(dto.getEndTime());

        return sessionRepository.save(session);
    }

    @Transactional
    public void cancelSession(Integer sessionId, Integer sellerId) {
        Seller seller = getSellerById(sellerId);

        AuctionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại"));

        if (!session.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy phiên này");
        }

        if (!"PENDING".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Chỉ được hủy phiên ở trạng thái chờ duyệt");
        }

        session.setStatus("CANCELED");
        sessionRepository.save(session);
    }

    public SellerStatsDTO getSellerStats(Integer sellerId) {
        getSellerById(sellerId);

        List<AuctionSession> myCompletedSessions = sessionRepository.findBySeller_Id(sellerId)
                .stream()
                .filter(s -> "COMPLETED".equalsIgnoreCase(s.getStatus()))
                .toList();

        long count = myCompletedSessions.size();

        BigDecimal revenue = myCompletedSessions.stream()
                .map(AuctionSession::getCurrentPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SellerStatsDTO(count, revenue);
    }

    private SessionResponseDTO mapToSessionResponseDTO(AuctionSession session) {
        SessionResponseDTO dto = new SessionResponseDTO();

        dto.setId(session.getId());

        if (session.getProduct() != null) {
            dto.setProductId(session.getProduct().getId());
            dto.setProductName(session.getProduct().getName());
            dto.setProductType(session.getProduct().getType());
            dto.setImageUrl(session.getProduct().getImageUrl());
            dto.setDescription(session.getProduct().getDescription());
        }

        if (session.getSeller() != null) {
            dto.setSellerId(session.getSeller().getId());
            dto.setSellerUsername(session.getSeller().getUsername());
            dto.setSellerFullname(session.getSeller().getFullname());
        }

        dto.setStartingPrice(session.getStartingPrice());
        dto.setCurrentPrice(session.getCurrentPrice());
        dto.setStepPrice(session.getStepPrice());

        dto.setCreatedAt(session.getCreatedAt());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setApprovedAt(session.getApprovedAt());
        dto.setRejectedAt(session.getRejectedAt());

        dto.setStatus(session.getStatus());
        dto.setRejectReason(session.getRejectReason());

        dto.setApprovedByAdminId(session.getApprovedByAdminId());
        dto.setRejectedByAdminId(session.getRejectedByAdminId());

        return dto;
    }
}