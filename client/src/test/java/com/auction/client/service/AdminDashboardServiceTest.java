package com.auction.client.service;

import com.auction.client.api.AdminApiClient;
import com.auction.client.dto.ApiResult;
import com.auction.client.model.AdminSessionRow;
import com.auction.client.model.AdminUserRow;
import com.auction.client.model.PendingSessionRow;
import javafx.beans.value.ObservableValue;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AdminDashboardServiceTest {

    private final FakeAdminApiClient adminApiClient = new FakeAdminApiClient();
    private final AdminDashboardService service = new AdminDashboardService(adminApiClient);

    @Test
    void getPendingSessions_success_returnsPendingRows() throws Exception {
        adminApiClient.pendingResponse = response(200, """
                {
                  "status": 200,
                  "message": "OK",
                  "data": [
                    {
                      "id": 1,
                      "productName": "Laptop",
                      "startingPrice": 1000000
                    }
                  ]
                }
                """);

        List<PendingSessionRow> result = service.getPendingSessions();

        assertEquals(1, result.size());
        assertEquals(1, read(result.get(0), "id", "sessionId"));
        assertEquals("Laptop", read(result.get(0), "productName", "product"));
        assertEquals(new BigDecimal("1000000"), read(result.get(0), "startingPrice", "price"));
        assertTrue(adminApiClient.getPendingCalled);
    }

    @Test
    void getPendingSessions_errorResponse_throwsException() throws Exception {
        adminApiClient.pendingResponse = response(403, """
                {
                  "status": 403,
                  "message": "Không có quyền"
                }
                """);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                service::getPendingSessions
        );

        assertEquals("Không có quyền", ex.getMessage());
    }

    @Test
    void getAllSessions_success_returnsSessionRows() throws Exception {
        adminApiClient.allSessionsResponse = response(200, """
                {
                  "status": 200,
                  "data": [
                    {
                      "id": 2,
                      "productName": "Phone",
                      "sellerUsername": "seller01",
                      "startingPrice": 500000,
                      "status": "APPROVED"
                    }
                  ]
                }
                """);

        List<AdminSessionRow> result = service.getAllSessions();

        assertEquals(1, result.size());
        assertEquals(2, read(result.get(0), "id", "sessionId"));
        assertEquals("Phone", read(result.get(0), "productName", "product"));
        assertEquals("seller01", read(result.get(0), "sellerUsername"));
        assertEquals(new BigDecimal("500000"), read(result.get(0), "startingPrice", "price"));
        assertEquals("APPROVED", read(result.get(0), "status"));
        assertNull(adminApiClient.lastStatus);
    }

    @Test
    void getAllUsers_success_returnsUserRows() throws Exception {
        adminApiClient.usersResponse = response(200, """
                {
                  "status": 200,
                  "data": [
                    {
                      "id": 3,
                      "username": "user01",
                      "fullname": "Nguyen Van A",
                      "email": "a@gmail.com",
                      "accountType": "SELLER",
                      "banned": true
                    }
                  ]
                }
                """);

        List<AdminUserRow> result = service.getAllUsers();

        assertEquals(1, result.size());
        assertEquals(3, read(result.get(0), "id", "userId"));
        assertEquals("user01", read(result.get(0), "username"));
        assertEquals("Nguyen Van A", read(result.get(0), "fullname", "fullName"));
        assertEquals("a@gmail.com", read(result.get(0), "email"));
        assertEquals("SELLER", read(result.get(0), "accountType", "role"));
        assertEquals(true, read(result.get(0), "banned"));
        assertNull(adminApiClient.lastRole);
    }

    @Test
    void approveSession_success_returnsSuccessResult() throws Exception {
        adminApiClient.approveResponse = response(200, "{\"status\":200,\"message\":\"Đã duyệt\"}");

        ApiResult<Void> result = service.approveSession(10, 1);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Đã duyệt", result.message);
        assertEquals(10, adminApiClient.lastSessionId);
        assertEquals(1, adminApiClient.lastAdminId);
    }

    @Test
    void rejectSession_error_returnsErrorResult() throws Exception {
        adminApiClient.rejectResponse = response(400, "{\"status\":400,\"message\":\"Từ chối thất bại\"}");

        ApiResult<Void> result = service.rejectSession(10, 1, "Sai thông tin");

        assertFalse(result.success);
        assertEquals(400, result.status);
        assertEquals("Từ chối thất bại", result.message);
        assertEquals("Sai thông tin", adminApiClient.lastReason);
    }

    @Test
    void banUser_successEmptyBody_usesDefaultMessage() throws Exception {
        adminApiClient.banResponse = response(200, "");

        ApiResult<Void> result = service.banUser(5, 1);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("User account banned.", result.message);
        assertEquals(5, adminApiClient.lastUserId);
        assertEquals(1, adminApiClient.lastAdminId);
    }

    @Test
    void cancelAuction_success_returnsSuccessResult() throws Exception {
        adminApiClient.cancelResponse = response(200, "{\"status\":200,\"message\":\"Đã hủy\"}");

        ApiResult<Void> result = service.cancelAuction(7, 1);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Đã hủy", result.message);
        assertEquals(7, adminApiClient.lastSessionId);
        assertEquals(1, adminApiClient.lastAdminId);
    }

    private HttpResponse<String> response(int status, String body) {
        return new FakeHttpResponse(status, body);
    }

    private Object read(Object target, String... names) {
        for (String name : names) {
            Object value = tryRead(target, name);
            if (value != null) {
                return value;
            }
        }

        fail("Không đọc được field/property: " + String.join(", ", names));
        return null;
    }

    private Object tryRead(Object target, String name) {
        String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);

        String[] methodNames = {
                name,
                name + "Property",
                "get" + capitalized,
                "is" + capitalized
        };

        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);

                if (value instanceof ObservableValue<?> observableValue) {
                    return observableValue.getValue();
                }

                return value;
            } catch (Exception ignored) {
            }
        }

        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(target);

            if (value instanceof ObservableValue<?> observableValue) {
                return observableValue.getValue();
            }

            return value;
        } catch (Exception ignored) {
        }

        return null;
    }

    private static class FakeAdminApiClient extends AdminApiClient {
        private HttpResponse<String> pendingResponse;
        private HttpResponse<String> allSessionsResponse;
        private HttpResponse<String> usersResponse;
        private HttpResponse<String> approveResponse;
        private HttpResponse<String> rejectResponse;
        private HttpResponse<String> banResponse;
        private HttpResponse<String> cancelResponse;

        private boolean getPendingCalled;

        private Integer lastSessionId;
        private Integer lastAdminId;
        private Integer lastUserId;
        private String lastReason;
        private String lastStatus;
        private String lastRole;

        @Override
        public HttpResponse<String> getPendingSessions() {
            getPendingCalled = true;
            return pendingResponse;
        }

        @Override
        public HttpResponse<String> getAllSessions(String status) {
            lastStatus = status;
            return allSessionsResponse;
        }

        @Override
        public HttpResponse<String> approveSession(int sessionId, int adminId) {
            lastSessionId = sessionId;
            lastAdminId = adminId;
            return approveResponse;
        }

        @Override
        public HttpResponse<String> rejectSession(int sessionId, int adminId, String reason) {
            lastSessionId = sessionId;
            lastAdminId = adminId;
            lastReason = reason;
            return rejectResponse;
        }

        @Override
        public HttpResponse<String> getAllUsers(String role) {
            lastRole = role;
            return usersResponse;
        }

        @Override
        public HttpResponse<String> banUser(int userId, int adminId) {
            lastUserId = userId;
            lastAdminId = adminId;
            return banResponse;
        }

        @Override
        public HttpResponse<String> cancelAuction(int sessionId, int adminId) {
            lastSessionId = sessionId;
            lastAdminId = adminId;
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