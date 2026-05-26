package com.auction.server.service;

import com.auction.server.dto.BidResponse;
import com.auction.server.dto.DeliveryInfoRequest;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Bid;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.AutoBidConfigRepository;
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
import java.time.LocalDateTime;
import java.util.List;
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

    @Mock
    private AutoBidConfigRepository autoBidConfigRepository;

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
        mockSession.setStatus(AuctionStatus.ACTIVE);

        // Chuẩn bị một Bidder giả
        mockUser = new User();
        mockUser.setId(99);
        mockUser.setBalance(new BigDecimal("9999999.00")); // Anti joy-bidding requires sufficient balance
    }
    // Test 1: ĐẶT GIÁ HỢP LỆ
    @Test
    @DisplayName("Đặt giá hợp lệ: Giá mới lớn hơn giá hiện tại")
    public void testBid_HopLe() {
        // 1. Giả lập Database trả về dữ liệu khi được gọi
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));

        // 2. Chạy hành động đặt giá (Giá mới = 15000 >= 11000)
        BigDecimal validBidPrice = new BigDecimal("15000.00");
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

    @Test
    @DisplayName("Dynamic minimum increment overrides a lower configured step as price increases")
    public void dynamicMinimumIncrementRejectsLowBidAtHigherPrice() {
        mockSession.setCurrentPrice(new BigDecimal("500000.00"));
        mockSession.setStepPrice(new BigDecimal("10000.00"));
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("520000.00"));

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("550000.00"));
        assertEquals(new BigDecimal("500000.00"), mockSession.getCurrentPrice());
        verify(bidRepository, never()).save(any(Bid.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("Seller cannot bid on an auction they created")
    public void sellerCannotBidOnOwnAuction() {
        Seller seller = new Seller();
        seller.setId(99);
        mockSession.setSeller(seller);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("15000.00"));

        assertFalse(response.isSuccess());
        assertEquals("Sellers cannot bid on their own auction.", response.getMessage());
        verify(userRepository, never()).findById(anyInt());
        verify(bidRepository, never()).save(any(Bid.class));
    }

    @Test
    @DisplayName("Seller cannot configure auto-bid on an auction they created")
    public void sellerCannotConfigureAutoBidOnOwnAuction() {
        Seller seller = new Seller();
        seller.setId(99);
        mockSession.setSeller(seller);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> auctionService.registerAutoBid(1, 99, new BigDecimal("15000.00"), new BigDecimal("1000.00")));

        assertEquals("Sellers cannot enable auto-bid on their own auction.", error.getMessage());
        verify(autoBidConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("Recorded winning bidder can submit delivery details when legacy session snapshot is stale")
    public void recordedWinnerCanSubmitDelivery_WhenSessionSnapshotIsStale() {
        mockSession.setStatus(AuctionStatus.ENDED);
        mockSession.setHighestBidderId(7);
        User recordedWinner = new User();
        recordedWinner.setId(99);
        Bid winningBid = new Bid(mockSession, recordedWinner, new BigDecimal("222222.00"), LocalDateTime.now());
        when(auctionSessionRepository.findById(1)).thenReturn(Optional.of(mockSession));
        when(bidRepository.findWinningBidsForSessions(List.of(1))).thenReturn(List.of(winningBid));

        DeliveryInfoRequest request = new DeliveryInfoRequest();
        request.setRecipientName("Nguyen Van A");
        request.setPhoneNumber("0900000000");
        request.setAddress("Ha Noi");

        auctionService.saveDeliveryInfo(1, 99, request);

        assertEquals("Nguyen Van A", mockSession.getDeliveryRecipient());
        assertEquals("0900000000", mockSession.getDeliveryPhone());
        assertEquals("Ha Noi", mockSession.getDeliveryAddress());
        verify(auctionSessionRepository).save(mockSession);
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

    @Test
    @DisplayName("Kết thúc phiên: Cập nhật trạng thái thành ENDED và lưu winner chính thức")
    public void testKetThucPhien_SetsWinnerAndPersists() {
        // 1. Giả lập Database
        mockSession.setStatus(AuctionStatus.ACTIVE);
        mockSession.setHighestBidderId(null);
        mockSession.setWinner(null);

        User winningBidder = new User();
        winningBidder.setId(99);
        winningBidder.setBalance(new BigDecimal("1000000.00"));
        winningBidder.setFrozenBalance(new BigDecimal("50000.00"));

        Bid winningBid = new Bid(mockSession, winningBidder, new BigDecimal("50000.00"), LocalDateTime.now());

        when(auctionSessionRepository.findById(1)).thenReturn(Optional.of(mockSession));
        when(bidRepository.findWinningBidsForSessions(List.of(1))).thenReturn(List.of(winningBid));
        when(userRepository.findById(99)).thenReturn(Optional.of(winningBidder));

        // 2. Chạy hành động
        boolean isSuccess = auctionService.endSession(1);

        // 3. Kiểm chứng
        assertTrue(isSuccess);
        assertEquals(AuctionStatus.ENDED, mockSession.getStatus(), "Trạng thái phải chuyển thành ENDED");
        assertEquals(new BigDecimal("50000.00"), mockSession.getCurrentPrice(), "Giá phiên phải là giá bid thắng");
        assertEquals(99, mockSession.getHighestBidderId(), "ID bidder cao nhất phải là 99");
        assertSame(winningBidder, mockSession.getWinner(), "Winner của session phải được set là winningBidder");

        // 4. Kiểm tra balances
        assertEquals(0, new BigDecimal("950000.00").compareTo(winningBidder.getBalance()), "Winner balance phải trừ 50000");
        assertEquals(0, BigDecimal.ZERO.compareTo(winningBidder.getFrozenBalance()), "Winner frozenBalance phải giải tỏa 50000");

        // 5. Đảm bảo đã lưu xuống DB
        verify(auctionSessionRepository, times(1)).save(mockSession);
    }

    @Test
    @DisplayName("getActiveSessions only returns sessions with visible products")
    public void getActiveSessions_filtersHiddenAndMissingItems() {
        AuctionSession visibleSession = mock(AuctionSession.class, RETURNS_DEEP_STUBS);
        AuctionSession hiddenSession = mock(AuctionSession.class, RETURNS_DEEP_STUBS);
        AuctionSession missingItemSession = new AuctionSession();

        when(visibleSession.getItem().isHidden()).thenReturn(false);
        when(hiddenSession.getItem().isHidden()).thenReturn(true);
        when(auctionSessionRepository.findByStatus(AuctionStatus.ACTIVE))
                .thenReturn(List.of(visibleSession, hiddenSession, missingItemSession));

        List<AuctionSession> result = auctionService.getActiveSessions();

        assertEquals(List.of(visibleSession), result);
        verify(auctionSessionRepository).findByStatus(AuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("getSessionById returns session with total bid count")
    public void getSessionById_returnsSessionWithTotalBidCount() {
        when(auctionSessionRepository.findById(1)).thenReturn(Optional.of(mockSession));
        when(bidRepository.countBySessionId(1)).thenReturn(3);

        AuctionSession result = auctionService.getSessionById(1);

        assertSame(mockSession, result);
        assertEquals(Integer.valueOf(3), result.getTotalBids());
        verify(bidRepository).countBySessionId(1);
    }

    @Test
    @DisplayName("getSessionById returns null when session does not exist")
    public void getSessionById_returnsNullWhenSessionDoesNotExist() {
        when(auctionSessionRepository.findById(404)).thenReturn(Optional.empty());

        AuctionSession result = auctionService.getSessionById(404);

        assertNull(result);
        verify(bidRepository, never()).countBySessionId(anyInt());
    }

    @Test
    @DisplayName("getBidHistory maps bid history and tolerates missing bidder")
    public void getBidHistory_mapsBidsAndToleratesMissingBidder() {
        User bidder = new User();
        bidder.setId(99);
        LocalDateTime firstBidTime = LocalDateTime.of(2026, 5, 26, 10, 0);
        Bid firstBid = new Bid(mockSession, bidder, new BigDecimal("12000.00"), firstBidTime);
        Bid anonymousBid = new Bid(mockSession, null, new BigDecimal("13000.00"), firstBidTime.plusMinutes(1));
        when(bidRepository.findBySessionIdOrderByTimeAsc(1)).thenReturn(List.of(firstBid, anonymousBid));

        var history = auctionService.getBidHistory(1);

        assertEquals(2, history.size());
        assertNotNull(history.get(0));
        assertNotNull(history.get(1));
        verify(bidRepository).findBySessionIdOrderByTimeAsc(1);
    }

}
