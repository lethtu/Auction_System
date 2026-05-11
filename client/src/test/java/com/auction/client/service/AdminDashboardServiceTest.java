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
import static org.mockito.Mockito.*;

class AdminDashboardServiceTest {

    private final AdminApiClient adminApiClient = mock(AdminApiClient.class);
    private final AdminDashboardService service = new AdminDashboardService(adminApiClient);

    @Test
    void getPendingSessions_success_returnsPendingRows() throws Exception {
        String body = """
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
                """;

        when(adminApiClient.getPendingSessions()).thenReturn(response(200, body));

        List<PendingSessionRow> result = service.getPendingSessions();

        assertEquals(1, result.size());
        assertEquals(1, read(result.get(0), "id", "sessionId"));
        assertEquals("Laptop", read(result.get(0), "productName", "product"));
        assertEquals(new BigDecimal("1000000"), read(result.get(0), "startingPrice", "price"));
    }

    @Test
    void getPendingSessions_errorResponse_throwsException() throws Exception {
        String body = """
                {
                  "status": 403,
                  "message": "Không có quyền"
                }
                """;

        when(adminApiClient.getPendingSessions()).thenReturn(response(403, body));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                service::getPendingSessions
        );

        assertEquals("Không có quyền", ex.getMessage());
    }

    @Test
    void getAllSessions_success_returnsSessionRows() throws Exception {
        String body = """
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
                """;

        when(adminApiClient.getAllSessions(null)).thenReturn(response(200, body));

        List<AdminSessionRow> result = service.getAllSessions();

        assertEquals(1, result.size());
        assertEquals(2, read(result.get(0), "id", "sessionId"));
        assertEquals("Phone", read(result.get(0), "productName", "product"));
        assertEquals("seller01", read(result.get(0), "sellerUsername"));
        assertEquals(new BigDecimal("500000"), read(result.get(0), "startingPrice", "price"));
        assertEquals("APPROVED", read(result.get(0), "status"));
    }

    @Test
    void getAllUsers_success_returnsUserRows() throws Exception {
        String body = """
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
                """;

        when(adminApiClient.getAllUsers(null)).thenReturn(response(200, body));

        List<AdminUserRow> result = service.getAllUsers();

        assertEquals(1, result.size());
        assertEquals(3, read(result.get(0), "id", "userId"));
        assertEquals("user01", read(result.get(0), "username"));
        assertEquals("Nguyen Van A", read(result.get(0), "fullname", "fullName"));
        assertEquals("a@gmail.com", read(result.get(0), "email"));
        assertEquals("SELLER", read(result.get(0), "accountType", "role"));
        assertEquals(true, read(result.get(0), "banned"));
    }

    @Test
    void approveSession_success_returnsSuccessResult() throws Exception {
        when(adminApiClient.approveSession(10, 1))
                .thenReturn(response(200, "{\"status\":200,\"message\":\"Đã duyệt\"}"));

        ApiResult<Void> result = service.approveSession(10, 1);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Đã duyệt", result.message);
    }

    @Test
    void rejectSession_error_returnsErrorResult() throws Exception {
        when(adminApiClient.rejectSession(10, 1, "Sai thông tin"))
                .thenReturn(response(400, "{\"status\":400,\"message\":\"Từ chối thất bại\"}"));

        ApiResult<Void> result = service.rejectSession(10, 1, "Sai thông tin");

        assertFalse(result.success);
        assertEquals(400, result.status);
        assertEquals("Từ chối thất bại", result.message);
    }

    @Test
    void banUser_successEmptyBody_usesDefaultMessage() throws Exception {
        when(adminApiClient.banUser(5, 1)).thenReturn(response(200, ""));

        ApiResult<Void> result = service.banUser(5, 1);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Đã khóa tài khoản user.", result.message);
    }

    @Test
    void cancelAuction_success_returnsSuccessResult() throws Exception {
        when(adminApiClient.cancelAuction(7, 1))
                .thenReturn(response(200, "{\"status\":200,\"message\":\"Đã hủy\"}"));

        ApiResult<Void> result = service.cancelAuction(7, 1);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Đã hủy", result.message);
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