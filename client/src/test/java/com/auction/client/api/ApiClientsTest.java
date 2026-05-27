package com.auction.client.api;

import com.auction.client.Config;
import com.auction.client.util.HttpRequestUtil;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public class ApiClientsTest {

    private static final String ADMIN_API = Config.API_URL + "/api/admin";
    private static final String SELLER_API = Config.API_URL + "/api/seller";
    private static final String FILE_API = Config.API_URL + "/api/files";

    private MockedStatic<HttpRequestUtil> mockedHttpRequestUtil;
    private HttpResponse<String> mockResponse;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        mockedHttpRequestUtil = mockStatic(HttpRequestUtil.class);
        mockResponse = mock(HttpResponse.class);

        mockedHttpRequestUtil.when(() -> HttpRequestUtil.get(anyString(), anyString()))
                .thenReturn(mockResponse);
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.postNoBody(anyString(), anyString()))
                .thenReturn(mockResponse);
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.delete(anyString(), anyString()))
                .thenReturn(mockResponse);
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.sendJson(
                        anyString(), anyString(), anyString(), any(JSONObject.class)))
                .thenReturn(mockResponse);
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.uploadImage(
                        anyString(), anyString(), any(File.class)))
                .thenReturn(mockResponse);
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.encode(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    public void tearDown() {
        mockedHttpRequestUtil.close();
    }

    @Test
    public void adminApiClient_shouldUseExpectedEndpointsAndHttpMethods() throws Exception {
        AdminApiClient adminClient = new AdminApiClient();

        assertSame(mockResponse, adminClient.getPendingSessions());
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.get(ADMIN_API, "/pending"));

        assertSame(mockResponse, adminClient.getAllSessions("ACTIVE"));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.get(ADMIN_API, "/sessions?status=ACTIVE"));

        assertSame(mockResponse, adminClient.getAllSessions(null));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.get(ADMIN_API, "/sessions"));

        assertSame(mockResponse, adminClient.approveSession(1, 2));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.postNoBody(ADMIN_API, "/approve/1?adminId=2"));

        assertSame(mockResponse, adminClient.rejectSession(1, 2, " bad quality "));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.postNoBody(
                ADMIN_API, "/reject/1?adminId=2&reason=bad quality"));

        assertSame(mockResponse, adminClient.getAllUsers("BIDDER"));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.get(ADMIN_API, "/users?role=BIDDER"));

        assertSame(mockResponse, adminClient.getAllUsers("   "));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.get(ADMIN_API, "/users"));

        assertSame(mockResponse, adminClient.banUser(3, 2));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.postNoBody(ADMIN_API, "/ban-user/3?adminId=2"));

        assertSame(mockResponse, adminClient.restoreUser(3, 2));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.postNoBody(ADMIN_API, "/restore-user/3?adminId=2"));

        assertSame(mockResponse, adminClient.cancelAuction(4, 2));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.postNoBody(ADMIN_API, "/cancel-auction/4?adminId=2"));

        assertSame(mockResponse, adminClient.hideProduct(5, 2));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.postNoBody(ADMIN_API, "/hide-product/5?adminId=2"));

        assertSame(mockResponse, adminClient.showProduct(5, 2));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.postNoBody(ADMIN_API, "/show-product/5?adminId=2"));
    }

    @Test
    public void sellerApiClient_shouldUseExpectedEndpointsAndHttpMethods() throws Exception {
        SellerApiClient sellerClient = new SellerApiClient();
        JSONObject body = new JSONObject()
                .put("itemName", "Vintage Watch")
                .put("startingPrice", 1000);
        File imageFile = new File("mock_image.png");

        assertSame(mockResponse, sellerClient.createAuction(body));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.sendJson(
                "POST", SELLER_API, "/create-auction", body));

        assertSame(mockResponse, sellerClient.updateSession(1, 2, body));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.sendJson(
                "PUT", SELLER_API, "/update-session/1?sellerId=2", body));

        assertSame(mockResponse, sellerClient.uploadImage(imageFile));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.uploadImage(
                FILE_API, "/images", imageFile));

        assertSame(mockResponse, sellerClient.getMySessions(2));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.get(SELLER_API, "/my-sessions/2"));

        assertSame(mockResponse, sellerClient.getMySessions(2, " PENDING "));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.get(SELLER_API, "/my-sessions/2?status=PENDING"));

        assertSame(mockResponse, sellerClient.cancelSession(1, 2));
        mockedHttpRequestUtil.verify(() -> HttpRequestUtil.delete(SELLER_API, "/cancel-session/1?sellerId=2"));
    }
}
