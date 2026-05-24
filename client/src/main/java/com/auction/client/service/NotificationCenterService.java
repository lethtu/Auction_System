package com.auction.client.service;

import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationType;
import com.auction.client.model.audio.SoundEvent;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationCenterService {
    private static NotificationCenterService instance;
    private final ObservableList<AppNotification> notifications;
    private final IntegerProperty unreadCount;
    private final Map<String, Long> lastNotifiedMap;
    private final Map<Integer, List<AppNotification>> notificationCacheByUser;
    private volatile Integer activeUserId;
    private static final int MAX_HISTORY_SIZE = 100;
    private static final long DEDUP_WINDOW_MS = 5000;

    private NotificationCenterService() {
        this.notifications = FXCollections.observableArrayList();
        this.unreadCount = new SimpleIntegerProperty(0);
        this.lastNotifiedMap = new ConcurrentHashMap<>();
        this.notificationCacheByUser = new ConcurrentHashMap<>();
        this.activeUserId = null;
    }

    public static synchronized NotificationCenterService getInstance() {
        if (instance == null) {
            instance = new NotificationCenterService();
        }
        return instance;
    }


    public void switchUser(Integer userId) {
        runOnFxThread(() -> {
            saveActiveUserNotifications();
            notifications.clear();
            lastNotifiedMap.clear();
            activeUserId = userId;

            if (userId == null) {
                unreadCount.set(0);
                return;
            }

            List<AppNotification> cached = notificationCacheByUser.get(userId);
            if (cached != null && !cached.isEmpty()) {
                notifications.addAll(cached);
            }
            recomputeUnreadCount();
        });
    }

    private void saveActiveUserNotifications() {
        if (activeUserId == null) {
            return;
        }
        notificationCacheByUser.put(activeUserId, snapshotNotifications());
    }

    private void updateActiveUserCache() {
        if (activeUserId != null) {
            notificationCacheByUser.put(activeUserId, snapshotNotifications());
        }
    }

    private List<AppNotification> snapshotNotifications() {
        int limit = Math.min(notifications.size(), MAX_HISTORY_SIZE);
        return new ArrayList<>(notifications.subList(0, limit));
    }

    private void recomputeUnreadCount() {
        int count = 0;
        for (AppNotification notification : notifications) {
            if (!notification.isRead()) {
                count++;
            }
        }
        unreadCount.set(count);
    }

    private void runOnFxThread(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    public ObservableList<AppNotification> getNotifications() {
        return notifications;
    }

    public IntegerProperty unreadCountProperty() {
        return unreadCount;
    }

    public void addNotification(AppNotification notification) {
        String dedupKey = buildDedupKey(notification);
        long now = System.currentTimeMillis();

        if (dedupKey != null) {
            Long lastTime = lastNotifiedMap.get(dedupKey);
            if (lastTime != null && (now - lastTime) < DEDUP_WINDOW_MS) {
                return; // Duplicate within window
            }
            lastNotifiedMap.put(dedupKey, now);
        }

        Platform.runLater(() -> {
            notifications.add(0, notification);
            if (!notification.isRead()) {
                unreadCount.set(unreadCount.get() + 1);
            }
            // Optional: limit history size
            if (notifications.size() > MAX_HISTORY_SIZE) {
                notifications.remove(MAX_HISTORY_SIZE, notifications.size());
            }
            updateActiveUserCache();
        });
        // Trigger sound (SoundManager internally checks if sound is enabled)
        if (!notification.isSoundMuted()) {
            SoundEvent sEvent = mapToSoundEvent(notification.getType());
            // Play sound with dedup if ENDING_SOON
            if (sEvent == SoundEvent.AUCTION_ENDING_SOON) {
                // If the notification has auctionId
                Integer auctionId = notification.getAuctionId();
                SoundManager.getInstance().playSound(sEvent, auctionId);
            } else {
                SoundManager.getInstance().playSound(sEvent);
            }
        }
    }

    private SoundEvent mapToSoundEvent(NotificationType type) {
        if (type == null) return SoundEvent.NOTIFICATION;
        switch (type) {
            case OUTBID: return SoundEvent.OUTBID;
            case BID_SUCCESS: return SoundEvent.BID_SUCCESS;
            case BID_FAILED: return SoundEvent.BID_ERROR;
            case ENDING_SOON: return SoundEvent.AUCTION_ENDING_SOON;
            case AUCTION_END_WIN:
            case AUCTION_WON: return SoundEvent.AUCTION_WON;
            case AUCTION_END_LOSE:
            case AUCTION_LOST: return SoundEvent.AUCTION_LOST;
            default: return SoundEvent.NOTIFICATION;
        }
    }

    public void markAsRead(String id) {
        Platform.runLater(() -> {
            for (AppNotification notification : notifications) {
                if (notification.getId().equals(id) && !notification.isRead()) {
                    notification.setRead(true);
                    unreadCount.set(Math.max(0, unreadCount.get() - 1));
                    updateActiveUserCache();
                    break;
                }
            }
        });
    }

    public void markAllAsRead() {
        Platform.runLater(() -> {
            for (AppNotification notification : notifications) {
                if (!notification.isRead()) {
                    notification.setRead(true);
                }
            }
            unreadCount.set(0);
            updateActiveUserCache();
        });
    }

    public void clearAll() {
        Platform.runLater(() -> {
            notifications.clear();
            unreadCount.set(0);
            lastNotifiedMap.clear();
            if (activeUserId != null) {
                notificationCacheByUser.remove(activeUserId);
            }
        });
    }

    private String buildDedupKey(AppNotification notification) {
        if (notification.getType() == NotificationType.SYSTEM_ERROR) {
            return null; // Don't dedup errors
        }
        return notification.getType().name() + "_" + 
               (notification.getAuctionId() != null ? notification.getAuctionId() : "none") + "_" + 
               notification.getTitle();
    }
}
