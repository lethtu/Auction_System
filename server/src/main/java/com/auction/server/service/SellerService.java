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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        validateAuctionInput(request);

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

        List<AuctionSession> sessions = auctionSessionRepository.findBySeller_Id(sellerId);

        if (status != null && !status.trim().isEmpty()) {
            try {
                AuctionStatus enumStatus = AuctionStatus.valueOf(status.trim().toUpperCase());
                sessions = sessions.stream()
                        .filter(session -> session.getStatus() == enumStatus)
                        .toList();
            } catch (IllegalArgumentException e) {
                return List.of();
            }
        }

        return sessions.stream()
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
        validateAuctionInput(request);
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

        List<AuctionSession> endedSessions = auctionSessionRepository.findBySeller_Id(sellerId)
                .stream()
                .filter(session -> session.getStatus() == AuctionStatus.ENDED)
                .toList();

        long count = endedSessions.size();

        BigDecimal revenue = endedSessions.stream()
                .map(AuctionSession::getCurrentPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SellerStatsDTO(count, revenue);
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
            logger.error("Chỉ được thao tác với phiên đang chờ duyệt");
            throw new RuntimeException("Chỉ được thao tác với phiên đang chờ duyệt");
        }
    }

    private void validateAuctionInput(CreateAuctionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu phiên đấu giá không hợp lệ");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }

        if (request.getType() == null || request.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Loại sản phẩm không được để trống");
        }

        if (request.getDescription() != null && request.getDescription().length() > 1000) {
            throw new IllegalArgumentException("Mô tả không được quá 1000 ký tự");
        }

        if (request.getStartingPrice() == null || request.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");
        }

        if (request.getStepPrice() == null || request.getStepPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bước giá phải lớn hơn 0");
        }

        LocalDateTime now = LocalDateTime.now();

        if (request.getStartTime() != null && request.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("Thời gian bắt đầu không được nằm trong quá khứ.");
        }

        if (request.getEndTime() == null || !request.getEndTime().isAfter(now)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải ở tương lai");
        }

        if (request.getStartTime() != null && request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu.");
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