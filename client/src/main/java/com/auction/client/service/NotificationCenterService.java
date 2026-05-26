package com.auction.client.service;

import com.auction.client.Config;
import com.auction.client.model.User;
import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationType;
import com.auction.client.model.audio.SoundEvent;
import com.auction.client.model.notification.NotificationSeverity;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationCenterService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationCenterService.class);
    private static NotificationCenterService instance;
    private final ObservableList<AppNotification> notifications;
    private final IntegerProperty unreadCount;
    private final Map<String, Long> lastNotifiedMap;
    private final Map<Integer, JSONObject> lastAuctionSnapshot;
    private HttpClient httpClient;
    private static final long DEDUP_WINDOW_MS = 5000;

    private NotificationCenterService() {
        this.notifications = FXCollections.observableArrayList();
        this.unreadCount = new SimpleIntegerProperty(0);
        this.lastNotifiedMap = new ConcurrentHashMap<>();
        this.lastAuctionSnapshot = new ConcurrentHashMap<>();
        this.httpClient = HttpClient.newHttpClient();
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
        logger.info("addNotification: type={}, severity={}, auctionId={}, title={}",
                notification.getType(), notification.getSeverity(), notification.getAuctionId(), notification.getTitle());
        if (!shouldDisplayNotification(notification)) {
            logger.info("Notification suppressed by display rules: type={}, auctionId={}",
                    notification.getType(), notification.getAuctionId());
            return;
        }

        String dedupKey = buildDedupKey(notification);
        long now = System.currentTimeMillis();

        if (dedupKey != null) {
            Long lastTime = lastNotifiedMap.get(dedupKey);
            if (lastTime != null && (now - lastTime) < DEDUP_WINDOW_MS) {
                logger.info("Notification suppressed by dedup: key={}", dedupKey);
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

    public void setHttpClient(HttpClient httpClient) {
        if (httpClient != null) {
            this.httpClient = httpClient;
        }
    }

    public void pollNotifications() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/auctions/all"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                return;
            }

            JSONArray sessions = extractAuctionArray(response.body());
            Map<Integer, JSONObject> latest = new HashMap<>();
            for (int i = 0; i < sessions.length(); i++) {
                JSONObject current = sessions.getJSONObject(i);
                int auctionId = current.optInt("id", -1);
                if (auctionId == -1) {
                    continue;
                }
                latest.put(auctionId, current);
                JSONObject previous = lastAuctionSnapshot.get(auctionId);
                if (previous != null) {
                    maybeNotifyOutbid(previous, current);
                }
            }

            lastAuctionSnapshot.clear();
            lastAuctionSnapshot.putAll(latest);
        } catch (Exception ignored) {
            // Polling is best-effort; socket notifications remain the primary path.
        }
    }

    private JSONArray extractAuctionArray(String responseBody) {
        JSONObject root = new JSONObject(responseBody);
        Object data = root.opt("data");
        if (data instanceof JSONArray array) {
            return array;
        }
        if (data instanceof JSONObject object) {
            JSONArray content = object.optJSONArray("content");
            return content == null ? new JSONArray() : content;
        }
        return new JSONArray();
    }

    private void maybeNotifyOutbid(JSONObject previous, JSONObject current) {
        Integer currentUserId = User.getId();
        if (currentUserId == null || !hasUserBid(previous, currentUserId)) {
            return;
        }

        Integer oldHighest = optionalInt(previous, "highestBidderId");
        Integer newHighest = optionalInt(current, "highestBidderId");
        if (newHighest == null || currentUserId.equals(newHighest)) {
            return;
        }

        BigDecimal oldPrice = parseMoney(previous.opt("currentPrice"));
        BigDecimal newPrice = parseMoney(current.opt("currentPrice"));
        boolean wasHighest = currentUserId.equals(oldHighest);
        if (!wasHighest || newPrice.compareTo(oldPrice) <= 0) {
            return;
        }

        String itemName = current.optString("productName", "Unknown Item");
        AppNotification notification = new AppNotification(
                NotificationType.OUTBID,
                NotificationSeverity.WARNING,
                "You have been outbid",
                "Product " + itemName + " is now at ₫ " + formatPrice(newPrice));
        notification.setAuctionId(current.optInt("id", -1));
        notification.setItemName(itemName);
        addNotification(notification);
    }

    private boolean hasUserBid(JSONObject session, int userId) {
        JSONArray bids = session.optJSONArray("bids");
        if (bids == null) {
            return false;
        }
        for (int i = 0; i < bids.length(); i++) {
            JSONObject bid = bids.optJSONObject(i);
            if (bid == null) {
                continue;
            }
            int bidderId = bid.has("userId") ? bid.optInt("userId", -1) : bid.optInt("bidderId", -1);
            if (bidderId == userId) {
                return true;
            }
        }
        return false;
    }

    private Integer optionalInt(JSONObject object, String key) {
        return object.has(key) && !object.isNull(key) ? object.optInt(key) : null;
    }

    private BigDecimal parseMoney(Object value) {
        if (value == null || JSONObject.NULL.equals(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String formatPrice(BigDecimal price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("###,###", symbols).format(price == null ? BigDecimal.ZERO : price);
    }

    private boolean shouldDisplayNotification(AppNotification notification) {
        if (notification == null) {
            return false;
        }

        SettingsService settings = SettingsService.getInstance();
        if (!settings.isNotificationsEnabled()) {
            return false;
        }

        NotificationType type = notification.getType();
        if (type == NotificationType.OUTBID) {
            return settings.isOutbidNotificationEnabled();
        }
        if (type == NotificationType.ENDING_SOON) {
            return settings.isEndingSoonNotificationEnabled();
        }
        if (type == NotificationType.AUCTION_END_WIN
                || type == NotificationType.AUCTION_WON
                || type == NotificationType.AUCTION_END_LOSE
                || type == NotificationType.AUCTION_LOST) {
            return settings.isAuctionResultNotificationEnabled();
        }

        return true;
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
            lastAuctionSnapshot.clear();
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
