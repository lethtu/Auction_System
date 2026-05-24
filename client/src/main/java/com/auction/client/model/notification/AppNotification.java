package com.auction.client.model.notification;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public class AppNotification {
    private String id;
    private NotificationType type;
    private NotificationSeverity severity;
    private String title;
    private String message;
    private Integer auctionId;
    private String itemName;
    private LocalDateTime createdAt;
    private BooleanProperty read;
    private String actionLabel;
    private Runnable action;
    private boolean soundMuted = false;

    public AppNotification(NotificationType type, NotificationSeverity severity, String title, String message) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.read = new SimpleBooleanProperty(false);
    }

    public boolean isSoundMuted() { return soundMuted; }
    public void setSoundMuted(boolean soundMuted) { this.soundMuted = soundMuted; }

    public String getId() { return id; }
    public NotificationType getType() { return type; }
    public NotificationSeverity getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Integer getAuctionId() { return auctionId; }
    public void setAuctionId(Integer auctionId) { this.auctionId = auctionId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read.get(); }
    public void setRead(boolean read) { this.read.set(read); }
    public BooleanProperty readProperty() { return read; }
    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
    public Runnable getAction() { return action; }
    public void setAction(Runnable action) { this.action = action; }
    public boolean hasAction() { return action != null && actionLabel != null && !actionLabel.isBlank(); }
}
