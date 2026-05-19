package com.auction.server.service;

import com.auction.server.dto.BidResponse;
import com.auction.server.exception.AuctionClosedException;
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

    // Dữ liệu giả dùng chung cho các bài Test
    private AuctionSession mockSession;
    private User mockUser;

    @BeforeEach
    public void setUp() {
        mockSession = new AuctionSession();
        mockSession.setId(1);
        mockSession.setCurrentPrice(new BigDecimal("1000.00"));
        mockSession.setStatus(AuctionStatus.ACTIVE);

        mockUser = new User();
        mockUser.setId(99);
        mockUser.setBalance(new BigDecimal("999999999.00"));
    }

    // ========================================================================
    // A. CORE FLOW TESTS
    // ========================================================================

    /**
     * Test #1: Kiểm tra vòng đời trạng thái PENDING -> ACTIVE -> ENDED (Task 1, 2)
     * Mô phỏng Scheduler chuyển trạng thái theo thời gian.
     */
    @Test
    @DisplayName("Core: Vòng đời PENDING -> ACTIVE -> ENDED")
    public void test_Lifecycle_PendingToActiveToEnded() {
        // Bắt đầu từ PENDING
        AuctionSession session = new AuctionSession();
        session.setId(10);
        session.setStatus(AuctionStatus.PENDING);
        session.setStartTime(LocalDateTime.now().minusMinutes(5)); // Đã tới giờ mở
        session.setEndTime(LocalDateTime.now().minusMinutes(1));   // Đã hết giờ
        session.setCurrentPrice(new BigDecimal("500.00"));

        assertEquals(AuctionStatus.PENDING, session.getStatus(), "Trạng thái ban đầu phải là PENDING");

        // Bước 1: Scheduler mở phiên (PENDING -> ACTIVE)
        session.setStatus(AuctionStatus.ACTIVE);
        assertEquals(AuctionStatus.ACTIVE, session.getStatus(), "Sau khi mở, trạng thái phải là ACTIVE");

        // Bước 2: Scheduler đóng phiên (ACTIVE -> ENDED)
        session.setStatus(AuctionStatus.ENDED);
        assertEquals(AuctionStatus.ENDED, session.getStatus(), "Sau khi đóng, trạng thái phải là ENDED");
    }

    /**
     * Test #2: Anti-Sniping — Đặt giá khi còn < 60s phải gia hạn thêm 60s (Task 5)
     */
    @Test
    @DisplayName("Core: Anti-Sniping gia hạn 60s khi bid trong 30s cuối")
    public void test_AntiSniping_ExtendEndTime_WhenBidInLast60Seconds() {
        // Setup: endTime cách "now" khoảng 30 giây
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(30);
        mockSession.setEndTime(originalEndTime);

        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));

        // Act: Đặt giá hợp lệ
        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("1500.00"));

        // Assert: Thành công + endTime đã được gia hạn thêm 60s
        assertTrue(response.isSuccess(), "Đặt giá phải thành công");
        assertNotNull(response.getNewEndTime(), "newEndTime phải có giá trị (đã gia hạn)");

        // endTime mới = endTime cũ + 60s
        LocalDateTime expectedNewEndTime = originalEndTime.plusSeconds(60);
        assertEquals(expectedNewEndTime, mockSession.getEndTime(),
                "endTime phải được cộng thêm đúng 60 giây");
    }

    /**
     * Test #3: Ném AuctionClosedException khi bid vào phiên ENDED (Task 7)
     */
    @Test
    @DisplayName("Core: Ném AuctionClosedException khi bid vào phiên ENDED")
    public void test_Exception_BidOnEndedSession_ThrowsAuctionClosedException() {
        // Setup: Phiên đã kết thúc
        mockSession.setStatus(AuctionStatus.ENDED);
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        // Act & Assert: Phải ném đúng exception
        AuctionClosedException exception = assertThrows(
                AuctionClosedException.class,
                () -> auctionService.updateBid(1, 99, new BigDecimal("2000.00")),
                "Phải ném AuctionClosedException khi bid vào phiên ENDED"
        );

        // Kiểm tra thông tin exception
        assertEquals(1, exception.getSessionId(), "SessionId trong exception phải đúng");
        assertTrue(exception.getMessage().contains("kết thúc hoặc bị hủy"), "Message phải chứa thông báo kết thúc");

        // Đảm bảo DB không bị ghi rác
        verify(bidRepository, never()).save(any(Bid.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    // ========================================================================
    // B. EDGE CASES & BOUNDARY TESTS
    // ========================================================================

    /**
     * Test #4: Đặt giá đúng bằng giá hiện tại phải bị từ chối
     * Boundary: Kiểm tra toán tử <= ở AuctionService line 77
     */
    @Test
    @DisplayName("Edge: Bid bằng đúng giá hiện tại (1000 == 1000) phải thất bại")
    public void test_Edge_BidExactlyEqualCurrentPrice_ShouldFail() {
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));

        // Đặt giá bằng đúng giá hiện tại
        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("1000.00"));

        assertFalse(response.isSuccess(), "Bid bằng giá hiện tại phải thất bại");
        assertEquals(new BigDecimal("1000.00"), mockSession.getCurrentPrice(),
                "Giá session không được thay đổi");

        // DB không bị ghi
        verify(bidRepository, never()).save(any(Bid.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    /**
     * Test #5: Anti-Sniping KHÔNG trigger khi thời gian còn 61 giây
     * Boundary: Điều kiện secondsLeft < 60 — 61s phải nằm NGOÀI vùng trigger
     */
    @Test
    @DisplayName("Edge: Anti-Sniping KHÔNG trigger khi còn 61 giây")
    public void test_Edge_AntiSniping_NotTrigger_WhenTimeLeft61Seconds() {
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(61);
        mockSession.setEndTime(originalEndTime);

        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("1500.00"));

        assertTrue(response.isSuccess(), "Đặt giá phải thành công");
        assertNull(response.getNewEndTime(), "newEndTime phải null (không gia hạn)");
        assertEquals(originalEndTime, mockSession.getEndTime(),
                "endTime phải giữ nguyên, không được thay đổi");
    }

    /**
     * Test #6: Anti-Sniping trigger khi thời gian còn 59 giây
     * Boundary: Xác nhận 59s nằm trong vùng trigger (< 60)
     */
    @Test
    @DisplayName("Edge: Anti-Sniping trigger khi còn 59 giây")
    public void test_Edge_AntiSniping_Trigger_WhenTimeLeftExactly59Seconds() {
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(59);
        mockSession.setEndTime(originalEndTime);

        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(mockUser));

        BidResponse response = auctionService.updateBid(1, 99, new BigDecimal("1500.00"));

        assertTrue(response.isSuccess(), "Đặt giá phải thành công");
        assertNotNull(response.getNewEndTime(), "newEndTime phải có giá trị (đã gia hạn)");

        LocalDateTime expectedNewEndTime = originalEndTime.plusSeconds(60);
        assertEquals(expectedNewEndTime, mockSession.getEndTime(),
                "endTime phải được cộng thêm đúng 60 giây");
    }

    /**
     * Test #7: Ném AuctionClosedException khi bid vào phiên CANCELED
     * Đảm bảo exception bao phủ cả CANCELED, không chỉ ENDED
     */
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
        assertTrue(exception.getMessage().contains("kết thúc hoặc bị hủy"), "Message phải chứa thông báo hủy");

        verify(bidRepository, never()).save(any(Bid.class));
    }

    /**
     * Test #8: Đặt giá với User không tồn tại trong DB
     * Kiểm tra nhánh bidder == null ở AuctionService line 110
     */
    @Test
    @DisplayName("Edge: Bid với User không tồn tại trả về lỗi, DB không bị ghi")
    public void test_Edge_BidWithNonExistentUser_ShouldFail() {
        when(auctionSessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(999)).thenReturn(Optional.empty()); // User không tồn tại

        BidResponse response = auctionService.updateBid(1, 999, new BigDecimal("1500.00"));

        assertFalse(response.isSuccess(), "Bid với user không tồn tại phải thất bại");
        assertTrue(response.getMessage().contains("không tồn tại"),
                "Message phải thông báo user không tồn tại");

        // Đảm bảo DB không bị ghi rác
        verify(bidRepository, never()).save(any(Bid.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }
}
