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
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.util.SellerSessionGuard;
import com.auction.server.util.SellerSessionUpdater;
import com.auction.server.validator.SellerAuctionValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SellerService {
    private final ItemRepository itemRepository;
    private final AuctionSessionRepository auctionSessionRepository;
    private final SellerSessionGuard sellerSessionGuard;

    public SellerService(
            ItemRepository itemRepository,
            AuctionSessionRepository auctionSessionRepository,
            SellerSessionGuard sellerSessionGuard
    ) {
        this.itemRepository = itemRepository;
        this.auctionSessionRepository = auctionSessionRepository;
        this.sellerSessionGuard = sellerSessionGuard;
    }

    @Transactional
    public SessionResponseDTO createAuctionSession(CreateAuctionRequest request) {
        SellerAuctionValidator.validate(request);

        Seller seller = sellerSessionGuard.getSellerById(request.getSellerId());

        Item item = ItemFactory.createItem(request.getType(), request);
        Item savedItem = itemRepository.save(item);

        AuctionSession session = new AuctionSession();
        session.setItem(savedItem);
        session.setSeller(seller);

        SellerSessionUpdater.updateSessionFromRequest(session, request);

        if (session.getStartTime() == null) {
            session.setStartTime(LocalDateTime.now());
        }

        session.setApplyMinRate(request.getApplyMinRate() != null ? request.getApplyMinRate() : false);
        session.setMinRate(request.getMinRate() != null ? request.getMinRate() : BigDecimal.ZERO);
        SellerSessionUpdater.resetApprovalInfo(session);

        session.setApprovedAt(LocalDateTime.now());
        session.setRejectedAt(null);
        session.setRejectReason(null);
        session.setApprovedByAdminId(null);
        session.setRejectedByAdminId(null);

        if ("DRAFT".equalsIgnoreCase(request.getStatus())) {
            session.setStatus(AuctionStatus.DRAFT);
        } else {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = session.getStartTime();
            if (startTime != null && startTime.isAfter(now)) {
                session.setStatus(AuctionStatus.COMING);
            } else {
                session.setStatus(AuctionStatus.ACTIVE);
            }
        }

        AuctionSession savedSession = auctionSessionRepository.save(session);
        return SessionResponseMapper.toDTO(savedSession);
    }

    public List<SessionResponseDTO> getMySessions(Integer sellerId, String status) {
        sellerSessionGuard.getSellerById(sellerId);

        return findSessionsBySellerAndStatus(sellerId, status)
                .stream()
                .map(SessionResponseMapper::toDTO)
                .toList();
    }

    public SessionResponseDTO getSessionDetail(Integer sessionId, Integer sellerId) {
        sellerSessionGuard.getSellerById(sellerId);

        AuctionSession session = sellerSessionGuard.getSessionById(sessionId);
        sellerSessionGuard.validateSessionOwner(session, sellerId, "You do not have permission to view this session");

        return SessionResponseMapper.toDTO(session);
    }

    @Transactional
    public SessionResponseDTO updateSession(Integer sessionId, Integer sellerId, CreateAuctionRequest request) {
        SellerAuctionValidator.validate(request);
        sellerSessionGuard.getSellerById(sellerId);

        AuctionSession session = sellerSessionGuard.getSessionById(sessionId);
        sellerSessionGuard.validateSessionOwner(session, sellerId, "You do not have permission to edit this session");

        if (session.getStatus() != AuctionStatus.ACTIVE && session.getStatus() != AuctionStatus.COMING && session.getStatus() != AuctionStatus.DRAFT) {
            throw new IllegalArgumentException("Can only edit sessions that are not ended or drafts");
        }

        Item item = session.getItem();
        SellerSessionUpdater.updateItemFromRequest(item, request);
        itemRepository.save(item);

        SellerSessionUpdater.updateSessionFromRequest(session, request);
        session.setApplyMinRate(request.getApplyMinRate() != null ? request.getApplyMinRate() : false);
        session.setMinRate(request.getMinRate() != null ? request.getMinRate() : BigDecimal.ZERO);

        if ("DRAFT".equalsIgnoreCase(request.getStatus())) {
            session.setStatus(AuctionStatus.DRAFT);
        } else {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = session.getStartTime();
            if (startTime != null && startTime.isAfter(now)) {
                session.setStatus(AuctionStatus.COMING);
            } else {
                session.setStatus(AuctionStatus.ACTIVE);
            }
        }

        AuctionSession savedSession = auctionSessionRepository.save(session);
        return SessionResponseMapper.toDTO(savedSession);
    }

    @Transactional
    public void cancelSession(Integer sessionId, Integer sellerId) {
        sellerSessionGuard.getSellerById(sellerId);

        AuctionSession session = sellerSessionGuard.getSessionById(sessionId);
        sellerSessionGuard.validateSessionOwner(session, sellerId, "You do not have permission to cancel this session");
        if (session.getStatus() != AuctionStatus.ACTIVE && session.getStatus() != AuctionStatus.COMING && session.getStatus() != AuctionStatus.DRAFT) {
            throw new IllegalArgumentException("Can only cancel active, upcoming, or draft sessions");
        }

        session.setStatus(AuctionStatus.CANCELED);
        auctionSessionRepository.save(session);
    }

    public SellerStatsDTO getSellerStats(Integer sellerId) {
        sellerSessionGuard.getSellerById(sellerId);

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
}