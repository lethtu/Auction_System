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

    public List<AuctionSession> getActiveSessions() {
        return auctionSessionRepository.findByStatus(AuctionStatus.ACTIVE);
    }

    public AuctionSession getSessionById(Integer id) {
        return auctionSessionRepository.findById(id).orElse(null);
    }

    public List<AuctionSession> getSessionsBySeller(Integer sellerId) {
        return auctionSessionRepository.findBySeller_Id(sellerId);
    }

    @Transactional
    public boolean endSession(Integer sessionId) {
        Optional<AuctionSession> sessionOpt = auctionSessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            AuctionSession session = sessionOpt.get();
            session.setStatus(AuctionStatus.ENDED);
            auctionSessionRepository.save(session);
            logger.info("Đã kết thúc phiên đấu giá ID: {}", sessionId);
            return true;
        }
        return false;
    }

    @Transactional
    public BidResponse updateBid(Integer ItemAuctionId, Integer BidderId, BigDecimal newBidAmount) {
        Optional<AuctionSession> itemOptional = auctionSessionRepository.findByIdForUpdate(ItemAuctionId);

        if (itemOptional.isEmpty()) {
            logger.error("Không tìm thấy sản phẩm với ID: {}", ItemAuctionId);
            return new BidResponse(false, "LỖI: Không tìm thấy sản phẩm với ID: " + ItemAuctionId, new BigDecimal("0"), null);
        }

        AuctionSession item = itemOptional.get();
        BigDecimal currentPrice = item.getCurrentPrice();

        if (item.getStatus().equals(AuctionStatus.ACTIVE)){
            if (newBidAmount.compareTo(currentPrice) <= 0) {
                logger.error("Đặt giá thất bại từ UserId: {} với giá: {} và giá hiện tại: {}", BidderId, newBidAmount, currentPrice);
                return new BidResponse(false, "THẤT BẠI: Giá đặt " + newBidAmount + " phải cao hơn giá hiện tại (" + currentPrice + ")", currentPrice, null);
            }

            LocalDateTime time = LocalDateTime.now();
            item.setCurrentPrice(newBidAmount);

            // ==========================================
            // THUẬT TOÁN ANTI-SNIPING (CHỐNG BẮN TỈA)
            // ==========================================
            LocalDateTime currentEndTime = item.getEndTime();
            long secondsLeft = java.time.Duration.between(time, currentEndTime).getSeconds();
            String updatedEndTimeStr = null;

            // Nếu thời gian còn lại dưới 60 giây, tự động cộng thêm 60 giây
            if (secondsLeft < 60 && secondsLeft >= 0) {
                LocalDateTime newEndTime = currentEndTime.plusSeconds(60);
                item.setEndTime(newEndTime);
                updatedEndTimeStr = newEndTime.toString();
                logger.info("Anti-Sniping: Gia hạn phiên {} thêm 60s. Thời gian mới: {}", ItemAuctionId, newEndTime);
            }
            // ==========================================

            User bidder = userRepository.findById(BidderId).orElse(null);
            if (bidder != null) {
                Bid bid = new Bid(item, bidder, newBidAmount, time);
                item.addBid(bid);

                try {
                    bidRepository.save(bid);
                    auctionSessionRepository.save(item);
                    logger.info("Đã cập nhật giá mới cho AuctionItem {} thành {} bởi {}", ItemAuctionId, newBidAmount, BidderId);
                    // Trả về kèm theo thời gian mới (nếu có)
                    return new BidResponse(true, "THÀNH CÔNG: Bạn đang dẫn đầu phiên đấu giá!", newBidAmount, updatedEndTimeStr);
                }
                catch (Exception e) {
                    logger.error("Lỗi khi lưu Database: ", e);
                    return new BidResponse(false, "LỖI HỆ THỐNG: Không thể lưu dữ liệu. Vui lòng thử lại.", currentPrice, null);
                }
            }
            else {
                logger.error("Thất bại: Không tìm thấy User với ID = {}", BidderId);
                return new BidResponse(false, "Lỗi: Tài khoản người dùng không tồn tại!", currentPrice, null);
            }
        }
        else{
            logger.error("Lỗi: Phiên đấu giá: {} hiện đang có trạng thái: {}", ItemAuctionId, item.getStatus());
            return new BidResponse(false, "Lỗi: Phiên này chưa được phép đấu giá", currentPrice, null);
        }
    }
}