package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.service.SellerService;
import com.auction.server.util.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerControllerTest {

    private MockHttpServletRequest mockReq;

    @BeforeEach
    void setUp() {
        mockReq = new MockHttpServletRequest();
        SessionManager.SessionUser sessionUser = new SessionManager.SessionUser(10, "seller_user", "seller");
        mockReq.setAttribute("sessionUser", sessionUser);
    }

    @Test
    void createAuction_success_returnsApiResponseSuccess() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        CreateAuctionRequest request = new CreateAuctionRequest();

        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(1);
        dto.setProductName("Laptop");
        service.createResult = dto;

        ApiResponse<SessionResponseDTO> response = controller.createAuction(mockReq, request);

        assertEquals(200, response.getStatus());
        assertEquals("Auction session created successfully.", response.getMessage());
        assertSame(dto, response.getData());
        assertSame(request, service.createdRequest);
        assertEquals(10, request.getSellerId());
    }

    @Test
    void createAuction_illegalArgument_returnsBadRequest() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        service.createException = new IllegalArgumentException("Invalid starting price");

        ApiResponse<SessionResponseDTO> response = controller.createAuction(mockReq, new CreateAuctionRequest());

        assertEquals(400, response.getStatus());
        assertEquals("Invalid starting price", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void createAuction_missingSessionUser_returnsBadRequest() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);
        MockHttpServletRequest requestWithoutUser = new MockHttpServletRequest();

        ApiResponse<SessionResponseDTO> response = controller.createAuction(requestWithoutUser, new CreateAuctionRequest());

        assertEquals(400, response.getStatus());
        assertEquals("Seller not authenticated", response.getMessage());
        assertNull(response.getData());
        assertNull(service.createdRequest);
    }

    @Test
    void viewMySessions_success_returnsSessionList() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(2);
        dto.setProductName("Phone");
        service.mySessionsResult = List.of(dto);

        ApiResponse<List<SessionResponseDTO>> response = controller.viewMySessions(mockReq, 10, "PENDING");

        assertEquals(200, response.getStatus());
        assertEquals("Session list retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals("Phone", response.getData().get(0).getProductName());
        assertEquals(10, service.lastSellerId);
        assertEquals("PENDING", service.lastStatus);
    }

    @Test
    void getSessionDetail_success_returnsSession() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(5);
        service.detailResult = dto;

        ApiResponse<SessionResponseDTO> response = controller.getSessionDetail(mockReq, 5, 10);

        assertEquals(200, response.getStatus());
        assertEquals("Session details retrieved successfully", response.getMessage());
        assertSame(dto, response.getData());
        assertEquals(5, service.lastSessionId);
        assertEquals(10, service.lastSellerId);
    }

    @Test
    void updateSession_success_overridesSellerIdAndReturnsSession() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);
        CreateAuctionRequest request = new CreateAuctionRequest();

        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(9);
        service.updateResult = dto;

        ApiResponse<SessionResponseDTO> response = controller.updateSession(mockReq, 9, 999, request);

        assertEquals(200, response.getStatus());
        assertEquals("Pending session updated successfully.", response.getMessage());
        assertSame(dto, response.getData());
        assertEquals(9, service.lastSessionId);
        assertEquals(10, service.lastSellerId);
        assertSame(request, service.updatedRequest);
        assertEquals(10, request.getSellerId());
    }

    @Test
    void updateSession_runtimeException_returnsServerError() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        service.updateException = new RuntimeException("Update failed");

        ApiResponse<SessionResponseDTO> response = controller.updateSession(mockReq, 9, 999, new CreateAuctionRequest());

        assertEquals(500, response.getStatus());
        assertEquals("Update failed", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void cancelAuction_success_returnsSuccessResponse() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        ApiResponse<Void> response = controller.cancelAuction(mockReq, 8, 10);

        assertEquals(200, response.getStatus());
        assertEquals("Session canceled successfully", response.getMessage());
        assertNull(response.getData());
        assertEquals(8, service.lastSessionId);
        assertEquals(10, service.lastSellerId);
        assertTrue(service.cancelCalled);
    }

    @Test
    void cancelAuction_runtimeException_returnsServerError() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        service.cancelException = new RuntimeException("Cannot cancel session");

        ApiResponse<Void> response = controller.cancelAuction(mockReq, 8, 10);

        assertEquals(500, response.getStatus());
        assertEquals("Cannot cancel session", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void getStats_success_returnsStatsResponse() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        ApiResponse<SellerStatsDTO> response = controller.getStats(mockReq, 10);

        assertEquals(200, response.getStatus());
        assertEquals("Statistics retrieved successfully", response.getMessage());
        assertNull(response.getData());
        assertEquals(10, service.lastSellerId);
    }

    @Test
    void deleteItem_success_returnsSuccessResponse() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        ApiResponse<Void> response = controller.deleteItem(mockReq, 50);

        assertEquals(200, response.getStatus());
        assertEquals("Product removed", response.getMessage());
        assertNull(response.getData());
        assertEquals(50, service.lastDeletedItemId);
        assertEquals(10, service.deleteSellerId);
        assertTrue(service.deleteCalled);
    }

    private static class FakeSellerService extends SellerService {
        private SessionResponseDTO createResult;
        private SessionResponseDTO detailResult;
        private SessionResponseDTO updateResult;
        private List<SessionResponseDTO> mySessionsResult = List.of();

        private CreateAuctionRequest createdRequest;
        private CreateAuctionRequest updatedRequest;

        private Integer lastSessionId;
        private Integer lastSellerId;
        private String lastStatus;

        private boolean cancelCalled;

        private RuntimeException createException;
        private RuntimeException updateException;
        private RuntimeException cancelException;

        private Integer lastDeletedItemId;
        private Integer deleteSellerId;
        private boolean deleteCalled;

        FakeSellerService() {
            super(null, null, null, org.mockito.Mockito.mock(com.auction.server.mapper.SessionResponseMapper.class));
        }

        @Override
        public SessionResponseDTO createAuctionSession(CreateAuctionRequest request) {
            if (createException != null) {
                throw createException;
            }

            this.createdRequest = request;
            return createResult;
        }

        @Override
        public List<SessionResponseDTO> getMySessions(Integer sellerId, String status) {
            this.lastSellerId = sellerId;
            this.lastStatus = status;
            return mySessionsResult;
        }

        @Override
        public SessionResponseDTO getSessionDetail(Integer sessionId, Integer sellerId) {
            this.lastSessionId = sessionId;
            this.lastSellerId = sellerId;
            return detailResult;
        }

        @Override
        public SessionResponseDTO updateSession(Integer sessionId, Integer sellerId, CreateAuctionRequest request) {
            if (updateException != null) {
                throw updateException;
            }

            this.lastSessionId = sessionId;
            this.lastSellerId = sellerId;
            this.updatedRequest = request;
            return updateResult;
        }

        @Override
        public void cancelSession(Integer sessionId, Integer sellerId) {
            if (cancelException != null) {
                throw cancelException;
            }

            this.lastSessionId = sessionId;
            this.lastSellerId = sellerId;
            this.cancelCalled = true;
        }

        @Override
        public SellerStatsDTO getSellerStats(Integer sellerId) {
            this.lastSellerId = sellerId;
            return null;
        }

        @Override
        public void softDeleteItem(Integer itemId, Integer sellerId) {
            this.lastDeletedItemId = itemId;
            this.deleteSellerId = sellerId;
            this.deleteCalled = true;
        }
    }
}