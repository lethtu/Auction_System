package com.auction.client.service;

import com.auction.client.model.User;
import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationType;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class NotificationCenterServiceTest {

    private HttpClient mockHttpClient;
    private NotificationCenterService service;

    @BeforeEach
    public void setUp() {
        mockHttpClient = mock(HttpClient.class);
        service = NotificationCenterService.getInstance();
        service.clearAll();
        service.setHttpClient(mockHttpClient);
        User.clearSession();
    }

    @Test
    public void testOutbidNotificationTriggered() throws Exception {
        // Set up active user
        User.setSession(1, "user1", "User One", "user1@example.com", "2000-01-01", "Hanoi", "BUYER", null);
        
        // Initial state - we have bid 100,000 on auction 10
        String initialJson = """
        {
            "status": 200,
            "data": [
                {
                    "id": 10,
                    "currentPrice": 100000,
                    "status": "ACTIVE",
                    "highestBidderId": 1,
                    "productName": "Awesome Item",
                    "bids": [
                        {
                            "userId": 1,
                            "amount": 100000
                        }
                    ]
                }
            ]
        }
        """;
        
        HttpResponse<String> mockResponse1 = mock(HttpResponse.class);
        when(mockResponse1.statusCode()).thenReturn(200);
        when(mockResponse1.body()).thenReturn(initialJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse1);
            
        // First poll to build the initial snapshot
        service.pollNotifications();
        waitFxEvents();
        assertEquals(0, service.getNotifications().size(), "Should have no notifications initially");

        // Now someone outbids us - currentPrice becomes 150,000 and highestBidderId is 2
        String updatedJson = """
        {
            "status": 200,
            "data": [
                {
                    "id": 10,
                    "currentPrice": 150000,
                    "status": "ACTIVE",
                    "highestBidderId": 2,
                    "productName": "Awesome Item",
                    "bids": [
                        {
                            "userId": 1,
                            "amount": 100000
                        },
                        {
                            "userId": 2,
                            "amount": 150000
                        }
                    ]
                }
            ]
        }
        """;
        
        HttpResponse<String> mockResponse2 = mock(HttpResponse.class);
        when(mockResponse2.statusCode()).thenReturn(200);
        when(mockResponse2.body()).thenReturn(updatedJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse2);

        // Second poll to detect changes
        service.pollNotifications();
        waitFxEvents();

        assertEquals(1, service.getNotifications().size(), "Should receive 1 notification");
        AppNotification notification = service.getNotifications().get(0);
        assertEquals(NotificationType.OUTBID, notification.getType());
        assertTrue(notification.getMessage().contains("Awesome Item"));
        assertTrue(notification.getMessage().contains("150.000"));
    }

    @Test
    public void testWatchlistNoNotificationForNonBidder() throws Exception {
        // Set up active user
        User.setSession(1, "user1", "User One", "user1@example.com", "2000-01-01", "Hanoi", "BUYER", null);
        User.watchlistIds.add(20); // Add auction 20 to watchlist
 
        // Initial state - price is 200,000 on auction 20, we haven't bid
        String initialJson = """
        {
            "status": 200,
            "data": [
                {
                    "id": 20,
                    "currentPrice": 200000,
                    "status": "ACTIVE",
                    "highestBidderId": 3,
                    "productName": "Watchlist Item",
                    "bids": [
                        {
                            "userId": 3,
                            "amount": 200000
                        }
                    ]
                }
            ]
        }
        """;
        
        HttpResponse<String> mockResponse1 = mock(HttpResponse.class);
        when(mockResponse1.statusCode()).thenReturn(200);
        when(mockResponse1.body()).thenReturn(initialJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse1);
            
        service.pollNotifications();
        waitFxEvents();
        assertEquals(0, service.getNotifications().size());
 
        // Now someone else bids 220,000 on auction 20
        String updatedJson = """
        {
            "status": 200,
            "data": [
                {
                    "id": 20,
                    "currentPrice": 220000,
                    "status": "ACTIVE",
                    "highestBidderId": 4,
                    "productName": "Watchlist Item",
                    "bids": [
                        {
                            "userId": 3,
                            "amount": 200000
                        },
                        {
                            "userId": 4,
                            "amount": 220000
                        }
                    ]
                }
            ]
        }
        """;
        
        HttpResponse<String> mockResponse2 = mock(HttpResponse.class);
        when(mockResponse2.statusCode()).thenReturn(200);
        when(mockResponse2.body()).thenReturn(updatedJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse2);
 
        service.pollNotifications();
        waitFxEvents();
 
        // Watchers who have not bid should NOT receive notifications
        assertEquals(0, service.getNotifications().size(), "Non-bidder watchers should not receive new bid notifications");
    }
 
    @Test
    public void testNoNotificationOnOwnBid() throws Exception {
        // Set up active user (ID 1)
        User.setSession(1, "user1", "User One", "user1@example.com", "2000-01-01", "Hanoi", "BUYER", null);
 
        // Initial state - price is 100,000 on auction 30, bid by user 2
        String initialJson = """
        {
            "status": 200,
            "data": [
                {
                    "id": 30,
                    "currentPrice": 100000,
                    "status": "ACTIVE",
                    "highestBidderId": 2,
                    "productName": "Watchlist Item",
                    "bids": [
                        {
                            "userId": 2,
                            "amount": 100000
                        }
                    ]
                }
            ]
        }
        """;
        
        HttpResponse<String> mockResponse1 = mock(HttpResponse.class);
        when(mockResponse1.statusCode()).thenReturn(200);
        when(mockResponse1.body()).thenReturn(initialJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse1);
            
        service.pollNotifications();
        waitFxEvents();
        assertEquals(0, service.getNotifications().size());
 
        // Now user 1 (the current user) bids 120,000 on auction 30 (making them the highest bidder)
        String updatedJson = """
        {
            "status": 200,
            "data": [
                {
                    "id": 30,
                    "currentPrice": 120000,
                    "status": "ACTIVE",
                    "highestBidderId": 1,
                    "productName": "Watchlist Item",
                    "bids": [
                        {
                            "userId": 2,
                            "amount": 100000
                        },
                        {
                            "userId": 1,
                            "amount": 120000
                        }
                    ]
                }
            ]
        }
        """;
        
        HttpResponse<String> mockResponse2 = mock(HttpResponse.class);
        when(mockResponse2.statusCode()).thenReturn(200);
        when(mockResponse2.body()).thenReturn(updatedJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse2);
 
        service.pollNotifications();
        waitFxEvents();
 
        // The user should NOT receive any notification for their own bid!
        assertEquals(0, service.getNotifications().size(), "Should not receive a notification on own bid");
    }

    private void waitFxEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX events");
    }
}
