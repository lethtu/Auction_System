package com.auction.server.service;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.factory.ItemFactory;
import com.auction.server.mapper.SessionResponseMapper;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import com.auction.server.validator.SellerAuctionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SellerService {
    private static final Logger logger = LoggerFactory.getLogger(SellerService.class);

    private final ItemRepository itemRepository;
    private final AuctionSessionRepository auctionSessionRepository;
    private final UserRepository userRepository;

    public SellerService(
            ItemRepository itemRepository,
            AuctionSessionRepository auctionSessionRepository,
            UserRepository userRepository
    ) {
        this.itemRepository = itemRepository;
        this.auctionSessionRepository = auctionSessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SessionResponseDTO createAuctionSession(CreateAuctionRequest request) {
        SellerAuctionValidator.validate(request);

        Seller seller = getSellerById(request.getSellerId());

        Item item = ItemFactory.createItem(request.getType());
        updateItemFromRequest(item, request);
        Item savedItem = itemRepository.save(item);

        AuctionSession session = new AuctionSession();
        session.setItem(savedItem);
        session.setSeller(seller);
        updateSessionFromRequest(session, request);
        resetApprovalInfo(session);
        session.setStatus(AuctionStatus.PENDING);

        AuctionSession savedSession = auctionSessionRepository.save(session);
        return SessionResponseMapper.toDTO(savedSession);
    }

    public List<SessionResponseDTO> getMySessions(Integer sellerId, String status) {
        getSellerById(sellerId);

        return findSessionsBySellerAndStatus(sellerId, status)
                .stream()
                .map(SessionResponseMapper::toDTO)
                .toList();
    }

    public SessionResponseDTO getSessionDetail(Integer sessionId, Integer sellerId) {
        getSellerById(sellerId);

        AuctionSession session = getSessionById(sessionId);
        validateSessionOwner(session, sellerId, "Bạn không có quyền xem phiên này");

        return SessionResponseMapper.toDTO(session);
    }

    @Transactional
    public SessionResponseDTO updatePendingSession(Integer sessionId, Integer sellerId, CreateAuctionRequest request) {
        SellerAuctionValidator.validate(request);
        getSellerById(sellerId);

        AuctionSession session = getSessionById(sessionId);
        validateSessionOwner(session, sellerId, "Bạn không có quyền sửa phiên này");
        validatePendingSession(session);

        Item item = session.getItem();
        updateItemFromRequest(item, request);
        itemRepository.save(item);

        updateSessionFromRequest(session, request);

        AuctionSession savedSession = auctionSessionRepository.save(session);
        return SessionResponseMapper.toDTO(savedSession);
    }

    @Transactional
    public void cancelSession(Integer sessionId, Integer sellerId) {
        getSellerById(sellerId);

        AuctionSession session = getSessionById(sessionId);
        validateSessionOwner(session, sellerId, "Bạn không có quyền hủy phiên này");
        validatePendingSession(session);

        session.setStatus(AuctionStatus.CANCELED);
        auctionSessionRepository.save(session);
    }

    public SellerStatsDTO getSellerStats(Integer sellerId) {
        getSellerById(sellerId);

        List<AuctionSession> endedSessions = auctionSessionRepository.findBySeller_IdAndStatus(
                sellerId,
                AuctionStatus.ENDED
        );

        BigDecimal revenue = endedSessions.stream()
                .map(AuctionSession::getCurrentPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SellerStatsDTO(endedSessions.size(), revenue);
    }

    private List<AuctionSession> findSessionsBySellerAndStatus(Integer sellerId, String status) {
        if (status == null || status.trim().isEmpty()) {
            return auctionSessionRepository.findBySeller_Id(sellerId);
        }

        try {
            AuctionStatus enumStatus = AuctionStatus.valueOf(status.trim().toUpperCase());
            return auctionSessionRepository.findBySeller_IdAndStatus(sellerId, enumStatus);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    private Seller getSellerById(Integer sellerId) {
        User user = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        if (!(user instanceof Seller)) {
            logger.error("Người dùng này không phải seller");
            throw new RuntimeException("Người dùng này không phải seller");
        }

        return (Seller) user;
    }

    private AuctionSession getSessionById(Integer sessionId) {
        return auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại"));
    }

    private void validateSessionOwner(AuctionSession session, Integer sellerId, String errorMessage) {
        if (session.getSeller() == null || !session.getSeller().getId().equals(sellerId)) {
            logger.error("Seller {} không có quyền với phiên {}", sellerId, session.getId());
            throw new RuntimeException(errorMessage);
        }
    }

    private void validatePendingSession(AuctionSession session) {
        if (session.getStatus() != AuctionStatus.PENDING) {
            throw new RuntimeException("Chỉ được thao tác với phiên đang chờ duyệt");
        }
    }

    private void updateItemFromRequest(Item item, CreateAuctionRequest request) {
        item.setName(request.getName());
        item.setType(request.getType());
        item.setImagePath(request.getImagePath());
        item.setDescription(request.getDescription());
    }

    private void updateSessionFromRequest(AuctionSession session, CreateAuctionRequest request) {
        session.setStartingPrice(request.getStartingPrice());
        session.setCurrentPrice(request.getStartingPrice());
        session.setStepPrice(request.getStepPrice());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
    }

    private void resetApprovalInfo(AuctionSession session) {
        session.setApprovedAt(null);
        session.setRejectedAt(null);
        session.setRejectReason(null);
        session.setApprovedByAdminId(null);
        session.setRejectedByAdminId(null);
    }
}