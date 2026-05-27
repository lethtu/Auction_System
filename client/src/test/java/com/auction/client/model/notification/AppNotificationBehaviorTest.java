package com.auction.client.model.notification;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppNotificationBehaviorTest {

    @Test
    void constructorInitializesDefaultsAndReadPropertyTracksState() {
        AppNotification notification = new AppNotification(
                NotificationType.GENERAL,
                NotificationSeverity.INFO,
                "Welcome",
                "Message"
        );

        assertNotNull(notification.getId());
        assertFalse(notification.getId().isBlank());
        assertEquals(NotificationType.GENERAL, notification.getType());
        assertEquals(NotificationSeverity.INFO, notification.getSeverity());
        assertEquals("Welcome", notification.getTitle());
        assertEquals("Message", notification.getMessage());
        assertNotNull(notification.getCreatedAt());
        assertNotNull(notification.readProperty());
        assertFalse(notification.isRead());
        assertFalse(notification.isSoundMuted());

        notification.setRead(true);
        assertTrue(notification.isRead());
        assertTrue(notification.readProperty().get());

        notification.setSoundMuted(true);
        assertTrue(notification.isSoundMuted());
    }

    @Test
    void hasActionRequiresRunnableAndNonBlankLabel() {
        AppNotification notification = new AppNotification(
                NotificationType.BID_SUCCESS,
                NotificationSeverity.SUCCESS,
                "Bid accepted",
                "Your bid was accepted"
        );
        AtomicInteger actionCalls = new AtomicInteger();

        assertFalse(notification.hasAction());

        notification.setActionLabel("Open auction");
        assertFalse(notification.hasAction(), "Label alone is not enough without action runnable.");

        notification.setAction(actionCalls::incrementAndGet);
        assertTrue(notification.hasAction());
        notification.getAction().run();
        assertEquals(1, actionCalls.get());

        notification.setActionLabel("   ");
        assertFalse(notification.hasAction(), "Blank labels should not be treated as actionable.");

        notification.setActionLabel(null);
        assertFalse(notification.hasAction(), "Null labels should not be treated as actionable.");

        notification.setActionLabel("View details");
        assertTrue(notification.hasAction());
    }

    @Test
    void settersStoreOptionalAuctionAndItemData() {
        AppNotification notification = new AppNotification(
                NotificationType.OUTBID,
                NotificationSeverity.WARNING,
                "Outbid",
                "Someone placed a higher bid"
        );

        notification.setAuctionId(42);
        notification.setItemName("Vintage camera");

        assertEquals(42, notification.getAuctionId());
        assertEquals("Vintage camera", notification.getItemName());
    }
}
