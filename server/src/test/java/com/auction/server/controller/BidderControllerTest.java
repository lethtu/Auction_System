package com.auction.server.controller;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.service.BidderService;
import com.auction.server.util.SessionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BidderController.class)
public class BidderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionManager sessionManager;

    @MockBean
    private BidderService bidderService;

    @MockBean
    private com.auction.server.repository.AuctionSessionRepository auctionSessionRepository;

    @MockBean
    private com.auction.server.repository.BidRepository bidRepository;

    @MockBean
    private com.auction.server.repository.UserRepository userRepository;

    @Test
    @DisplayName("POST /up-to-seller: valid bidder returns success body")
    public void testUpToSeller_API_ThanhCong() throws Exception {
        mockBidderToken(10);

        Mockito.when(bidderService.upToSeller(10))
                .thenReturn(Map.of("success", true, "message", "Account upgraded successfully"));

        mockMvc.perform(post("/api/bidder/up-to-seller")
                        .header("X-Auth-Token", "valid_token")
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Account upgraded successfully"))
                .andExpect(jsonPath("$.data").value("SUCCESS"));

        Mockito.verify(sessionManager).updateRoleByUserId(10, "seller");
    }

    @Test
    @DisplayName("POST /up-to-seller: missing user returns failed body")
    public void testUpToSeller_API_KhongTimThayUser() throws Exception {
        Mockito.when(sessionManager.getSession("valid_token"))
                .thenReturn(new SessionManager.SessionUser(999, "bidder_999", "bidder"));

        Mockito.when(bidderService.upToSeller(999))
                .thenReturn(Map.of("success", false, "message", "User does not exist"));

        mockMvc.perform(post("/api/bidder/up-to-seller")
                        .header("X-Auth-Token", "valid_token")
                        .param("userId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("User does not exist"))
                .andExpect(jsonPath("$.data").value("FAILED"));

        Mockito.verify(sessionManager, Mockito.never()).updateRoleByUserId(Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    @DisplayName("POST /up-to-seller: seller account returns failed body")
    public void testUpToSeller_API_DaLaSeller() throws Exception {
        Mockito.when(sessionManager.getSession("valid_token"))
                .thenReturn(new SessionManager.SessionUser(14, "bidder_14", "bidder"));

        Mockito.when(bidderService.upToSeller(14))
                .thenReturn(Map.of("success", false, "message", "Account is not a BIDDER or is already a SELLER"));

        mockMvc.perform(post("/api/bidder/up-to-seller")
                        .header("X-Auth-Token", "valid_token")
                        .param("userId", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Account is not a BIDDER or is already a SELLER"))
                .andExpect(jsonPath("$.data").value("FAILED"));

        Mockito.verify(sessionManager, Mockito.never()).updateRoleByUserId(Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    @DisplayName("GET /active-sessions: returns paged active sessions")
    public void activeSessions_ReturnsPagedDataFromService() throws Exception {
        mockBidderToken(10);

        AuctionSession session = new AuctionSession();
        session.setId(99);

        Mockito.when(bidderService.getActiveSessions(1, 5))
                .thenReturn(new PageImpl<>(List.of(session)));

        mockMvc.perform(get("/api/bidder/active-sessions")
                        .header("X-Auth-Token", "valid_token")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Active auction sessions retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].id").value(99));

        Mockito.verify(bidderService).getActiveSessions(1, 5);
    }

    @Test
    @DisplayName("GET /my-bidding-sessions: invalid bidderId returns 400 body")
    public void myBiddingSessions_InvalidBidderId_ReturnsBadRequestBody() throws Exception {
        Mockito.when(sessionManager.getSession("valid_token"))
                .thenReturn(new SessionManager.SessionUser(1, "admin", "admin"));

        mockMvc.perform(get("/api/bidder/my-bidding-sessions")
                        .header("X-Auth-Token", "valid_token")
                        .param("bidderId", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid bidderId"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        Mockito.verify(bidderService, Mockito.never()).getMyBiddingSessions(Mockito.anyInt());
    }

    @Test
    @DisplayName("GET /my-bidding-sessions: valid bidderId returns sessions")
    public void myBiddingSessions_ValidBidderId_ReturnsSessions() throws Exception {
        mockBidderToken(10);

        AuctionSession session = new AuctionSession();
        session.setId(30);

        Mockito.when(bidderService.getMyBiddingSessions(10)).thenReturn(List.of(session));

        mockMvc.perform(get("/api/bidder/my-bidding-sessions")
                        .header("X-Auth-Token", "valid_token")
                        .param("bidderId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Bidder's auction sessions retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value(30));

        Mockito.verify(bidderService).getMyBiddingSessions(10);
    }

    @Test
    @DisplayName("POST /deposit: existing bidder returns new balance")
    public void depositMoney_UserExists_ReturnsNewBalance() throws Exception {
        mockBidderToken(10);

        Mockito.when(bidderService.depositMoney(10, new BigDecimal("100.50")))
                .thenReturn(Optional.of(new BigDecimal("600.50")));

        mockMvc.perform(post("/api/bidder/deposit")
                        .header("X-Auth-Token", "valid_token")
                        .param("bidderId", "10")
                        .param("amount", "100.50"))
                .andExpect(status().isOk())
                .andExpect(content().string("New balance: 600.50"));

        Mockito.verify(bidderService).depositMoney(10, new BigDecimal("100.50"));
    }

    @Test
    @DisplayName("POST /deposit: missing bidder returns not found")
    public void depositMoney_UserMissing_ReturnsNotFound() throws Exception {
        mockBidderToken(10);

        Mockito.when(bidderService.depositMoney(10, BigDecimal.TEN))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/bidder/deposit")
                        .header("X-Auth-Token", "valid_token")
                        .param("bidderId", "10")
                        .param("amount", "10"))
                .andExpect(status().isNotFound());

        Mockito.verify(bidderService).depositMoney(10, BigDecimal.TEN);
    }

    @Test
    @DisplayName("GET /my-bids: returns bidder session DTOs from service")
    public void myBids_ReturnsDtosFromService() throws Exception {
        mockBidderToken(10);

        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(1);
        dto.setCurrentPrice(new BigDecimal("222222"));
        dto.setHighestBidderId(10);
        dto.setDeliveryRecipient("Winner");

        Mockito.when(bidderService.getMyBids(10)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/bidder/my-bids")
                        .header("X-Auth-Token", "valid_token")
                        .param("bidderId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Participated auction sessions retrieved successfully"))
                .andExpect(jsonPath("$.data[0].currentPrice").value(222222))
                .andExpect(jsonPath("$.data[0].highestBidderId").value(10))
                .andExpect(jsonPath("$.data[0].deliveryRecipient").value("Winner"));
    }

    private void mockBidderToken(int userId) {
        Mockito.when(sessionManager.getSession("valid_token"))
                .thenReturn(new SessionManager.SessionUser(userId, "bidder_" + userId, "bidder"));
    }
}