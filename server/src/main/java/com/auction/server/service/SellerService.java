package com.auction.server.service;

<<<<<<< HEAD
=======
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
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
<<<<<<< HEAD
=======
    private static final Logger logger = LoggerFactory.getLogger(SellerService.class);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)

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
<<<<<<< HEAD
=======
            logger.error("Người dùng này không phải seller");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            throw new RuntimeException("Người dùng này không phải seller");
        }

        return (Seller) user;
    }

    // Gộp Validation của cả Khánh và Minh
    private void validateAuctionInput(CreateAuctionRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
<<<<<<< HEAD
=======
            logger.error("Tên sản phẩm không được để trống");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }

        if (request.getType() == null || request.getType().trim().isEmpty()) {
<<<<<<< HEAD
=======
            logger.error("Loại sản phẩm không được để trống");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            throw new IllegalArgumentException("Loại sản phẩm không được để trống");
        }

        if (request.getDescription() != null && request.getDescription().length() > 1000) {
<<<<<<< HEAD
=======
            logger.error("Mô tả không được quá 1000 ký tự");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            throw new IllegalArgumentException("Mô tả không được quá 1000 ký tự");
        }

        if (request.getStartingPrice() == null || request.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
<<<<<<< HEAD
=======
            logger.error("Giá khởi điểm phải lớn hơn 0");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");
        }

        if (request.getStepPrice() != null && request.getStepPrice().compareTo(BigDecimal.ZERO) <= 0) {
<<<<<<< HEAD
=======
            logger.error("Bước giá phải lớn hơn 0");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            throw new IllegalArgumentException("Bước giá phải lớn hơn 0");
        }

        LocalDateTime now = LocalDateTime.now();
<<<<<<< HEAD
        if (request.getStartTime() != null && request.getStartTime().isBefore(now)) {
=======
        if (request.getStartTime() != null && request.getStartTime().isBefore(now)){
            logger.error("Thời gian bắt đầu không được nằm trong quá khứ.");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            throw new IllegalArgumentException("Thời gian bắt đầu không được nằm trong quá khứ.");
        }

        if (request.getEndTime() == null || !request.getEndTime().isAfter(now)) {
<<<<<<< HEAD
=======
            logger.error("Thời gian kết thúc phải ở tương lai");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            throw new IllegalArgumentException("Thời gian kết thúc phải ở tương lai");
        }

        if (request.getStartTime() != null && request.getEndTime().isBefore(request.getStartTime())) {
<<<<<<< HEAD
=======
            logger.error("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu.");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
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
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());

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
<<<<<<< HEAD
=======
            logger.error("{} không có quyền xem phiên {}", sellerId, sessionId);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
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
<<<<<<< HEAD
=======
            logger.error("{} không có quyền sửa phiên {}", sellerId, sessionId);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            throw new RuntimeException("Bạn không có quyền sửa phiên này");
        }

        if (session.getStatus() != AuctionStatus.PENDING) {
<<<<<<< HEAD
=======
            logger.error("Chỉ được sửa phiên đang chờ duyệt");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
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

        return auctionSessionRepository.save(session);
    }

    @Transactional
    public void cancelSession(Integer sessionId, Integer sellerId) {
        Seller seller = getSellerById(sellerId);

        AuctionSession session = auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại"));

        if (!session.getSeller().getId().equals(seller.getId())) {
<<<<<<< HEAD
=======
            logger.error("{} không có quyền hủy phiên này", sellerId);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            throw new RuntimeException("Bạn không có quyền hủy phiên này");
        }

        if (session.getStatus() != AuctionStatus.PENDING) {
<<<<<<< HEAD
=======
            logger.error("Chỉ được hủy phiên ở trạng thái chờ duyệt");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
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

        return dto;
    }
}