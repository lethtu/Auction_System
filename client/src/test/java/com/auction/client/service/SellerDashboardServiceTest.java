package com.auction.client.service;

import com.auction.client.api.SellerApiClient;
import com.auction.client.dto.ApiResult;
import com.auction.client.dto.CreateAuctionRequest;
import com.auction.client.model.SessionItem;
import org.junit.jupiter.api.Test;
import org.json.JSONObject;

import javax.net.ssl.SSLSession;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SellerDashboardServiceTest {

    private final SellerApiClient sellerApiClient = mock(SellerApiClient.class);
    private final SellerDashboardService service = new SellerDashboardService(sellerApiClient);

    @Test
    void createAuction_success_returnsSuccessResult() throws Exception {
        CreateAuctionRequest request = mock(CreateAuctionRequest.class);

        when(sellerApiClient.createAuction(any(JSONObject.class)))
                .thenReturn(response(200, "{\"status\":200,\"message\":\"Tạo thành công\"}"));

        ApiResult<Void> result = service.createAuction(request);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Tạo thành công", result.message);
    }

    @Test
    void updateSession_error_returnsErrorResult() throws Exception {
        CreateAuctionRequest request = mock(CreateAuctionRequest.class);

        when(sellerApiClient.updateSession(eq(10), eq(2), any(JSONObject.class)))
                .thenReturn(response(400, "{\"status\":400,\"message\":\"Cập nhật thất bại\"}"));

        ApiResult<Void> result = service.updateSession(10, 2, request);

        assertFalse(result.success);
        assertEquals(400, result.status);
        assertEquals("Cập nhật thất bại", result.message);
    }

    @Test
    void getMySessions_success_returnsSessionList() throws Exception {
        String body = """
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
                """;

        when(sellerApiClient.getMySessions(2)).thenReturn(response(200, body));

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
    }

    @Test
    void getMySessionsWithStatus_success_callsApiWithStatus() throws Exception {
        String body = """
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
                """;

        when(sellerApiClient.getMySessions(2, "ACTIVE")).thenReturn(response(200, body));

        List<SessionItem> result = service.getMySessions(2, "ACTIVE");

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).id);
        assertEquals("Phone", result.get(0).productName);
        assertEquals("ACTIVE", result.get(0).status);

        verify(sellerApiClient).getMySessions(2, "ACTIVE");
    }

    @Test
    void getMySessions_errorResponse_throwsException() throws Exception {
        String body = """
                {
                  "status": 403,
                  "message": "Không có quyền"
                }
                """;

        when(sellerApiClient.getMySessions(2)).thenReturn(response(403, body));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.getMySessions(2)
        );

        assertEquals("Không có quyền", ex.getMessage());
    }

    @Test
    void cancelSession_success_returnsSuccessResult() throws Exception {
        when(sellerApiClient.cancelSession(5, 2))
                .thenReturn(response(200, "{\"status\":200,\"message\":\"Đã hủy phiên\"}"));

        ApiResult<Void> result = service.cancelSession(5, 2);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Đã hủy phiên", result.message);
    }

    @Test
    void cancelSession_successEmptyBody_usesDefaultMessage() throws Exception {
        when(sellerApiClient.cancelSession(5, 2)).thenReturn(response(200, ""));

        ApiResult<Void> result = service.cancelSession(5, 2);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Đã hủy phiên thành công.", result.message);
    }

    private HttpResponse<String> response(int status, String body) {
        return new FakeHttpResponse(status, body);
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