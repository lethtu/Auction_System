package com.auction.server.service;

import com.auction.server.dto.BidResponse;
import com.auction.server.exception.AuctionClosedException;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Bid;
import com.auction.server.model.Electronics;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Bài Test tổng hợp cho vòng đời (Lifecycle) của phiên đấu giá.
 * Bao phủ: Task 1,2 (Lifecycle), Task 5 (Anti-Sniping), Task 7 (Exception).
 */
@ExtendWith(MockitoExtension.class)
public class AuctionLifecycleTest {

    @Mock
    private AuctionSessionRepository auctionSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private AuctionService auctionService;

    private AuctionSession mockSession;
    private User mockUser;

    @BeforeEach
    public void setUp() {
        mockSession = new AuctionSession();
        mockSession.setId(1);
        mockSession.setCurrentPrice(new BigDecimal("1000.00"));
        mockSession.setStepPrice(new BigDecimal("100.00"));
        mockSession.setStatus(AuctionStatus.ACTIVE);
        mockSession.setStartTime(LocalDateTime.now().minusMinutes(10));
        mockSession.setEndTime(LocalDateTime.now().plusMinutes(10));

        Electronics mockItem = new Electronics();
        mockItem.setId(1);
        mockItem.setName("Mock Electronics Item");
        mockItem.setType("electronics");
        mockItem.setDescription("Mock item for auction lifecycle test");
        mockSession.setItem(mockItem);

        mockUser = new User();
        mockUser.setId(99);
        mockUser.setBalance(new BigDecimal("999999999.00"));
    }

    // ========================================================================
    // A. CORE FLOW TESTS
    // ========================================================================


    @Test
    @DisplayName("Core: Anti-Sniping gia hạn 60s khi bid trong 30s cuối")
    public void test_AntiSniping_ExtendEndTime_WhenBidInLast60Seconds() {
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(30);
        mockSession.setEndTime(originalEndTime);

        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("1500.00"));

        assertTrue(response.isSuccess(), "Đặt giá phải thành công");
        assertNotNull(response.getNewEndTime(), "newEndTime phải có giá trị vì đã gia hạn");

        LocalDateTime expectedNewEndTime = originalEndTime.plusSeconds(60);
        assertEquals(expectedNewEndTime, mockSession.getEndTime(),
                "endTime phải được cộng thêm đúng 60 giây");
    }

    @Test
    @DisplayName("Core: Ném AuctionClosedException khi bid vào phiên ENDED")
    public void test_Exception_BidOnEndedSession_ThrowsAuctionClosedException() {
        mockSession.setStatus(AuctionStatus.ENDED);

        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        AuctionClosedException exception = assertThrows(
                AuctionClosedException.class,
                () -> auctionService.updateBid(1, 99, new BigDecimal("2000.00")),
                "Phải ném AuctionClosedException khi bid vào phiên ENDED"
        );

        assertEquals(1, exception.getSessionId(), "SessionId trong exception phải đúng");
        assertTrue(exception.getMessage().contains("ended") || exception.getMessage().contains("closed"),
                "Message phải chứa thông báo ended");

        verify(bidRepository, never()).save(any(Bid.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    // ========================================================================
    // B. EDGE CASES & BOUNDARY TESTS
    // ========================================================================

    @Test
    @DisplayName("Edge: Bid bằng đúng giá hiện tại (1000 == 1000) phải thất bại")
    public void test_Edge_BidExactlyEqualCurrentPrice_ShouldFail() {
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("1000.00"));

        assertFalse(response.isSuccess(), "Bid bằng giá hiện tại phải thất bại");
        assertEquals(new BigDecimal("1000.00"), mockSession.getCurrentPrice(),
                "Giá session không được thay đổi");

        verify(bidRepository, never()).save(any(Bid.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("Edge: Anti-Sniping KHÔNG trigger khi còn 61 giây")
    public void test_Edge_AntiSniping_NotTrigger_WhenTimeLeft61Seconds() {
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(61);
        mockSession.setEndTime(originalEndTime);

        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("1500.00"));

        assertTrue(response.isSuccess(), "Đặt giá phải thành công");
        assertNull(response.getNewEndTime(), "newEndTime phải null vì không gia hạn");
        assertEquals(originalEndTime, mockSession.getEndTime(),
                "endTime phải giữ nguyên, không được thay đổi");
    }

    @Test
    @DisplayName("Edge: Anti-Sniping trigger khi còn 59 giây")
    public void test_Edge_AntiSniping_Trigger_WhenTimeLeftExactly59Seconds() {
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(59);
        mockSession.setEndTime(originalEndTime);

        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("1500.00"));

        assertTrue(response.isSuccess(), "Đặt giá phải thành công");
        assertNotNull(response.getNewEndTime(), "newEndTime phải có giá trị vì đã gia hạn");

        LocalDateTime expectedNewEndTime = originalEndTime.plusSeconds(60);
        assertEquals(expectedNewEndTime, mockSession.getEndTime(),
                "endTime phải được cộng thêm đúng 60 giây");
    }

    @Test
    @DisplayName("Edge: Ném AuctionClosedException khi bid vào phiên CANCELED")
    public void test_Edge_BidOnCanceledSession_ThrowsAuctionClosedException() {
        mockSession.setStatus(AuctionStatus.CANCELED);

        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        AuctionClosedException exception = assertThrows(
                AuctionClosedException.class,
                () -> auctionService.updateBid(1, 99, new BigDecimal("2000.00")),
                "Phải ném AuctionClosedException khi bid vào phiên CANCELED"
        );

        assertEquals(1, exception.getSessionId());
        assertTrue(exception.getMessage().contains("canceled") || exception.getMessage().contains("hủy"),
                "Message phải chứa thông báo canceled");

        verify(bidRepository, never()).save(any(Bid.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("Edge: Bid với User không tồn tại trả về lỗi, DB không bị ghi")
    public void test_Edge_BidWithNonExistentUser_ShouldFail() {
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        BidResponse response = auctionService.updateBid(1, 999, new BigDecimal("1500.00"));

        assertFalse(response.isSuccess(), "Bid với user không tồn tại phải thất bại");
        assertNotNull(response.getMessage(), "Phải trả về thông báo lỗi");

        verify(bidRepository, never()).save(any(Bid.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }
}
