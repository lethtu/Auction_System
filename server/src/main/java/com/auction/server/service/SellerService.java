package com.auction.server.service;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.factory.ItemFactory;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository; // Đảm bảo em đã tạo Repo này
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SellerService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    /**
     * Hàm xử lý tạo phiên đấu giá.
     * Sử dụng @Transactional để đảm bảo tính toàn vẹn dữ liệu giữa Product và AuctionSession.
     */
    @Transactional
    public AuctionSession createAuctionSession(CreateAuctionRequest request) {
        // --- BẮT ĐẦU ĐOẠN VALIDATION CẦN THÊM VÀO ---
        LocalDateTime now = LocalDateTime.now();

        if (request.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("Thời gian bắt đầu không được nằm trong quá khứ.");
        }
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu.");
        }
        if (request.getStartingPrice() < 0) {
            throw new IllegalArgumentException("Giá khởi điểm không được là số âm.");
        }
        // --- KẾT THÚC ĐOẠN VALIDATION ---
        // 1. Khởi tạo và lưu Product
        // 1. Khởi tạo Item bằng Factory Pattern
        // Xưởng sẽ tự động đẻ ra Art, Electronics hoặc Vehicle dựa vào type người dùng gửi lên
        Item item = ItemFactory.createItem(request.getType());
        item.setName(request.getName());
        item.setImagePath(request.getImagePath());

        // Save item xuống DB để lấy ID
        Item savedItem = itemRepository.save(item);

        // 2. Khởi tạo và lưu AuctionSession
        AuctionSession session = new AuctionSession();
        session.setItem(savedItem); // Map Item vừa lưu vào Session

        // Khởi tạo các giá trị ban đầu cho phiên đấu giá
        session.setCurrentPrice(request.getStartingPrice()); // Giá hiện tại bằng giá khởi điểm
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setStatus(AuctionStatus.PENDING); // Trạng thái mặc định là chờ diễn ra

        // Save session và trả về kết quả
        return auctionSessionRepository.save(session);
    }
}