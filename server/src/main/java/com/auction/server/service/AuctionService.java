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
        return auctionSessionRepository.findByStatus(AuctionStatus.ACTIVE)
                .stream()
                .filter(this::isProductVisible)
                .toList();
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

    private boolean isProductVisible(AuctionSession session) {
        return session != null && (session.getItem() == null || !session.getItem().isHidden());
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

        if (!isProductVisible(item)) {
            logger.error("Đặt giá thất bại: sản phẩm của phiên {} đang bị ẩn", ItemAuctionId);
            return new BidResponse(false, "Lỗi: Sản phẩm này đang bị ẩn", BigDecimal.ZERO, null, null);
        }

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
        else {
            // TASK 7: Ném Exception nếu phiên đã kết thúc hoặc bị hủy
            if (item.getStatus() == AuctionStatus.ENDED || item.getStatus() == AuctionStatus.CANCELED) {
                logger.warn("Chặn đặt giá: Phiên {} đã đóng.", ItemAuctionId);
                // Đảm bảo package import khớp với project của cậu
                throw new com.auction.server.exception.AuctionClosedException("Lỗi: Phiên đấu giá này đã kết thúc hoặc bị hủy!", ItemAuctionId);
            }

            // Nếu chỉ là PENDING (chưa mở) thì trả về false bình thường
            logger.error("Lỗi: Phiên đấu giá: {} hiện đang có trạng thái: {}", ItemAuctionId, item.getStatus());
            return new BidResponse(false, "Lỗi: Phiên này chưa được phép đấu giá", currentPrice, null, item.getHighestBidderId());
        }
    }
}