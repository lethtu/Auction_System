package com.auction.server.service;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.*;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.util.SellerSessionGuard;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SellerServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private AuctionSessionRepository auctionSessionRepository;

    @Mock
    private SellerSessionGuard sellerSessionGuard;

    @InjectMocks
    private SellerService sellerService;

    private Seller mockSeller;
    private Item mockItem;

    @BeforeEach
    public void setUp() {
        mockSeller = new Seller();
        mockSeller.setId(2);
        mockSeller.setUsername("seller_test");

        mockItem = new Electronics();
        mockItem.setId(100);
        mockItem.setName("Laptop Gaming");
        mockItem.setType("electronics");
        mockItem.setDescription("Máy còn tốt");
    }

    @Test
    @DisplayName("Tạo phiên COMING: Thời gian bắt đầu ở tương lai -> Status trên DB phải là COMING")
    public void testCreateSession_Status_Coming() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setSellerId(2);
        request.setName("Sản phẩm Coming");
        request.setType("electronics");
        request.setStartingPrice(new BigDecimal("1000000"));
        request.setStepPrice(new BigDecimal("50000"));
        request.setReservePrice(new BigDecimal("1200000"));
        
        LocalDateTime futureStart = LocalDateTime.now().plusDays(2);
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(3);
        request.setStartTime(futureStart);
        request.setEndTime(futureEnd);

        when(sellerSessionGuard.getSellerById(2)).thenReturn(mockSeller);
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionSessionRepository.save(any(AuctionSession.class))).thenAnswer(inv -> {
            AuctionSession saved = inv.getArgument(0);
            saved.setId(10);
            return saved;
        });

        SessionResponseDTO result = sellerService.createAuctionSession(request);

        assertNotNull(result);
        assertEquals("COMING", result.getStatus());
        assertEquals("Sản phẩm Coming", result.getProductName());
        assertEquals(new BigDecimal("1200000"), result.getReservePrice());
        
        verify(itemRepository, times(1)).save(any(Item.class));
        verify(auctionSessionRepository, times(1)).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("Tạo phiên ACTIVE: Thời gian bắt đầu ở quá khứ/hiện tại -> Status trên DB phải là ACTIVE")
    public void testCreateSession_Status_Active() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setSellerId(2);
        request.setName("Sản phẩm Active");
        request.setType("electronics");
        request.setStartingPrice(new BigDecimal("2000000"));
        request.setStepPrice(new BigDecimal("50000"));
        
        LocalDateTime pastStart = LocalDateTime.now().minusDays(1);
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(2);
        request.setStartTime(pastStart);
        request.setEndTime(futureEnd);

        when(sellerSessionGuard.getSellerById(2)).thenReturn(mockSeller);
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionSessionRepository.save(any(AuctionSession.class))).thenAnswer(inv -> {
            AuctionSession saved = inv.getArgument(0);
            saved.setId(11);
            return saved;
        });

        SessionResponseDTO result = sellerService.createAuctionSession(request);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        
        verify(itemRepository, times(1)).save(any(Item.class));
        verify(auctionSessionRepository, times(1)).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("Tạo phiên DRAFT: Request status là DRAFT -> Status trên DB phải là DRAFT")
    public void testCreateSession_Status_Draft() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setSellerId(2);
        request.setName("Sản phẩm Draft");
        request.setType("electronics");
        request.setStatus("DRAFT");

        when(sellerSessionGuard.getSellerById(2)).thenReturn(mockSeller);
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionSessionRepository.save(any(AuctionSession.class))).thenAnswer(inv -> {
            AuctionSession saved = inv.getArgument(0);
            saved.setId(12);
            return saved;
        });

        SessionResponseDTO result = sellerService.createAuctionSession(request);

        assertNotNull(result);
        assertEquals("DRAFT", result.getStatus());
        
        verify(itemRepository, times(1)).save(any(Item.class));
        verify(auctionSessionRepository, times(1)).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("Cập nhật phiên thành công")
    public void testUpdateSession_Success() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setSellerId(2);
        request.setName("Laptop Gaming New");
        request.setType("electronics");
        request.setStartingPrice(new BigDecimal("1200000"));
        request.setStepPrice(new BigDecimal("100000"));
        request.setEndTime(LocalDateTime.now().plusDays(2));

        AuctionSession existingSession = new AuctionSession();
        existingSession.setId(20);
        existingSession.setSeller(mockSeller);
        existingSession.setItem(mockItem);
        existingSession.setStatus(AuctionStatus.DRAFT);

        when(sellerSessionGuard.getSessionById(20)).thenReturn(existingSession);
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionSessionRepository.save(any(AuctionSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponseDTO result = sellerService.updateSession(20, 2, request);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals("Laptop Gaming New", result.getProductName());

        verify(itemRepository, times(1)).save(any(Item.class));
        verify(auctionSessionRepository, times(1)).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("Cập nhật phiên thất bại khi trạng thái không hợp lệ (ví dụ: ENDED)")
    public void testUpdateSession_InvalidStatus() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setSellerId(2);
        request.setName("Laptop Gaming New");
        request.setType("electronics");
        request.setStartingPrice(new BigDecimal("1200000"));
        request.setStepPrice(new BigDecimal("100000"));
        request.setEndTime(LocalDateTime.now().plusDays(2));

        AuctionSession existingSession = new AuctionSession();
        existingSession.setId(20);
        existingSession.setSeller(mockSeller);
        existingSession.setItem(mockItem);
        existingSession.setStatus(AuctionStatus.ENDED);

        when(sellerSessionGuard.getSessionById(20)).thenReturn(existingSession);

        assertThrows(IllegalArgumentException.class, () -> {
            sellerService.updateSession(20, 2, request);
        });

        verify(itemRepository, never()).save(any(Item.class));
        verify(auctionSessionRepository, never()).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("Hủy phiên thành công")
    public void testCancelSession_Success() {
        AuctionSession existingSession = new AuctionSession();
        existingSession.setId(30);
        existingSession.setSeller(mockSeller);
        existingSession.setItem(mockItem);
        existingSession.setStatus(AuctionStatus.COMING);

        when(sellerSessionGuard.getSessionById(30)).thenReturn(existingSession);

        sellerService.cancelSession(30, 2);

        assertEquals(AuctionStatus.CANCELED, existingSession.getStatus());
        verify(auctionSessionRepository, times(1)).save(existingSession);
    }

    @Test
    @DisplayName("Hủy phiên thất bại khi trạng thái đã kết thúc")
    public void testCancelSession_InvalidStatus() {
        AuctionSession existingSession = new AuctionSession();
        existingSession.setId(30);
        existingSession.setSeller(mockSeller);
        existingSession.setItem(mockItem);
        existingSession.setStatus(AuctionStatus.ENDED);

        when(sellerSessionGuard.getSessionById(30)).thenReturn(existingSession);

        assertThrows(IllegalArgumentException.class, () -> {
            sellerService.cancelSession(30, 2);
        });

        verify(auctionSessionRepository, never()).save(existingSession);
    }

    @Test
    @DisplayName("Lấy thống kê seller thành công")
    public void testGetSellerStats() {
        AuctionSession session1 = new AuctionSession();
        session1.setStatus(AuctionStatus.ENDED);
        session1.setCurrentPrice(new BigDecimal("1500000"));

        AuctionSession session2 = new AuctionSession();
        session2.setStatus(AuctionStatus.ENDED);
        session2.setCurrentPrice(new BigDecimal("2500000"));

        when(auctionSessionRepository.findBySeller_IdAndStatus(2, AuctionStatus.ENDED))
                .thenReturn(List.of(session1, session2));

        SellerStatsDTO stats = sellerService.getSellerStats(2);

        assertNotNull(stats);
        assertEquals(2, stats.getTotalSoldItems());
        assertEquals(new BigDecimal("4000000"), stats.getTotalRevenue());
    }

    @Test
    @DisplayName("Lấy danh sách phiên của seller thành công")
    public void testGetMySessions() {
        AuctionSession session1 = new AuctionSession();
        session1.setId(1);
        session1.setSeller(mockSeller);
        session1.setItem(mockItem);
        session1.setStatus(AuctionStatus.ACTIVE);

        when(auctionSessionRepository.findBySeller_Id(2)).thenReturn(List.of(session1));

        List<SessionResponseDTO> result = sellerService.getMySessions(2, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.get(0).getStatus());
    }
}
