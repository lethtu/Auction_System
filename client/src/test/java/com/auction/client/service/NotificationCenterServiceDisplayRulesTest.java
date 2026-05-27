package com.auction.client.service;

import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationSeverity;
import com.auction.client.model.notification.NotificationType;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class NotificationCenterServiceDisplayRulesTest {

    private NotificationCenterService service;
    private SettingsService settings;

    private boolean previousNotificationsEnabled;
    private boolean previousOutbidEnabled;
    private boolean previousEndingSoonEnabled;
    private boolean previousAuctionResultEnabled;
    private boolean previousSoundEnabled;

    @BeforeEach
    void setUp() throws Exception {
        service = NotificationCenterService.getInstance();
        settings = SettingsService.getInstance();

        previousNotificationsEnabled = settings.isNotificationsEnabled();
        previousOutbidEnabled = settings.isOutbidNotificationEnabled();
        previousEndingSoonEnabled = settings.isEndingSoonNotificationEnabled();
        previousAuctionResultEnabled = settings.isAuctionResultNotificationEnabled();
        previousSoundEnabled = settings.isSoundEnabled();

        settings.setNotificationsEnabled(true);
        settings.setOutbidNotificationEnabled(true);
        settings.setEndingSoonNotificationEnabled(true);
        settings.setAuctionResultNotificationEnabled(true);
        settings.setSoundEnabled(false);
        settings.flush();

        service.clearAll();
        waitFxEvents();
    }

    @AfterEach
    void tearDown() throws Exception {
        service.clearAll();
        waitFxEvents();

        settings.setNotificationsEnabled(previousNotificationsEnabled);
        settings.setOutbidNotificationEnabled(previousOutbidEnabled);
        settings.setEndingSoonNotificationEnabled(previousEndingSoonEnabled);
        settings.setAuctionResultNotificationEnabled(previousAuctionResultEnabled);
        settings.setSoundEnabled(previousSoundEnabled);
        settings.flush();
    }

    @Test
    void addNotification_suppressesAllNotificationsWhenGlobalSettingIsDisabled() throws Exception {
        settings.setNotificationsEnabled(false);

        service.addNotification(mutedNotification(NotificationType.GENERAL, "General notice", 11));
        waitFxEvents();

        assertEquals(0, service.getNotifications().size());
        assertEquals(0, service.unreadCountProperty().get());
    }

    @Test
    void addNotification_respectsTypeSpecificNotificationToggles() throws Exception {
        settings.setOutbidNotificationEnabled(false);
        service.addNotification(mutedNotification(NotificationType.OUTBID, "Outbid", 21));
        waitFxEvents();
        assertEquals(0, service.getNotifications().size());

        settings.setOutbidNotificationEnabled(true);
        settings.setEndingSoonNotificationEnabled(false);
        service.addNotification(mutedNotification(NotificationType.ENDING_SOON, "Ending soon", 22));
        waitFxEvents();
        assertEquals(0, service.getNotifications().size());

        settings.setEndingSoonNotificationEnabled(true);
        settings.setAuctionResultNotificationEnabled(false);
        service.addNotification(mutedNotification(NotificationType.AUCTION_END_WIN, "Win old type", 23));
        service.addNotification(mutedNotification(NotificationType.AUCTION_WON, "Win new type", 24));
        service.addNotification(mutedNotification(NotificationType.AUCTION_END_LOSE, "Lose old type", 25));
        service.addNotification(mutedNotification(NotificationType.AUCTION_LOST, "Lose new type", 26));
        waitFxEvents();

        assertEquals(0, service.getNotifications().size());
        assertEquals(0, service.unreadCountProperty().get());
    }

    @Test
    void addNotification_deduplicatesSameTypeAuctionAndTitleWithinWindow() throws Exception {
        AppNotification first = mutedNotification(NotificationType.GENERAL, "Duplicate title", 31);
        AppNotification second = mutedNotification(NotificationType.GENERAL, "Duplicate title", 31);

        service.addNotification(first);
        service.addNotification(second);
        waitFxEvents();

        assertEquals(1, service.getNotifications().size());
        assertEquals(1, service.unreadCountProperty().get());
    }

    @Test
    void markAsRead_markAllAsReadAndClearAllUpdateUnreadState() throws Exception {
        AppNotification first = mutedNotification(NotificationType.GENERAL, "First", 41);
        AppNotification second = mutedNotification(NotificationType.BID_SUCCESS, "Second", 42);

        service.addNotification(first);
        service.addNotification(second);
        waitFxEvents();

        assertEquals(2, service.getNotifications().size());
        assertEquals(2, service.unreadCountProperty().get());

        service.markAsRead(first.getId());
        waitFxEvents();
        assertTrue(first.isRead());
        assertEquals(1, service.unreadCountProperty().get());

        service.markAllAsRead();
        waitFxEvents();
        assertTrue(second.isRead());
        assertEquals(0, service.unreadCountProperty().get());

        service.clearAll();
        waitFxEvents();
        assertEquals(0, service.getNotifications().size());
        assertEquals(0, service.unreadCountProperty().get());
    }

    private AppNotification mutedNotification(NotificationType type, String title, int auctionId) {
        AppNotification notification = new AppNotification(type, NotificationSeverity.INFO, title, "Message for " + title);
        notification.setAuctionId(auctionId);
        notification.setSoundMuted(true);
        return notification;
    }

    private void waitFxEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX events");
    }
}
