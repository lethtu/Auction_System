package com.auction.client.service;

import com.auction.client.api.SellerApiClient;
import com.auction.client.dto.ApiResult;
import com.auction.client.dto.CreateAuctionRequest;
import com.auction.client.model.SessionItem;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SellerDashboardServiceTest {

    private final FakeSellerApiClient sellerApiClient = new FakeSellerApiClient();
    private final SellerDashboardService service = new SellerDashboardService(sellerApiClient);

    @Test
    void createAuction_success_returnsSuccessResult() throws Exception {
        sellerApiClient.createResponse = response(200, "{\"status\":200,\"message\":\"Tạo thành công\"}");

        ApiResult<Void> result = service.createAuction(newRequest());

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Tạo thành công", result.message);
        assertNotNull(sellerApiClient.lastBody);
        assertEquals("Laptop", sellerApiClient.lastBody.getString("name"));
    }

    @Test
    void updateSession_error_returnsErrorResult() throws Exception {
        sellerApiClient.updateResponse = response(400, "{\"status\":400,\"message\":\"Cập nhật thất bại\"}");

        ApiResult<Void> result = service.updateSession(10, 2, newRequest());

        assertFalse(result.success);
        assertEquals(400, result.status);
        assertEquals("Cập nhật thất bại", result.message);
        assertEquals(10, sellerApiClient.lastSessionId);
        assertEquals(2, sellerApiClient.lastSellerId);
        assertNotNull(sellerApiClient.lastBody);
    }

    @Test
    void getMySessions_success_returnsSessionList() throws Exception {
        sellerApiClient.mySessionsResponse = response(200, """
                {
                  "status": 200,
                  "message": "OK",
                  "data": [
                    {
                      "id": 1,
                      "productName": "Laptop",
                      "productType": "Electronics",
                      "imageUrl": "laptop.png",
                      "description": "Gaming laptop",
                      "startingPrice": 1000000,
                      "currentPrice": 1500000,
                      "stepPrice": 100000,
                      "endTime": "2026-05-20T10:00:00",
                      "status": "PENDING"
                    }
                  ]
                }
                """);

        List<SessionItem> result = service.getMySessions(2);

        assertEquals(1, result.size());

        SessionItem item = result.get(0);
        assertEquals(1, item.id);
        assertEquals("Laptop", item.productName);
        assertEquals("Electronics", item.productType);
        assertEquals("laptop.png", item.imageUrl);
        assertEquals("Gaming laptop", item.description);
        assertEquals(new BigDecimal("1000000"), item.startingPrice);
        assertEquals(new BigDecimal("1500000"), item.currentPrice);
        assertEquals(new BigDecimal("100000"), item.stepPrice);
        assertEquals("2026-05-20T10:00:00", item.endTime);
        assertEquals("PENDING", item.status);
        assertEquals(2, sellerApiClient.lastSellerId);
        assertNull(sellerApiClient.lastStatus);
    }

    @Test
    void getMySessionsWithStatus_success_callsApiWithStatus() throws Exception {
        sellerApiClient.mySessionsResponse = response(200, """
                {
                  "status": 200,
                  "data": [
                    {
                      "id": 2,
                      "productName": "Phone",
                      "status": "ACTIVE"
                    }
                  ]
                }
                """);

        List<SessionItem> result = service.getMySessions(2, "ACTIVE");

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).id);
        assertEquals("Phone", result.get(0).productName);
        assertEquals("ACTIVE", result.get(0).status);
        assertEquals(2, sellerApiClient.lastSellerId);
        assertEquals("ACTIVE", sellerApiClient.lastStatus);
    }

    @Test
    void getMySessions_errorResponse_throwsException() throws Exception {
        sellerApiClient.mySessionsResponse = response(403, """
                {
                  "status": 403,
                  "message": "Không có quyền"
                }
                """);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.getMySessions(2)
        );

        assertEquals("Không có quyền", ex.getMessage());
    }

    @Test
    void cancelSession_success_returnsSuccessResult() throws Exception {
        sellerApiClient.cancelResponse = response(200, "{\"status\":200,\"message\":\"Đã hủy phiên\"}");

        ApiResult<Void> result = service.cancelSession(5, 2);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Đã hủy phiên", result.message);
        assertEquals(5, sellerApiClient.lastSessionId);
        assertEquals(2, sellerApiClient.lastSellerId);
    }

    @Test
    void cancelSession_successEmptyBody_usesDefaultMessage() throws Exception {
        sellerApiClient.cancelResponse = response(200, "");

        ApiResult<Void> result = service.cancelSession(5, 2);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Đã hủy phiên thành công.", result.message);
    }

    private CreateAuctionRequest newRequest() {
        try {
            for (Constructor<?> constructor : CreateAuctionRequest.class.getConstructors()) {
                if (constructor.getParameterCount() == 9) {
                    return (CreateAuctionRequest) constructor.newInstance(
                            "Laptop",
                            "Electronics",
                            "laptop.png",
                            "Gaming laptop",
                            new BigDecimal("1000000"),
                            new BigDecimal("100000"),
                            "2026-05-12T10:00:00",
                            "2026-05-20T10:00:00",
                            2
                    );
                }

                if (constructor.getParameterCount() == 8) {
                    return (CreateAuctionRequest) constructor.newInstance(
                            "Laptop",
                            "Electronics",
                            "laptop.png",
                            "Gaming laptop",
                            new BigDecimal("1000000"),
                            new BigDecimal("100000"),
                            "2026-05-20T10:00:00",
                            2
                    );
                }
            }

            throw new IllegalStateException("Không tìm thấy constructor phù hợp cho CreateAuctionRequest.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpResponse<String> response(int status, String body) {
        return new FakeHttpResponse(status, body);
    }

    private static class FakeSellerApiClient extends SellerApiClient {
        private HttpResponse<String> createResponse;
        private HttpResponse<String> updateResponse;
        private HttpResponse<String> mySessionsResponse;
        private HttpResponse<String> cancelResponse;

        private Integer lastSessionId;
        private Integer lastSellerId;
        private String lastStatus;
        private JSONObject lastBody;

        @Override
        public HttpResponse<String> createAuction(JSONObject body) {
            lastBody = body;
            return createResponse;
        }

        @Override
        public HttpResponse<String> updateSession(int sessionId, int sellerId, JSONObject body) {
            lastSessionId = sessionId;
            lastSellerId = sellerId;
            lastBody = body;
            return updateResponse;
        }

        @Override
        public HttpResponse<String> getMySessions(int sellerId) {
            lastSellerId = sellerId;
            lastStatus = null;
            return mySessionsResponse;
        }

        @Override
        public HttpResponse<String> getMySessions(int sellerId, String status) {
            lastSellerId = sellerId;
            lastStatus = status;
            return mySessionsResponse;
        }

        @Override
        public HttpResponse<String> cancelSession(int sessionId, int sellerId) {
            lastSessionId = sessionId;
            lastSellerId = sellerId;
            return cancelResponse;
        }
    }

    private record FakeHttpResponse(int statusCode, String body) implements HttpResponse<String> {
        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return URI.create("http://localhost");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}