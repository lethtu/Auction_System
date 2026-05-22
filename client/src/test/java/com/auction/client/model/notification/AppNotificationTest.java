package com.auction.client.model.notification;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppNotificationTest {

    @Test
    void constructor_setsDefaultsAndStoresContent() {
        AppNotification notification = new AppNotification(
                NotificationType.NEW_BID,
                NotificationSeverity.INFO,
                "New bid",
                "A bidder placed a new bid");

        assertNotNull(notification.getId());
        assertEquals(NotificationType.NEW_BID, notification.getType());
        assertEquals(NotificationSeverity.INFO, notification.getSeverity());
        assertEquals("New bid", notification.getTitle());
        assertEquals("A bidder placed a new bid", notification.getMessage());
        assertNotNull(notification.getCreatedAt());
        assertFalse(notification.isRead());
    }

    @Test
    void setters_updateOptionalFieldsAndReadProperty() {
        AppNotification notification = new AppNotification(
                NotificationType.AUCTION_EXTENDED,
                NotificationSeverity.WARNING,
                "Extended",
                "Auction was extended");
        AtomicBoolean listenerObservedRead = new AtomicBoolean(false);
        notification.readProperty().addListener((obs, oldValue, newValue) -> listenerObservedRead.set(newValue));

        notification.setAuctionId(10);
        notification.setItemName("Laptop");
        notification.setActionLabel("View auction");
        notification.setRead(true);

        assertEquals(10, notification.getAuctionId());
        assertEquals("Laptop", notification.getItemName());
        assertEquals("View auction", notification.getActionLabel());
        assertTrue(notification.isRead());
        assertTrue(listenerObservedRead.get());
    }
}