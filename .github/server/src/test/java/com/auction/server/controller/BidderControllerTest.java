package com.auction.server.controller;

import com.auction.server.service.BidderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BidderController.class)
public class BidderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock toàn bộ tầng service và repository để controller test chạy độc lập
    @MockBean
    private BidderService bidderService;

    @MockBean
    private com.auction.server.repository.AuctionSessionRepository auctionSessionRepository;

    @MockBean
    private com.auction.server.repository.BidRepository bidRepository;

    @MockBean
    private com.auction.server.repository.UserRepository userRepository;

    // =====================================================================
    // TEST 1: API /api/bidder/up-to-seller - Nâng cấp thành công
    // =====================================================================
    @Test
    @DisplayName("POST /up-to-seller: BIDDER hợp lệ -> HTTP 200 và success = true")
    public void testUpToSeller_API_ThanhCong() throws Exception {
        // Giả lập service trả về kết quả thành công
        Mockito.when(bidderService.upToSeller(10))
                .thenReturn(Map.of("success", true, "message", "Account upgraded successfully"));

        mockMvc.perform(post("/api/bidder/up-to-seller")
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Account upgraded successfully"))
                .andExpect(jsonPath("$.data").value("SUCCESS"));
    }

    // =====================================================================
    // TEST 2: API /api/bidder/up-to-seller - Không tìm thấy user
    // =====================================================================
    @Test
    @DisplayName("POST /up-to-seller: userId không tồn tại -> HTTP 200 với status 400 trong body")
    public void testUpToSeller_API_KhongTimThayUser() throws Exception {
        Mockito.when(bidderService.upToSeller(999))
                .thenReturn(Map.of("success", false, "message", "User does not exist"));

        mockMvc.perform(post("/api/bidder/up-to-seller")
                        .param("userId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("User does not exist"))
                .andExpect(jsonPath("$.data").value("FAILED"));
    }

    // =====================================================================
    // TEST 3: API /api/bidder/up-to-seller - User đã là SELLER
    // =====================================================================
    @Test
    @DisplayName("POST /up-to-seller: User đã là SELLER -> HTTP 200 với status 400 trong body")
    public void testUpToSeller_API_DaLaSeller() throws Exception {
        Mockito.when(bidderService.upToSeller(14))
                .thenReturn(Map.of("success", false, "message", "Account is not a BIDDER or is already a SELLER"));

        mockMvc.perform(post("/api/bidder/up-to-seller")
                        .param("userId", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Account is not a BIDDER or is already a SELLER"))
                .andExpect(jsonPath("$.data").value("FAILED"));
    }
}
