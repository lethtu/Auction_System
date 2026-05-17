package com.auction.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.dto.BidResponse;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Bid;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.BidRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuctionService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.auction.server.repository.AutoBidConfigRepository autoBidConfigRepository;

    public List<AuctionSession> getActiveSessions() {
        return auctionSessionRepository.findByStatus(AuctionStatus.ACTIVE);
    }

    public AuctionSession getSessionById(Integer id) {
        AuctionSession session = auctionSessionRepository.findById(id).orElse(null);
        if (session != null) {
            session.setTotalBids(bidRepository.countBySessionId(session.getId()));
        }
        return session;
    }

    public List<AuctionSession> getSessionsBySeller(Integer sellerId) {
        return auctionSessionRepository.findBySeller_Id(sellerId);
    }

    @Transactional
    public boolean endSession(Integer sessionId) {
        Optional<AuctionSession> sessionOpt = auctionSessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            AuctionSession session = sessionOpt.get();
            if (Boolean.TRUE.equals(session.getApplyMinRate()) && session.getMinRate() != null) {
                if (session.getCurrentPrice() != null && session.getCurrentPrice().compareTo(session.getMinRate()) >= 0) {
                    session.setStatus(AuctionStatus.ENDED);
                } else {
                    session.setStatus(AuctionStatus.CANCELED);
                    logger.info("Phiên ID {} bị hủy do giá cuối ({}) không đạt min rate ({})",
                            session.getId(), session.getCurrentPrice(), session.getMinRate());
                }
            } else {
                session.setStatus(AuctionStatus.ENDED);
            }
            auctionSessionRepository.save(session);
            logger.info("Đã kết thúc phiên đấu giá ID: {} với trạng thái: {}", sessionId, session.getStatus());
            return true;
        }
        return false;
    }

    private BigDecimal calculateMinimumNextBid(BigDecimal currentPrice) {
        if (currentPrice == null) return BigDecimal.ZERO;
        
        if (currentPrice.compareTo(new BigDecimal("100000")) < 0) {
            return currentPrice.add(new BigDecimal("10000")); // + 10k
        } else if (currentPrice.compareTo(new BigDecimal("500000")) < 0) {
            return currentPrice.add(new BigDecimal("20000")); // + 20k
        } else if (currentPrice.compareTo(new BigDecimal("1000000")) < 0) {
            return currentPrice.add(new BigDecimal("50000")); // + 50k
        } else if (currentPrice.compareTo(new BigDecimal("5000000")) < 0) {
            return currentPrice.add(new BigDecimal("100000")); // + 100k
        } else {
            return currentPrice.add(new BigDecimal("200000")); // + 200k
        }
    }

    @Transactional
    public BidResponse updateBid(Integer ItemAuctionId, Integer BidderId, BigDecimal newBidAmount) {
        Optional<AuctionSession> itemOptional = auctionSessionRepository.findByIdForUpdate(ItemAuctionId);

        if (itemOptional.isEmpty()) {
            logger.error("Không tìm thấy sản phẩm với ID: {}", ItemAuctionId);
            return new BidResponse(false, "LỖI: Không tìm thấy sản phẩm với ID: " + ItemAuctionId, new BigDecimal("0"), null, null);
        }

        AuctionSession item = itemOptional.get();
        BigDecimal currentPrice = item.getCurrentPrice() == null ? BigDecimal.ZERO : item.getCurrentPrice();
        
        BigDecimal minimumRequiredBid;
        if (item.getStepPrice() != null && item.getStepPrice().compareTo(BigDecimal.ZERO) > 0) {
            minimumRequiredBid = currentPrice.add(item.getStepPrice());
        } else {
            minimumRequiredBid = calculateMinimumNextBid(currentPrice);
        }

        if (item.getStatus().equals(AuctionStatus.ACTIVE)){
            if (newBidAmount.compareTo(minimumRequiredBid) < 0) {
                logger.error("Đặt giá thất bại từ UserId: {} với giá: {} nhưng hệ thống yêu cầu tối thiểu: {}", BidderId, newBidAmount, minimumRequiredBid);
                return new BidResponse(
                        false,
                        "THẤT BẠI: Mức giá hợp lệ tiếp theo phải từ " + minimumRequiredBid + " trở lên!",
                        currentPrice,
                        null,
                        item.getHighestBidderId()
                );
            }

            User bidder = userRepository.findById(BidderId).orElse(null);
            if (bidder == null) {
                logger.error("Thất bại: Không tìm thấy User với ID = {}", BidderId);
                return new BidResponse(false, "Lỗi: Tài khoản người dùng không tồn tại!", currentPrice, null, item.getHighestBidderId());
            }

            LocalDateTime time = LocalDateTime.now();
            item.setCurrentPrice(newBidAmount);
            item.setHighestBidderId(BidderId);

            // ==========================================
            // THUẬT TOÁN ANTI-SNIPING (CHỐNG BẮN TỈA) - ĐÃ BỌC GIÁP
            // ==========================================
            LocalDateTime currentEndTime = item.getEndTime();
            String updatedEndTimeStr = null;

            // KIỂM TRA NULL: Nếu endTime bị null (do dữ liệu test ẩu), bỏ qua logic gia hạn
            if (currentEndTime != null) {
                try {
                    long secondsLeft = java.time.Duration.between(time, currentEndTime).getSeconds();

                    // Nếu thời gian còn lại dưới 60 giây, tự động cộng thêm 60 giây
                    if (secondsLeft < 60 && secondsLeft >= 0) {
                        LocalDateTime newEndTime = currentEndTime.plusSeconds(60);
                        item.setEndTime(newEndTime);
                        updatedEndTimeStr = newEndTime.toString();
                        logger.info("Anti-Sniping: Gia hạn phiên {} thêm 60s. Thời gian mới: {}", ItemAuctionId, newEndTime);
                    }
                } catch (Exception e) {
                    // Đề phòng các lỗi tính toán thời gian hiếm gặp khác để không làm sập luồng Đặt giá
                    logger.warn("Cảnh báo: Lỗi tính toán Anti-sniping cho phiên {}: {}", ItemAuctionId, e.getMessage());
                }
            }
            // ==========================================

            item.setWinner(bidder); // QUAN TRỌNG: Ghi nhận người dẫn đầu/chiến thắng
            Bid bid = new Bid(item, bidder, newBidAmount, time);
            item.addBid(bid);

            try {
                bidRepository.save(bid);
                auctionSessionRepository.save(item);
                logger.info("Đã cập nhật giá mới cho AuctionItem {} thành {} bởi {}", ItemAuctionId, newBidAmount, BidderId);
                int bidCount = Math.toIntExact(bidRepository.countBySessionId(ItemAuctionId));
                // Trả về kèm theo thời gian mới (nếu có) và số lượt bid thật trong DB
                return new BidResponse(
                        true,
                        "THÀNH CÔNG: Bạn đang dẫn đầu phiên đấu giá!",
                        newBidAmount,
                        updatedEndTimeStr,
                        BidderId,
                        bidCount
                );
            }
            catch (Exception e) {
                logger.error("Lỗi khi lưu Database: ", e);
                return new BidResponse(false, "LỖI HỆ THỐNG: Không thể lưu dữ liệu. Vui lòng thử lại.", currentPrice, null, item.getHighestBidderId());
            }
        }
        else{
            logger.error("Lỗi: Phiên đấu giá: {} hiện đang có trạng thái: {}", ItemAuctionId, item.getStatus());
            return new BidResponse(false, "Lỗi: Phiên này chưa được phép đấu giá", currentPrice, null, item.getHighestBidderId());
        }
    }

    // ==========================================
    // AUTO-BID ENGINE O(1)
    // ==========================================

    /**
     * Đăng ký hoặc cập nhật cấu hình Auto-bid vào Database (upsert).
     */
    @Transactional
    public void registerAutoBid(Integer sessionId, Integer bidderId, BigDecimal maxBid, BigDecimal increment) {
        Optional<com.auction.server.model.AutoBidConfig> existing =
                autoBidConfigRepository.findBySessionIdAndBidderIdAndActiveTrue(sessionId, bidderId);

        com.auction.server.model.AutoBidConfig config;
        if (existing.isPresent()) {
            config = existing.get();
            config.setMaxBid(maxBid);
            config.setIncrement(increment);
            logger.info("AUTO-BID updated: sessionId={}, bidderId={}, maxBid={}, increment={}",
                    sessionId, bidderId, maxBid, increment);
        } else {
            config = new com.auction.server.model.AutoBidConfig(sessionId, bidderId, maxBid, increment);
            logger.info("AUTO-BID registered: sessionId={}, bidderId={}, maxBid={}, increment={}",
                    sessionId, bidderId, maxBid, increment);
        }

        autoBidConfigRepository.save(config);
    }

    /**
     * Thuật toán O(1): Tính giá chốt hạ dựa trên Top 2 auto-bidder.
     * Công thức: newPrice = min(challenger.maxBid + winner.increment, winner.maxBid)
     *
     * @param sessionId phiên đấu giá
     * @return BidResponse nếu auto-bid thành công, null nếu không cần hành động
     */
    @Transactional
    public BidResponse resolveAutoBids(Integer sessionId) {
        // 1. Lấy session với pessimistic lock
        Optional<AuctionSession> sessionOpt = auctionSessionRepository.findByIdForUpdate(sessionId);
        if (sessionOpt.isEmpty()) {
            return null;
        }

        AuctionSession session = sessionOpt.get();
        if (!session.getStatus().equals(AuctionStatus.ACTIVE)) {
            return null;
        }

        BigDecimal currentPrice = session.getCurrentPrice() == null ? BigDecimal.ZERO : session.getCurrentPrice();

        // 2. Query Top 2 auto-bid configs (đã sort maxBid DESC)
        List<com.auction.server.model.AutoBidConfig> configs =
                autoBidConfigRepository.findBySessionIdAndActiveTrueOrderByMaxBidDesc(sessionId);

        if (configs.isEmpty()) {
            return null; // Không có auto-bid nào
        }

        com.auction.server.model.AutoBidConfig winner = configs.get(0); // maxBid cao nhất

        // Nếu winner đang là người giữ giá → không cần auto-bid
        if (winner.getBidderId().equals(session.getHighestBidderId())) {
            // Winner đã dẫn đầu, kiểm tra xem có challenger nào cần phản đòn không
            if (configs.size() < 2) {
                return null; // Chỉ 1 auto-bidder và đã dẫn đầu
            }
            // Swap: challenger trở thành người cần phản đòn
            com.auction.server.model.AutoBidConfig challenger = configs.get(1);
            // Challenger cần bid currentPrice + challenger.increment
            BigDecimal challengerBid = currentPrice.add(challenger.getIncrement());
            if (challengerBid.compareTo(challenger.getMaxBid()) > 0) {
                // Challenger hết maxBid → vô hiệu hóa
                challenger.setActive(false);
                autoBidConfigRepository.save(challenger);
                logger.info("AUTO-BID expired: bidderId={} hết maxBid={}", challenger.getBidderId(), challenger.getMaxBid());
                return null;
            }
            // Challenger đặt giá, rồi winner sẽ phản đòn ở lần resolve tiếp
            // Nhưng ta tính O(1) luôn:
            BigDecimal newPrice = challengerBid.min(winner.getMaxBid());
            // Winner cần phản đòn nếu challenger bid thành công
            if (newPrice.compareTo(currentPrice) > 0) {
                BigDecimal finalPrice = newPrice.add(winner.getIncrement()).min(winner.getMaxBid());
                if (finalPrice.compareTo(newPrice) > 0) {
                    // Winner phản đòn thành công
                    return executeAutoBid(session, winner, configs, finalPrice, currentPrice);
                } else {
                    // Winner hết room → challenger thắng tại newPrice
                    return executeAutoBid(session, challenger, configs, newPrice, currentPrice);
                }
            }
            return null;
        }

        // 3. Xác định Challenger
        BigDecimal challengerMax;
        if (configs.size() >= 2) {
            challengerMax = configs.get(1).getMaxBid(); // Người có maxBid thứ 2
        } else {
            challengerMax = currentPrice; // Không có đối thủ → giá hiện tại
        }

        // 4. CÔNG THỨC O(1): Tính giá chốt hạ
        BigDecimal newPrice = challengerMax.add(winner.getIncrement()).min(winner.getMaxBid());

        // 5. Đảm bảo newPrice > currentPrice
        if (newPrice.compareTo(currentPrice) <= 0) {
            return null;
        }

        // Đảm bảo newPrice >= currentPrice + stepPrice (đáp ứng bước giá tối thiểu)
        BigDecimal stepPrice = session.getStepPrice() == null ? BigDecimal.ONE : session.getStepPrice();
        BigDecimal minimumBid = currentPrice.add(stepPrice);
        if (newPrice.compareTo(minimumBid) < 0) {
            newPrice = minimumBid.min(winner.getMaxBid());
            if (newPrice.compareTo(minimumBid) < 0) {
                return null; // Winner không đủ maxBid để đặt bước giá tối thiểu
            }
        }

        return executeAutoBid(session, winner, configs, newPrice, currentPrice);
    }

    /**
     * Thực hiện auto-bid: cập nhật session, ghi bid vào DB, vô hiệu hóa config hết hạn.
     */
    private BidResponse executeAutoBid(
            AuctionSession session,
            com.auction.server.model.AutoBidConfig winner,
            List<com.auction.server.model.AutoBidConfig> allConfigs,
            BigDecimal newPrice,
            BigDecimal previousPrice
    ) {
        // Kiểm tra user tồn tại
        User bidder = userRepository.findById(winner.getBidderId()).orElse(null);
        if (bidder == null) {
            logger.error("AUTO-BID: Không tìm thấy User ID={}", winner.getBidderId());
            return null;
        }

        LocalDateTime time = LocalDateTime.now();
        session.setCurrentPrice(newPrice);
        session.setHighestBidderId(winner.getBidderId());

        // Anti-sniping
        String updatedEndTimeStr = null;
        LocalDateTime currentEndTime = session.getEndTime();
        if (currentEndTime != null) {
            try {
                long secondsLeft = java.time.Duration.between(time, currentEndTime).getSeconds();
                if (secondsLeft < 60 && secondsLeft >= 0) {
                    LocalDateTime newEndTime = currentEndTime.plusSeconds(60);
                    session.setEndTime(newEndTime);
                    updatedEndTimeStr = newEndTime.toString();
                    logger.info("AUTO-BID Anti-Sniping: Gia hạn phiên {} thêm 60s", session.getId());
                }
            } catch (Exception e) {
                logger.warn("AUTO-BID: Lỗi tính Anti-sniping: {}", e.getMessage());
            }
        }

        // Ghi Bid vào DB
        Bid bid = new Bid(session, bidder, newPrice, time);
        session.addBid(bid);

        try {
            bidRepository.save(bid);
            auctionSessionRepository.save(session);

            int bidCount = Math.toIntExact(bidRepository.countBySessionId(session.getId()));

            // Vô hiệu hóa các config đã hết maxBid
            for (com.auction.server.model.AutoBidConfig cfg : allConfigs) {
                if (newPrice.compareTo(cfg.getMaxBid()) >= 0 && !cfg.getId().equals(winner.getId())) {
                    cfg.setActive(false);
                    autoBidConfigRepository.save(cfg);
                    logger.info("AUTO-BID deactivated: bidderId={} hết maxBid={}",
                            cfg.getBidderId(), cfg.getMaxBid());
                }
            }

            logger.info("AUTO-BID O(1) thành công: sessionId={}, winner={}, newPrice={}",
                    session.getId(), winner.getBidderId(), newPrice);

            return new BidResponse(
                    true,
                    "Auto-bid: Giá đã được đẩy lên tự động!",
                    newPrice,
                    updatedEndTimeStr,
                    winner.getBidderId(),
                    bidCount
            );
        } catch (Exception e) {
            logger.error("AUTO-BID: Lỗi khi lưu DB", e);
            return null;
        }
    }
}