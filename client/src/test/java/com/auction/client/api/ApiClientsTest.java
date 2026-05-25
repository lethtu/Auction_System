package com.auction.client.api;

import com.auction.client.util.HttpRequestUtil;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public class ApiClientsTest {

    private MockedStatic<HttpRequestUtil> mockedHttpRequestUtil;
    private HttpResponse<String> mockResponse;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        mockedHttpRequestUtil = mockStatic(HttpRequestUtil.class);
        mockResponse = mock(HttpResponse.class);
    }

    @AfterEach
    public void tearDown() {
        mockedHttpRequestUtil.close();
    }

    @Test
    public void testAdminApiClient() throws Exception {
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.get(anyString(), anyString())).thenReturn(mockResponse);
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.postNoBody(anyString(), anyString())).thenReturn(mockResponse);
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.encode(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminApiClient adminClient = new AdminApiClient();

        assertNotNull(adminClient.getPendingSessions());
        assertNotNull(adminClient.getAllSessions("ACTIVE"));
        assertNotNull(adminClient.getAllSessions(null));
        assertNotNull(adminClient.approveSession(1, 2));
        assertNotNull(adminClient.rejectSession(1, 2, "bad quality"));
        assertNotNull(adminClient.getAllUsers("BIDDER"));
        assertNotNull(adminClient.getAllUsers(null));
        assertNotNull(adminClient.banUser(3, 2));
        assertNotNull(adminClient.restoreUser(3, 2));
        assertNotNull(adminClient.cancelAuction(4, 2));
        assertNotNull(adminClient.hideProduct(5, 2));
        assertNotNull(adminClient.showProduct(5, 2));
    }

    @Test
    public void testSellerApiClient() throws Exception {
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.get(anyString(), anyString())).thenReturn(mockResponse);
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.delete(anyString(), anyString())).thenReturn(mockResponse);
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.sendJson(anyString(), anyString(), anyString(), any(JSONObject.class))).thenReturn(mockResponse);
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.uploadImage(anyString(), anyString(), any(File.class))).thenReturn(mockResponse);
        mockedHttpRequestUtil.when(() -> HttpRequestUtil.encode(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        SellerApiClient sellerClient = new SellerApiClient();
        JSONObject body = new JSONObject();

        assertNotNull(sellerClient.createAuction(body));
        assertNotNull(sellerClient.updateSession(1, 2, body));
        assertNotNull(sellerClient.uploadImage(new File("mock_image.png")));
        assertNotNull(sellerClient.getMySessions(2));
        assertNotNull(sellerClient.getMySessions(2, "PENDING"));
        assertNotNull(sellerClient.cancelSession(1, 2));
    }
}
