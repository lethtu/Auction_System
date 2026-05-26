package com.auction.server.service;

import com.auction.server.dto.BidResponse;
import com.auction.server.dto.DeliveryInfoRequest;
import com.auction.server.exception.AuctionClosedException;
import com.auction.server.model.AutoBidConfig;
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


    @Test
    @DisplayName("getSessionsBySeller delegates to repository")
    public void getSessionsBySeller_returnsRepositoryResult() {
        AuctionSession anotherSession = new AuctionSession();
        anotherSession.setId(2);
        when(auctionSessionRepository.findBySeller_Id(99)).thenReturn(List.of(mockSession, anotherSession));

        List<AuctionSession> result = auctionService.getSessionsBySeller(99);

        assertEquals(List.of(mockSession, anotherSession), result);
        verify(auctionSessionRepository).findBySeller_Id(99);
    }

    @Test
    @DisplayName("updateBid returns failure when session does not exist")
    public void updateBid_returnsFailureWhenSessionDoesNotExist() {
        when(auctionSessionRepository.findByIdForUpdate(404)).thenReturn(Optional.empty());

        BidResponse response = auctionService.updateBid(404, 99, new BigDecimal("15000.00"));

        assertFalse(response.isSuccess());
        assertEquals("Auction session not found.", response.getMessage());
        verifyNoInteractions(userRepository);
        verify(bidRepository, never()).save(any(Bid.class));
    }

    @Test
    @DisplayName("updateBid throws when session already ended")
    public void updateBid_throwsWhenSessionEnded() {
        mockSession.setStatus(AuctionStatus.ENDED);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        AuctionClosedException error = assertThrows(AuctionClosedException.class,
                () -> auctionService.updateBid(1, 99, new BigDecimal("15000.00")));

        assertTrue(error.getMessage().contains("ended"));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("updateBid throws when session canceled")
    public void updateBid_throwsWhenSessionCanceled() {
        mockSession.setStatus(AuctionStatus.CANCELED);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        AuctionClosedException error = assertThrows(AuctionClosedException.class,
                () -> auctionService.updateBid(1, 99, new BigDecimal("15000.00")));

        assertTrue(error.getMessage().contains("canceled"));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("updateBid rejects sessions that are not active")
    public void updateBid_rejectsSessionWithoutActiveStatus() {
        mockSession.setStatus(null);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("15000.00"));

        assertFalse(response.isSuccess());
        assertEquals("Auction is not open for bidding.", response.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("updateBid returns failure when bidder does not exist")
    public void updateBid_returnsFailureWhenBidderDoesNotExist() {
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("15000.00"));

        assertFalse(response.isSuccess());
        assertEquals("Bidder not found.", response.getMessage());
        verify(bidRepository, never()).save(any(Bid.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("updateBid returns failure when available balance is insufficient")
    public void updateBid_returnsFailureWhenAvailableBalanceInsufficient() {
        mockUser.setBalance(new BigDecimal("1000.00"));
        mockUser.setFrozenBalance(BigDecimal.ZERO);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("15000.00"));

        assertFalse(response.isSuccess());
        assertEquals("Insufficient available balance.", response.getMessage());
        verify(bidRepository, never()).save(any(Bid.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("updateBid only freezes the delta when bidder is already highest")
    public void updateBid_highestBidderOnlyFreezesDelta() {
        mockSession.setCurrentPrice(new BigDecimal("10000.00"));
        mockSession.setHighestBidderId(99);
        mockUser.setBalance(new BigDecimal("100000.00"));
        mockUser.setFrozenBalance(new BigDecimal("10000.00"));
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));
        when(bidRepository.countBySessionId(1)).thenReturn(5);

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("20000.00"));

        assertTrue(response.isSuccess());
        assertEquals(0, new BigDecimal("20000.00").compareTo(mockUser.getFrozenBalance()));
        assertEquals(99, mockSession.getHighestBidderId());
        verify(userRepository, never()).findById(77);
        verify(auctionSessionRepository).save(mockSession);
    }

    @Test
    @DisplayName("updateBid releases previous highest bidder balance")
    public void updateBid_releasesPreviousHighestBidderBalance() {
        mockSession.setCurrentPrice(new BigDecimal("10000.00"));
        mockSession.setHighestBidderId(77);
        mockUser.setBalance(new BigDecimal("100000.00"));
        mockUser.setFrozenBalance(BigDecimal.ZERO);
        User previousBidder = new User();
        previousBidder.setId(77);
        previousBidder.setBalance(new BigDecimal("100000.00"));
        previousBidder.setFrozenBalance(new BigDecimal("10000.00"));
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));
        when(userRepository.findById(77)).thenReturn(Optional.of(previousBidder));
        when(bidRepository.countBySessionId(1)).thenReturn(6);

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("20000.00"));

        assertTrue(response.isSuccess());
        assertEquals(0, BigDecimal.ZERO.compareTo(previousBidder.getFrozenBalance()));
        assertEquals(0, new BigDecimal("20000.00").compareTo(mockUser.getFrozenBalance()));
        verify(userRepository).save(previousBidder);
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("updateBid extends end time when bid is placed near the end")
    public void updateBid_extendsEndTimeNearEnd() {
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(30);
        mockSession.setEndTime(originalEndTime);
        mockUser.setBalance(new BigDecimal("100000.00"));
        mockUser.setFrozenBalance(BigDecimal.ZERO);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));
        when(bidRepository.countBySessionId(1)).thenReturn(1);

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("15000.00"));

        assertTrue(response.isSuccess());
        assertTrue(mockSession.getEndTime().isAfter(originalEndTime));
    }

    @Test
    @DisplayName("registerAutoBid rejects invalid input")
    public void registerAutoBid_rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> auctionService.registerAutoBid(null, 99, new BigDecimal("10000.00"), new BigDecimal("1000.00")));
        assertThrows(IllegalArgumentException.class,
                () -> auctionService.registerAutoBid(1, 99, BigDecimal.ZERO, new BigDecimal("1000.00")));
        assertThrows(IllegalArgumentException.class,
                () -> auctionService.registerAutoBid(1, 99, new BigDecimal("10000.00"), BigDecimal.ZERO));

        verifyNoInteractions(auctionSessionRepository);
        verify(autoBidConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerAutoBid rejects missing session")
    public void registerAutoBid_rejectsMissingSession() {
        when(auctionSessionRepository.findByIdForUpdate(404)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> auctionService.registerAutoBid(404, 99, new BigDecimal("15000.00"), new BigDecimal("1000.00")));

        assertEquals("Auction session not found.", error.getMessage());
        verify(autoBidConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerAutoBid rejects inactive session")
    public void registerAutoBid_rejectsInactiveSession() {
        mockSession.setStatus(null);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> auctionService.registerAutoBid(1, 99, new BigDecimal("15000.00"), new BigDecimal("1000.00")));

        assertEquals("Auto-bid is available only for active auctions.", error.getMessage());
        verify(autoBidConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerAutoBid rejects maximum bid that is not higher than current price")
    public void registerAutoBid_rejectsMaxBidNotHigherThanCurrentPrice() {
        mockSession.setCurrentPrice(new BigDecimal("15000.00"));
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> auctionService.registerAutoBid(1, 99, new BigDecimal("15000.00"), new BigDecimal("1000.00")));

        assertEquals("Maximum bid must be higher than the current price.", error.getMessage());
        verifyNoInteractions(userRepository);
        verify(autoBidConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerAutoBid rejects missing bidder")
    public void registerAutoBid_rejectsMissingBidder() {
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> auctionService.registerAutoBid(1, 99, new BigDecimal("15000.00"), new BigDecimal("1000.00")));

        assertEquals("Bidder not found.", error.getMessage());
        verify(autoBidConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerAutoBid updates an existing active config")
    public void registerAutoBid_updatesExistingConfig() {
        AutoBidConfig existing = new AutoBidConfig(1, 99, new BigDecimal("15000.00"), new BigDecimal("1000.00"));
        existing.setActive(false);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));
        when(autoBidConfigRepository.findBySessionIdAndBidderIdAndActiveTrue(1, 99)).thenReturn(Optional.of(existing));

        auctionService.registerAutoBid(1, 99, new BigDecimal("20000.00"), new BigDecimal("2000.00"));

        assertEquals(new BigDecimal("20000.00"), existing.getMaxBid());
        assertEquals(new BigDecimal("2000.00"), existing.getIncrement());
        verify(autoBidConfigRepository).save(existing);
    }

    @Test
    @DisplayName("resolveAutoBids returns null for missing or inactive session")
    public void resolveAutoBids_returnsNullForMissingOrInactiveSession() {
        when(auctionSessionRepository.findByIdForUpdate(404)).thenReturn(Optional.empty());
        assertNull(auctionService.resolveAutoBids(404));

        mockSession.setStatus(null);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        assertNull(auctionService.resolveAutoBids(1));

        verifyNoInteractions(autoBidConfigRepository);
    }

    @Test
    @DisplayName("resolveAutoBids returns null when no active configs exist")
    public void resolveAutoBids_returnsNullWhenNoConfigsExist() {
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(autoBidConfigRepository.findBySessionIdAndActiveTrueOrderByMaxBidDesc(1)).thenReturn(List.of());

        assertNull(auctionService.resolveAutoBids(1));
    }

    @Test
    @DisplayName("resolveAutoBids deactivates seller configs and returns null if only sellers remain")
    public void resolveAutoBids_deactivatesSellerConfigs() {
        Seller seller = new Seller();
        seller.setId(99);
        mockSession.setSeller(seller);
        AutoBidConfig sellerConfig = new AutoBidConfig(1, 99, new BigDecimal("30000.00"), new BigDecimal("1000.00"));
        sellerConfig.setActive(true);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(autoBidConfigRepository.findBySessionIdAndActiveTrueOrderByMaxBidDesc(1)).thenReturn(List.of(sellerConfig));

        assertNull(auctionService.resolveAutoBids(1));

        verify(autoBidConfigRepository).save(sellerConfig);
    }

    @Test
    @DisplayName("resolveAutoBids returns null when winner is already highest bidder")
    public void resolveAutoBids_returnsNullWhenWinnerAlreadyHighestBidder() {
        mockSession.setHighestBidderId(99);
        AutoBidConfig config = new AutoBidConfig(1, 99, new BigDecimal("30000.00"), new BigDecimal("1000.00"));
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(autoBidConfigRepository.findBySessionIdAndActiveTrueOrderByMaxBidDesc(1)).thenReturn(List.of(config));

        assertNull(auctionService.resolveAutoBids(1));

        verify(userRepository, never()).findById(anyInt());
    }

    @Test
    @DisplayName("resolveAutoBids deactivates config when next price exceeds max bid")
    public void resolveAutoBids_deactivatesWhenNextPriceExceedsMaxBid() {
        mockSession.setCurrentPrice(new BigDecimal("50000.00"));
        AutoBidConfig config = new AutoBidConfig(1, 99, new BigDecimal("55000.00"), new BigDecimal("1000.00"));
        config.setActive(true);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(autoBidConfigRepository.findBySessionIdAndActiveTrueOrderByMaxBidDesc(1)).thenReturn(List.of(config));

        assertNull(auctionService.resolveAutoBids(1));

        verify(autoBidConfigRepository).save(config);
    }

    @Test
    @DisplayName("endSession returns false when session does not exist")
    public void endSession_returnsFalseWhenSessionDoesNotExist() {
        when(auctionSessionRepository.findById(404)).thenReturn(Optional.empty());

        assertFalse(auctionService.endSession(404));

        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("endSession cancels when min rate is not met and releases frozen balance")
    public void endSession_cancelsWhenMinRateNotMetAndReleasesFrozenBalance() {
        mockSession.setApplyMinRate(true);
        mockSession.setMinRate(new BigDecimal("20000.00"));
        mockSession.setCurrentPrice(new BigDecimal("15000.00"));
        mockSession.setHighestBidderId(99);
        mockUser.setFrozenBalance(new BigDecimal("15000.00"));
        when(auctionSessionRepository.findById(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));

        boolean result = auctionService.endSession(1);

        assertTrue(result);
        assertEquals(AuctionStatus.CANCELED, mockSession.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(mockUser.getFrozenBalance()));
        verify(userRepository).save(mockUser);
        verify(auctionSessionRepository).save(mockSession);
    }

    @Test
    @DisplayName("saveDeliveryInfo rejects blank required fields")
    public void saveDeliveryInfo_rejectsBlankRequiredFields() {
        DeliveryInfoRequest request = new DeliveryInfoRequest();
        request.setRecipientName(" ");
        request.setPhoneNumber("0900000000");
        request.setAddress("Ha Noi");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> auctionService.saveDeliveryInfo(1, 99, request));

        assertEquals("Recipient, phone number and address are required.", error.getMessage());
        verifyNoInteractions(auctionSessionRepository);
    }

    @Test
    @DisplayName("saveDeliveryInfo rejects missing session")
    public void saveDeliveryInfo_rejectsMissingSession() {
        DeliveryInfoRequest request = deliveryRequest("A", "090", "Address", null);
        when(auctionSessionRepository.findById(404)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> auctionService.saveDeliveryInfo(404, 99, request));

        assertEquals("Auction session not found.", error.getMessage());
    }

    @Test
    @DisplayName("saveDeliveryInfo rejects non ended session")
    public void saveDeliveryInfo_rejectsNonEndedSession() {
        DeliveryInfoRequest request = deliveryRequest("A", "090", "Address", null);
        when(auctionSessionRepository.findById(1)).thenReturn(Optional.of(mockSession));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> auctionService.saveDeliveryInfo(1, 99, request));

        assertEquals("Delivery details can be submitted only after the auction has ended.", error.getMessage());
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("saveDeliveryInfo rejects non winner")
    public void saveDeliveryInfo_rejectsNonWinner() {
        mockSession.setStatus(AuctionStatus.ENDED);
        mockSession.setHighestBidderId(99);
        DeliveryInfoRequest request = deliveryRequest("A", "090", "Address", null);
        when(auctionSessionRepository.findById(1)).thenReturn(Optional.of(mockSession));
        when(bidRepository.findWinningBidsForSessions(List.of(1))).thenReturn(List.of());

        SecurityException error = assertThrows(SecurityException.class,
                () -> auctionService.saveDeliveryInfo(1, 77, request));

        assertEquals("Only the auction winner can submit delivery information.", error.getMessage());
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("saveDeliveryInfo trims and truncates delivery fields")
    public void saveDeliveryInfo_trimsAndTruncatesFields() {
        mockSession.setStatus(AuctionStatus.ENDED);
        mockSession.setHighestBidderId(99);
        String longName = "  " + "A".repeat(200) + "  ";
        String longPhone = "  " + "9".repeat(40) + "  ";
        String longAddress = "  " + "B".repeat(600) + "  ";
        String longNote = "  " + "C".repeat(600) + "  ";
        DeliveryInfoRequest request = deliveryRequest(longName, longPhone, longAddress, longNote);
        when(auctionSessionRepository.findById(1)).thenReturn(Optional.of(mockSession));
        when(bidRepository.findWinningBidsForSessions(List.of(1))).thenReturn(List.of());

        auctionService.saveDeliveryInfo(1, 99, request);

        assertEquals(150, mockSession.getDeliveryRecipient().length());
        assertEquals(30, mockSession.getDeliveryPhone().length());
        assertEquals(500, mockSession.getDeliveryAddress().length());
        assertEquals(500, mockSession.getDeliveryNote().length());
        assertNotNull(mockSession.getDeliverySubmittedAt());
        verify(auctionSessionRepository).save(mockSession);
    }

    private DeliveryInfoRequest deliveryRequest(String recipient, String phone, String address, String note) {
        DeliveryInfoRequest request = new DeliveryInfoRequest();
        request.setRecipientName(recipient);
        request.setPhoneNumber(phone);
        request.setAddress(address);
        request.setNote(note);
        return request;
    }

}
