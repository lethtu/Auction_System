package com.auction.client.controller;

import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationType;
import com.auction.client.service.NotificationCenterService;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationCenterController {
    @FXML private Label lblUnreadCount;
    @FXML private VBox listContainer;
    @FXML private Button tabAll, tabUnread, tabAuction, tabSystem;

    private NotificationCenterService service = NotificationCenterService.getInstance();
    private String currentFilter = "ALL";

    @FXML
    public void initialize() {
        lblUnreadCount.textProperty().bind(service.unreadCountProperty().asString().concat(" unread"));
        
        service.getNotifications().addListener((ListChangeListener<AppNotification>) c -> {
            Platform.runLater(this::renderList);
        });
        
        renderList();
    }

    @FXML
    private void handleMarkAllRead(ActionEvent event) {
        service.markAllAsRead();
        renderList();
    }

    @FXML private void filterAll(ActionEvent event) { setFilter("ALL", tabAll); }
    @FXML private void filterUnread(ActionEvent event) { setFilter("UNREAD", tabUnread); }
    @FXML private void filterAuction(ActionEvent event) { setFilter("AUCTION", tabAuction); }
    @FXML private void filterSystem(ActionEvent event) { setFilter("SYSTEM", tabSystem); }

    private void setFilter(String filter, Button activeTab) {
        this.currentFilter = filter;
        
        tabAll.setStyle(getInactiveTabStyle());
        tabUnread.setStyle(getInactiveTabStyle());
        tabAuction.setStyle(getInactiveTabStyle());
        tabSystem.setStyle(getInactiveTabStyle());
        
        activeTab.setStyle(getActiveTabStyle());
        
        renderList();
    }

    private String getActiveTabStyle() {
        return "-fx-background-color: #e040a0; -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 6 16; -fx-cursor: hand;";
    }
    
    private String getInactiveTabStyle() {
        return "-fx-background-color: #f2e8f2; -fx-text-fill: #604868; -fx-font-family: 'DM Sans'; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 6 16; -fx-cursor: hand;";
    }

    private void renderList() {
        listContainer.getChildren().clear();
        
        List<AppNotification> filtered = service.getNotifications().stream().filter(n -> {
            if ("UNREAD".equals(currentFilter)) return !n.isRead();
            if ("AUCTION".equals(currentFilter)) return n.getType() == NotificationType.NEW_BID || n.getType() == NotificationType.OUTBID || n.getType() == NotificationType.BID_SUCCESS || n.getType() == NotificationType.AUCTION_END_WIN || n.getType() == NotificationType.AUCTION_END_LOSE;
            if ("SYSTEM".equals(currentFilter)) return n.getType() == NotificationType.SYSTEM_ERROR;
            return true;
        }).collect(Collectors.toList());

        if (filtered.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(50, 0, 0, 0));
            Label icon = new Label("notifications_off");
            icon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 48px; -fx-text-fill: #dcc8e0;");
            Label msg = new Label("No notifications here");
            msg.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #907898;");
            emptyState.getChildren().addAll(icon, msg);
            listContainer.getChildren().add(emptyState);
            return;
        }

        for (AppNotification n : filtered) {
            listContainer.getChildren().add(createNotificationCard(n));
        }
    }

    private HBox createNotificationCard(AppNotification n) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        String bgColor = n.isRead() ? "#ffffff" : "#fff0f8";
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; -fx-border-color: #f2e8f2; -fx-border-radius: 12; -fx-padding: 15;");
        
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #fbf2fb; -fx-background-radius: 12; -fx-border-color: #f2e8f2; -fx-border-radius: 12; -fx-padding: 15; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: " + (n.isRead() ? "#ffffff" : "#fff0f8") + "; -fx-background-radius: 12; -fx-border-color: #f2e8f2; -fx-border-radius: 12; -fx-padding: 15;"));
        card.setOnMouseClicked(e -> {
            service.markAsRead(n.getId());
        });

        Label icon = new Label(getIconForType(n.getType()));
        icon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 24px; -fx-text-fill: " + getColorForSeverity(n.getSeverity()) + "; -fx-background-color: #fef7ff; -fx-background-radius: 50%; -fx-padding: 10;");

        VBox textCol = new VBox(5);
        HBox.setHgrow(textCol, Priority.ALWAYS);
        Label title = new Label(n.getTitle());
        title.setStyle("-fx-font-family: 'DM Sans'; -fx-font-weight: " + (n.isRead() ? "bold" : "900") + "; -fx-font-size: 15px; -fx-text-fill: #2e1a28;");
        Label message = new Label(n.getMessage());
        message.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-text-fill: #604868;");
        message.setWrapText(true);
        Label time = new Label(n.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy")));
        time.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 11px; -fx-text-fill: #907898;");
        textCol.getChildren().addAll(title, message, time);

        Circle unreadDot = new Circle(4, Color.web("#e040a0"));
        unreadDot.setVisible(!n.isRead());

        card.getChildren().addAll(icon, textCol, unreadDot);
        return card;
    }

    private String getIconForType(NotificationType type) {
        switch (type) {
            case NEW_BID: return "gavel";
            case OUTBID: return "trending_down";
            case BID_SUCCESS: return "check_circle";
            case BID_FAILED: return "error";
            case AUCTION_EXTENDED: return "timer";
            case AUCTION_END_WIN: return "emoji_events";
            case AUCTION_END_LOSE: return "sentiment_dissatisfied";
            case AUTO_BID_CONFIGURED: return "bolt";
            case SYSTEM_ERROR: return "warning";
            default: return "notifications";
        }
    }

    private String getColorForSeverity(com.auction.client.model.notification.NotificationSeverity severity) {
        switch (severity) {
            case SUCCESS: return "#22c55e";
            case WARNING: return "#eab308";
            case DANGER: return "#e53e3e";
            case INFO:
            default: return "#e040a0";
        }
    }
}
