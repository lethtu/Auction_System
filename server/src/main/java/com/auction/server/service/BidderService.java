package com.auction.server.service;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.mapper.SessionResponseMapper;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.Bid;
import com.auction.server.model.User;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BidderService {
    private static final Logger logger = LoggerFactory.getLogger(BidderService.class);

    private static final String BIDDER_ROLE = "bidder";
    private static final String SELLER_ROLE = "seller";

    private static final String USER_NOT_FOUND_MESSAGE = "User does not exist";
    private static final String INVALID_ROLE_MESSAGE = "Account is not a BIDDER or is already a SELLER";
    private static final String UPDATE_FAILED_MESSAGE = "Upgrade failed";
    private static final String UPDATE_SUCCESS_MESSAGE = "Account upgraded successfully";

    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final SessionResponseMapper sessionResponseMapper;

    public BidderService(
            UserRepository userRepository,
            BidRepository bidRepository,
            SessionResponseMapper sessionResponseMapper
    ) {
        this.userRepository = userRepository;
        this.bidRepository = bidRepository;
        this.sessionResponseMapper = sessionResponseMapper;
    }

    @Transactional
    public Map<String, Object> upToSeller(Integer userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = userRepository.findById(userId).orElse(null);

            if (user == null) {
                logger.error("Error: User ID {} not found", userId);
                return buildResponse(false, USER_NOT_FOUND_MESSAGE);
            }

            String currentRole = normalizeRole(user.getAccountType());

            if (!BIDDER_ROLE.equals(currentRole)) {
                logger.warn("User {} cannot be upgraded because current role is {}", userId, currentRole);
                return buildResponse(false, INVALID_ROLE_MESSAGE);
            }

            int updated = userRepository.updateRoleById(userId, SELLER_ROLE);

            if (updated == 0) {
                logger.error("Could not update role for User {}", userId);
                return buildResponse(false, UPDATE_FAILED_MESSAGE);
            }

            logger.info("User {} upgraded to seller successfully", userId);
            return buildResponse(true, UPDATE_SUCCESS_MESSAGE);

        } catch (Exception e) {
            logger.error("System error while upgrading User {}", userId, e);
            response.put("success", false);
            response.put("message", "System error: " + e.getMessage());
            return response;
        }
    }

    public List<SessionResponseDTO> getMyBids(Integer bidderId) {
        List<AuctionSession> sessions = bidRepository.findSessionsByBidderId(bidderId);
        if (sessions.isEmpty()) {
            return List.of();
        }

        List<Integer> sessionIds = sessions.stream()
                .map(AuctionSession::getId)
                .toList();

        Map<Integer, Object[]> statsBySessionId = loadStatsBySessionId(sessionIds, bidderId);
        Map<Integer, Bid> winningBidsBySessionId = loadWinningBidsBySessionId(sessionIds);

        return sessions.stream()
                .map(session -> toMyBidSessionDTO(session, bidderId, statsBySessionId, winningBidsBySessionId))
                .toList();
    }

    private Map<Integer, Object[]> loadStatsBySessionId(List<Integer> sessionIds, Integer bidderId) {
        Map<Integer, Object[]> statsBySessionId = new HashMap<>();
        for (Object[] row : bidRepository.findSessionStatsForBidder(sessionIds, bidderId)) {
            statsBySessionId.put((Integer) row[0], row);
        }
        return statsBySessionId;
    }

    private Map<Integer, Bid> loadWinningBidsBySessionId(List<Integer> sessionIds) {
        Map<Integer, Bid> winningBidsBySessionId = new HashMap<>();
        for (Bid bid : bidRepository.findWinningBidsForSessions(sessionIds)) {
            if (bid.getSession() != null) {
                winningBidsBySessionId.putIfAbsent(bid.getSession().getId(), bid);
            }
        }
        return winningBidsBySessionId;
    }

    private SessionResponseDTO toMyBidSessionDTO(
            AuctionSession session,
            Integer bidderId,
            Map<Integer, Object[]> statsBySessionId,
            Map<Integer, Bid> winningBidsBySessionId) {
        Object[] stats = statsBySessionId.get(session.getId());
        int bidCount = stats == null ? 0 : ((Number) stats[1]).intValue();

        SessionResponseDTO dto = sessionResponseMapper.mapToDTO(session, bidCount);
        dto.setUserMaxBid(stats == null ? null : (java.math.BigDecimal) stats[2]);

        Integer winnerId = applyWinningBidSnapshot(dto, session, winningBidsBySessionId.get(session.getId()));
        if (bidderId.equals(winnerId)) {
            copyWinnerDeliveryInfo(dto, session);
        }
        return dto;
    }

    private Integer applyWinningBidSnapshot(SessionResponseDTO dto, AuctionSession session, Bid winningBid) {
        Integer winnerId = session.getHighestBidderId();
        if (winningBid != null && winningBid.getBidder() != null) {
            winnerId = winningBid.getBidder().getId();
            dto.setCurrentPrice(winningBid.getAmount());
            dto.setHighestBidderId(winnerId);
        }
        return winnerId;
    }

    private void copyWinnerDeliveryInfo(SessionResponseDTO dto, AuctionSession session) {
        dto.setDeliveryRecipient(session.getDeliveryRecipient());
        dto.setDeliveryPhone(session.getDeliveryPhone());
        dto.setDeliveryAddress(session.getDeliveryAddress());
        dto.setDeliveryNote(session.getDeliveryNote());
        dto.setDeliverySubmittedAt(session.getDeliverySubmittedAt());
    }

    private Map<String, Object> buildResponse(boolean success, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        return response;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        return role.trim().toLowerCase(Locale.ROOT);
    }
}
