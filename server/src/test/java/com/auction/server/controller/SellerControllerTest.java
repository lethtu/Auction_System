package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.service.SellerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SellerControllerTest {

    @Test
    void createAuction_success_returnsApiResponseSuccess() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        CreateAuctionRequest request = new CreateAuctionRequest();

        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(1);
        dto.setProductName("Laptop");
        service.createResult = dto;

        ApiResponse<SessionResponseDTO> response = controller.createAuction(request);

        assertEquals(200, response.getStatus());
        assertEquals("Auction session created successfully.", response.getMessage());
        assertSame(dto, response.getData());
        assertSame(request, service.createdRequest);
    }

    @Test
    void createAuction_illegalArgument_returnsBadRequest() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        service.createException = new IllegalArgumentException("Giá khởi điểm không hợp lệ");

        ApiResponse<SessionResponseDTO> response = controller.createAuction(new CreateAuctionRequest());

        assertEquals(400, response.getStatus());
        assertEquals("Giá khởi điểm không hợp lệ", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void viewMySessions_success_returnsSessionList() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(2);
        dto.setProductName("Phone");
        service.mySessionsResult = List.of(dto);

        ApiResponse<List<SessionResponseDTO>> response = controller.viewMySessions(10, "PENDING");

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

        ApiResponse<SessionResponseDTO> response = controller.getSessionDetail(5, 10);

        assertEquals(200, response.getStatus());
        assertEquals("Session details retrieved successfully", response.getMessage());
        assertSame(dto, response.getData());
        assertEquals(5, service.lastSessionId);
        assertEquals(10, service.lastSellerId);
    }

    @Test
    void cancelAuction_success_returnsSuccessResponse() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        ApiResponse<Void> response = controller.cancelAuction(8, 10);

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

        service.cancelException = new RuntimeException("Không thể hủy phiên này");

        ApiResponse<Void> response = controller.cancelAuction(8, 10);

        assertEquals(500, response.getStatus());
        assertEquals("Không thể hủy phiên này", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void getStats_success_returnsStatsResponse() {
        FakeSellerService service = new FakeSellerService();
        SellerController controller = new SellerController(service);

        ApiResponse<SellerStatsDTO> response = controller.getStats(10);

        assertEquals(200, response.getStatus());
        assertEquals("Statistics retrieved successfully", response.getMessage());
        assertNull(response.getData());
        assertEquals(10, service.lastSellerId);
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
        private RuntimeException cancelException;

        FakeSellerService() {
            super(null, null, null);
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
    }
}