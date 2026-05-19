package com.auction.server.service;

import com.auction.server.dto.BidResponse;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Bid;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionServiceTest {

    @Mock
    private AuctionSessionRepository auctionSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private AuctionService auctionService;

    // Dữ liệu giả (Mock Data)
    private AuctionSession mockSession;
    private User mockUser;

    @BeforeEach
    public void setUp() {
        mockSession = new AuctionSession();
        mockSession.setId(1);
        mockSession.setCurrentPrice(new BigDecimal("1000.00"));
        mockSession.setStepPrice(new BigDecimal("100.00"));
        mockSession.setStatus(AuctionStatus.ACTIVE);

        // Chuẩn bị một Bidder giả
        mockUser = new User();
        mockUser.setId(99);
    }
    // Test 1: ĐẶT GIÁ HỢP LỆ
    @Test
    @DisplayName("Đặt giá hợp lệ: Giá mới lớn hơn giá hiện tại")
    public void testBid_HopLe() {
        // 1. Giả lập Database trả về dữ liệu khi được gọi
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));

        // 2. Chạy hành động đặt giá (Giá mới = 1500 > 1000)
        BigDecimal validBidPrice = new BigDecimal("1500.00");
        BidResponse response = auctionService.updateBid(1, 99, validBidPrice);

        System.out.println("LÝ DO THẤT BẠI: " + response.getMessage());
        // 3. Kiểm chứng
        assertTrue(response.isSuccess(), "Response phải trả về true");
        assertEquals(validBidPrice, mockSession.getCurrentPrice(), "Giá của session phải được cập nhật");

        // 4. Kiểm tra xem Database có được gọi lệnh 'save' không?
        verify(bidRepository, times(1)).save(any(Bid.class)); // Bảng bids phải được lưu 1 lần
        verify(auctionSessionRepository, times(1)).save(mockSession); // Bảng session phải được cập nhật
    }

    // Test 2: ĐẶT GIÁ KHÔNG HỢP LỆ (Giá quá thấp)
    @Test
    @DisplayName("Đặt giá không hợp lệ: Giá mới nhỏ hơn hoặc bằng giá hiện tại")
    public void testBid_KhongHopLe_GiaThap() {
        // 1. Giả lập Database
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        // Không cần mock User vì code sẽ bị chặn trước khi gọi đến User

        // 2. Đặt giá (Giá mới = 900 < 1000)
        BigDecimal invalidBidPrice = new BigDecimal("900.00");
        BidResponse response = auctionService.updateBid(1, 99, invalidBidPrice);

        // 3. Kiểm chứng
        assertFalse(response.isSuccess(), "Response phải trả về false");
        assertEquals(new BigDecimal("1000.00"), mockSession.getCurrentPrice(), "Giá của session không được phép thay đổi");

        // 4. Đảm bảo Database KHÔNG BỊ GHI rác
        verify(bidRepository, never()).save(any(Bid.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    // Test 3: KẾT THÚC PHIÊN ĐẤU GIÁ

    @Test
    @DisplayName("Kết thúc phiên: Cập nhật trạng thái thành CLOSED")
    public void testKetThucPhien_HopLe() {
        // 1. Giả lập Database
        when(auctionSessionRepository.findById(1)).thenReturn(Optional.of(mockSession));

        // 2. Chạy hành động
        boolean isSuccess = auctionService.endSession(1);

        // 3. Kiểm chứng
        assertTrue(isSuccess);
        assertEquals(AuctionStatus.ENDED, mockSession.getStatus(), "Trạng thái phải chuyển thành CLOSED");

        // 4. Đảm bảo đã lưu xuống DB
        verify(auctionSessionRepository, times(1)).save(mockSession);
    }
}