package com.auction.client.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class GoogleOAuthServiceTest {

    private GoogleOAuthService oauthService;
    private HttpClient httpClient;

    @BeforeEach
    public void setUp() {
        oauthService = new GoogleOAuthService();
        httpClient = HttpClient.newHttpClient();
    }

    @AfterEach
    public void tearDown() {
        oauthService.stopServer();
    }

    @Test
    public void testAuthorizationFlow_Success() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> successCode = new AtomicReference<>();
        AtomicReference<String> successUri = new AtomicReference<>();

        oauthService.startAuthorizationFlow("mockClientId", new GoogleOAuthService.AuthorizationCallback() {
            @Override
            public void onSuccess(String code, String redirectUri) {
                successCode.set(code);
                successUri.set(redirectUri);
                latch.countDown();
            }

            @Override
            public void onFailure(String error) {
                latch.countDown();
            }
        });

        // Use reflection to get port
        Field serverField = GoogleOAuthService.class.getDeclaredField("server");
        serverField.setAccessible(true);
        com.sun.net.httpserver.HttpServer server = (com.sun.net.httpserver.HttpServer) serverField.get(oauthService);
        assertNotNull(server);
        int port = server.getAddress().getPort();

        // Simulate Google redirecting back with auth code
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/callback?code=mock_auth_code_123"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Login Successful"));

        // Wait for callback thread
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("mock_auth_code_123", successCode.get());
        assertNotNull(successUri.get());
    }

    @Test
    public void testAuthorizationFlow_Failure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> failureError = new AtomicReference<>();

        oauthService.startAuthorizationFlow("mockClientId", new GoogleOAuthService.AuthorizationCallback() {
            @Override
            public void onSuccess(String code, String redirectUri) {
                latch.countDown();
            }

            @Override
            public void onFailure(String error) {
                failureError.set(error);
                latch.countDown();
            }
        });

        // Use reflection to get port
        Field serverField = GoogleOAuthService.class.getDeclaredField("server");
        serverField.setAccessible(true);
        com.sun.net.httpserver.HttpServer server = (com.sun.net.httpserver.HttpServer) serverField.get(oauthService);
        assertNotNull(server);
        int port = server.getAddress().getPort();

        // Simulate Google redirecting back with error
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/callback?error=access_denied"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Login Failed"));

        // Wait for callback thread
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("access_denied", failureError.get());
    }
    @Test
    public void testAuthorizationFlow_EncodedCodeWithEqualsAndSlash() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> successCode = new AtomicReference<>();

        oauthService.startAuthorizationFlow("mockClientId", new GoogleOAuthService.AuthorizationCallback() {
            @Override
            public void onSuccess(String code, String redirectUri) {
                successCode.set(code);
                latch.countDown();
            }

            @Override
            public void onFailure(String error) {
                latch.countDown();
            }
        });

        Field serverField = GoogleOAuthService.class.getDeclaredField("server");
        serverField.setAccessible(true);
        com.sun.net.httpserver.HttpServer server = (com.sun.net.httpserver.HttpServer) serverField.get(oauthService);
        assertNotNull(server);
        int port = server.getAddress().getPort();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/callback?code=mock%2Bauth%3Dcode%252F123"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Login Successful"));

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("mock+auth=code%2F123", successCode.get());
    }

}
