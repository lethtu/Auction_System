package com.auction.client.service;

import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationSeverity;
import com.auction.client.model.notification.NotificationType;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationCenterServiceTest {

    private NotificationCenterService service;

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @BeforeEach
    void setUp() {
        service = NotificationCenterService.getInstance();
        service.clearAll();
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    void tearDown() {
        service.clearAll();
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void addNotification_prependsNewestAndIncrementsUnreadCount() {
        AppNotification first = notification(NotificationType.NEW_BID, "First");
        AppNotification second = notification(NotificationType.OUTBID, "Second");

        service.addNotification(first);
        WaitForAsyncUtils.waitForFxEvents();
        service.addNotification(second);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(2, service.getNotifications().size());
        assertSame(second, service.getNotifications().get(0));
        assertSame(first, service.getNotifications().get(1));
        assertEquals(2, service.unreadCountProperty().get());
    }

    @Test
    void markAsRead_marksSingleNotificationAndDecrementsUnreadCount() {
        AppNotification notification = notification(NotificationType.BID_SUCCESS, "Won");
        service.addNotification(notification);
        WaitForAsyncUtils.waitForFxEvents();

        service.markAsRead(notification.getId());
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(notification.isRead());
        assertEquals(0, service.unreadCountProperty().get());
    }

    @Test
    void markAllAsRead_marksEveryNotificationAndClearsUnreadCount() {
        AppNotification first = notification(NotificationType.NEW_BID, "First");
        AppNotification second = notification(NotificationType.OUTBID, "Second");
        service.addNotification(first);
        service.addNotification(second);
        WaitForAsyncUtils.waitForFxEvents();

        service.markAllAsRead();
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(first.isRead());
        assertTrue(second.isRead());
        assertEquals(0, service.unreadCountProperty().get());
    }

    @Test
    void addNotification_deduplicatesNonErrorNotificationsWithinWindow() {
        AppNotification first = notification(NotificationType.NEW_BID, "Same auction");
        first.setAuctionId(99);
        AppNotification duplicate = notification(NotificationType.NEW_BID, "Same auction");
        duplicate.setAuctionId(99);

        service.addNotification(first);
        service.addNotification(duplicate);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(1, service.getNotifications().size());
        assertSame(first, service.getNotifications().get(0));
    }

    private AppNotification notification(NotificationType type, String title) {
        return new AppNotification(type, NotificationSeverity.INFO, title, "Message: " + title);
    }
}