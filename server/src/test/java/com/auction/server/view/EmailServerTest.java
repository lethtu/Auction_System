package com.auction.server.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServerTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpResponse<String> httpResponse;

    @InjectMocks
    private EmailServer emailServer;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        ReflectionTestUtils.setField(emailServer, "senderEmail", "noreply@auction.com");
        ReflectionTestUtils.setField(emailServer, "senderName", "Auction System");
        ReflectionTestUtils.setField(emailServer, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(emailServer, "httpClient", httpClient);
        ReflectionTestUtils.setField(emailServer, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("Test: Gửi email thành công (Async)")
    public void testSendEmail_Success() throws IOException, InterruptedException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        // Hành động
        emailServer.SendEmail("customer@gmail.com", "Chào mừng", "Nội dung đấu giá");

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        });
    }

    @Test
    @DisplayName("Test: Gửi email thất bại (Lỗi Server mail)")
    public void testSendEmail_Failure() throws IOException, InterruptedException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("Bad Request");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        // Hành động
        emailServer.SendEmail("fail@test.com", "Test", "Body");

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        });
    }
}