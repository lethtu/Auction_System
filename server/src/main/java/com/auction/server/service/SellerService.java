package com.auction.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.factory.ItemFactory;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SellerService {
    private static final Logger logger = LoggerFactory.getLogger(SellerService.class);

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private AuctionSessionRepository auctionSessionRepository; // Đã đổi tên Repo theo chuẩn

    @Autowired
    private UserRepository userRepository;

    private Seller getSellerById(Integer sellerId) {
        User user = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        if (!(user instanceof Seller)) {
            logger.error("Người dùng này không phải seller");
            throw new RuntimeException("Người dùng này không phải seller");
        }

        return (Seller) user;
    }

    private void validateAuctionInput(CreateAuctionRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            logger.error("Tên sản phẩm không được để trống");
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }

        if (request.getType() == null || request.getType().trim().isEmpty()) {
            logger.error("Loại sản phẩm không được để trống");
            throw new IllegalArgumentException("Loại sản phẩm không được để trống");
        }

        if (request.getDescription() != null && request.getDescription().length() > 1000) {
            logger.error("Mô tả không được quá 1000 ký tự");
            throw new IllegalArgumentException("Mô tả không được quá 1000 ký tự");
        }

        if (request.getStartingPrice() == null || request.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Giá khởi điểm phải lớn hơn 0");
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");
        }

        if (request.getStepPrice() != null && request.getStepPrice().compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Bước giá phải lớn hơn 0");
            throw new IllegalArgumentException("Bước giá phải lớn hơn 0");
        }

        LocalDateTime now = LocalDateTime.now();

        // Cho phép dung sai 10 giây do độ trễ mạng (Network Latency) nếu Client có gửi startTime
        if (request.getStartTime() != null && request.getStartTime().isBefore(now.minusSeconds(10))) {
            logger.error("Thời gian bắt đầu không được nằm trong quá khứ.");
            throw new IllegalArgumentException("Thời gian bắt đầu không được nằm trong quá khứ.");
        }

        // Xác định thời gian bắt đầu thực tế để làm mốc so sánh với thời gian kết thúc
        LocalDateTime actualStart = (request.getStartTime() == null) ? now : request.getStartTime();

        if (request.getEndTime() == null || !request.getEndTime().isAfter(actualStart)) {
            logger.error("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu.");
            throw new IllegalArgumentException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu.");
        }
    }

    /**
     * Hàm tạo phiên đấu giá (Đã đồng bộ với SellerController của Khánh)
     */
    @Transactional
    public AuctionSession createAuctionSession(CreateAuctionRequest request) {
        validateAuctionInput(request);
        Seller seller = getSellerById(request.getSellerId());

        // Dùng ItemFactory của Khánh, map dữ liệu từ Request
        Item item = ItemFactory.createItem(request.getType());
        item.setName(request.getName());
        item.setType(request.getType());
        item.setImagePath(request.getImagePath()); // Dùng imagePath chuẩn
        item.setDescription(request.getDescription());

        Item savedItem = itemRepository.save(item);

        AuctionSession session = new AuctionSession();
        session.setItem(savedItem);
        session.setSeller(seller);
        session.setStartingPrice(request.getStartingPrice());
        session.setCurrentPrice(request.getStartingPrice());
        session.setStepPrice(request.getStepPrice());

        // Tối ưu Server-side Timing: Nếu Client để trống, Server tự gán giờ hiện hành
        if (request.getStartTime() == null) {
            session.setStartTime(LocalDateTime.now());
        } else {
            session.setStartTime(request.getStartTime());
        }

        session.setEndTime(request.getEndTime());
        session.setApplyMinRate(request.getApplyMinRate() != null ? request.getApplyMinRate() : false);
        session.setMinRate(request.getMinRate() != null ? request.getMinRate() : BigDecimal.ZERO);

        session.setApprovedAt(null);
        session.setRejectedAt(null);
        session.setRejectReason(null);
        session.setApprovedByAdminId(null);
        session.setRejectedByAdminId(null);

        session.setStatus(AuctionStatus.PENDING); // Ép dùng Enum PENDING

        return auctionSessionRepository.save(session);
    }

    // Các tính năng mới của Minh (Đã chuẩn hóa Enum)
    public List<SessionResponseDTO> getMySessions(Integer sellerId, String status) {
        getSellerById(sellerId);

        List<AuctionSession> sessions = auctionSessionRepository.findBySeller_Id(sellerId);

        if (status != null && !status.trim().isEmpty()) {
            try {
                // Chuyển String gửi lên thành Enum để lọc
                AuctionStatus enumStatus = AuctionStatus.valueOf(status.trim().toUpperCase());
                sessions = sessions.stream()
                        .filter(session -> session.getStatus() == enumStatus)
                        .toList();
            } catch (IllegalArgumentException e) {
                // Bỏ qua lọc nếu status gửi lên không khớp chuẩn Enum
            }
        }

        return sessions.stream()
                .map(this::mapToSessionResponseDTO)
                .toList();
    }

    public SessionResponseDTO getSessionDetail(Integer sessionId, Integer sellerId) {
        Seller seller = getSellerById(sellerId);

        AuctionSession session = auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại"));

        if (!session.getSeller().getId().equals(seller.getId())) {
            logger.error("{} không có quyền xem phiên {}", sellerId, sessionId);
            throw new RuntimeException("Bạn không có quyền xem phiên này");
        }

        return mapToSessionResponseDTO(session);
    }

    @Transactional
    public AuctionSession updatePendingSession(Integer sessionId, Integer sellerId, CreateAuctionRequest request) {
        validateAuctionInput(request);
        Seller seller = getSellerById(sellerId);

        AuctionSession session = auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại"));

        if (!session.getSeller().getId().equals(seller.getId())) {
            logger.error("{} không có quyền sửa phiên {}", sellerId, sessionId);
            throw new RuntimeException("Bạn không có quyền sửa phiên này");
        }

        if (session.getStatus() != AuctionStatus.PENDING) {
            logger.error("Chỉ được sửa phiên đang chờ duyệt");
            throw new RuntimeException("Chỉ được sửa phiên đang chờ duyệt");
        }

        Item item = session.getItem();
        item.setName(request.getName());
        item.setType(request.getType());
        item.setImagePath(request.getImagePath());
        item.setDescription(request.getDescription());
        itemRepository.save(item);

        session.setStartingPrice(request.getStartingPrice());
        session.setCurrentPrice(request.getStartingPrice());
        session.setStepPrice(request.getStepPrice());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setApplyMinRate(request.getApplyMinRate() != null ? request.getApplyMinRate() : false);
        session.setMinRate(request.getMinRate() != null ? request.getMinRate() : BigDecimal.ZERO);

        return auctionSessionRepository.save(session);
    }

    @Transactional
    public void cancelSession(Integer sessionId, Integer sellerId) {
        Seller seller = getSellerById(sellerId);

        AuctionSession session = auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại"));

        if (!session.getSeller().getId().equals(seller.getId())) {
            logger.error("{} không có quyền hủy phiên này", sellerId);
            throw new RuntimeException("Bạn không có quyền hủy phiên này");
        }

        if (session.getStatus() != AuctionStatus.PENDING) {
            logger.error("Chỉ được hủy phiên ở trạng thái chờ duyệt");
            throw new RuntimeException("Chỉ được hủy phiên ở trạng thái chờ duyệt");
        }

        session.setStatus(AuctionStatus.CANCELED); // Ép dùng Enum CANCELED
        auctionSessionRepository.save(session);
    }

    public SellerStatsDTO getSellerStats(Integer sellerId) {
        getSellerById(sellerId);

        List<AuctionSession> myCompletedSessions = auctionSessionRepository.findBySeller_Id(sellerId)
                .stream()
                .filter(s -> s.getStatus() == AuctionStatus.ENDED) // Thay "COMPLETED" bằng ENDED chuẩn
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

        if (session.getItem() != null) {
            dto.setProductId(session.getItem().getId());
            dto.setProductName(session.getItem().getName());
            dto.setProductType(session.getItem().getType());
            dto.setImageUrl(session.getItem().getImagePath()); // Fix thành getImagePath
            dto.setDescription(session.getItem().getDescription());
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

        if (session.getStatus() != null) {
            dto.setStatus(session.getStatus().name());
        }

        dto.setRejectReason(session.getRejectReason());

        dto.setApprovedByAdminId(session.getApprovedByAdminId());
        dto.setRejectedByAdminId(session.getRejectedByAdminId());
        dto.setApplyMinRate(session.getApplyMinRate());
        dto.setMinRate(session.getMinRate());

        return dto;
    }
}