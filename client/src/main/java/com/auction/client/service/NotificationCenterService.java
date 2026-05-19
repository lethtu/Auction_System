package com.auction.client.service;

import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationType;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationCenterService {
    private static NotificationCenterService instance;
    private final ObservableList<AppNotification> notifications;
    private final IntegerProperty unreadCount;
    private final Map<String, Long> lastNotifiedMap;
    private static final long DEDUP_WINDOW_MS = 5000;

    private NotificationCenterService() {
        this.notifications = FXCollections.observableArrayList();
        this.unreadCount = new SimpleIntegerProperty(0);
        this.lastNotifiedMap = new ConcurrentHashMap<>();
    }

    public static synchronized NotificationCenterService getInstance() {
        if (instance == null) {
            instance = new NotificationCenterService();
        }
        return instance;
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
            if (notifications.size() > 100) {
                notifications.remove(100, notifications.size());
            }
        });
    }

    public void markAsRead(String id) {
        Platform.runLater(() -> {
            for (AppNotification notification : notifications) {
                if (notification.getId().equals(id) && !notification.isRead()) {
                    notification.setRead(true);
                    unreadCount.set(Math.max(0, unreadCount.get() - 1));
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
        });
    }

    public void clearAll() {
        Platform.runLater(() -> {
            notifications.clear();
            unreadCount.set(0);
            lastNotifiedMap.clear();
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
