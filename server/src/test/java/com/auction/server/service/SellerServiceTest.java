package com.auction.server.service;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.model.Seller;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository;
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

@ExtendWith(MockitoExtension.class)
public class SellerServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private AuctionSessionRepository auctionSessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SellerService sellerService;

    private Seller mockSeller;

    @BeforeEach
    public void setUp() {
        mockSeller = new Seller();
        mockSeller.setId(2);
        mockSeller.setUsername("seller_test");

        // Mock các hành vi lưu DB và lấy User
        when(userRepository.findById(2)).thenReturn(Optional.of(mockSeller));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auctionSessionRepository.save(any(AuctionSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Tạo phiên COMING: Thời gian bắt đầu ở tương lai -> Status trên DB phải là COMING")
    public void testCreateSession_Status_Coming() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setSellerId(2);
        request.setName("Sản phẩm Coming");
        request.setType("Electronics");
        request.setStartingPrice(new BigDecimal("1000000"));
        request.setStepPrice(new BigDecimal("50000"));
        
        // Thời gian bắt đầu ở tương lai (2 ngày sau)
        LocalDateTime futureStart = LocalDateTime.now().plusDays(2);
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(3);
        request.setStartTime(futureStart);
        request.setEndTime(futureEnd);

        // Chạy hành động tạo phiên
        AuctionSession session = sellerService.createAuctionSession(request);

        // Kiểm chứng trạng thái lưu vào DB
        assertNotNull(session);
        assertEquals(AuctionStatus.COMING, session.getStatus(), "Phiên có thời gian bắt đầu ở tương lai phải có status lưu DB là COMING");
        assertEquals(futureStart, session.getStartTime());
        assertEquals(futureEnd, session.getEndTime());
        
        // Verify các thao tác repository được kích hoạt chính xác
        verify(itemRepository, times(1)).save(any(Item.class));
        verify(auctionSessionRepository, times(1)).save(any(AuctionSession.class));
    }

    @Test
    @DisplayName("Tạo phiên ACTIVE: Thời gian bắt đầu ở quá khứ -> Status trên DB phải là ACTIVE")
    public void testCreateSession_Status_Active() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setSellerId(2);
        request.setName("Sản phẩm Active");
        request.setType("Electronics");
        request.setStartingPrice(new BigDecimal("2000000"));
        request.setStepPrice(new BigDecimal("50000"));
        
        // Thời gian bắt đầu ở quá khứ (1 ngày trước)
        LocalDateTime pastStart = LocalDateTime.now().minusDays(1);
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(2);
        request.setStartTime(pastStart);
        request.setEndTime(futureEnd);

        // Chạy hành động tạo phiên
        AuctionSession session = sellerService.createAuctionSession(request);

        // Kiểm chứng trạng thái lưu vào DB
        assertNotNull(session);
        assertEquals(AuctionStatus.ACTIVE, session.getStatus(), "Phiên có thời gian bắt đầu ở quá khứ phải có status lưu DB là ACTIVE");
        assertEquals(pastStart, session.getStartTime());
        assertEquals(futureEnd, session.getEndTime());
        
        // Verify các thao tác repository được kích hoạt chính xác
        verify(itemRepository, times(1)).save(any(Item.class));
        verify(auctionSessionRepository, times(1)).save(any(AuctionSession.class));
    }
}
