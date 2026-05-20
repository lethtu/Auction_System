package com.auction.client.util;

import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationType;
import com.auction.client.service.NotificationCenterService;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;

public class NotificationBellBinder {

    public static void bind(Node bellIconNode, Label badgeLabel) {
        NotificationCenterService service = NotificationCenterService.getInstance();
        
        // Initial state
        updateBadge(badgeLabel, service.unreadCountProperty().get());
        
        // Listen to unread count
        service.unreadCountProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> updateBadge(badgeLabel, newVal.intValue()));
        });

        Popup popup = createPopup(service);

        // Toggle popup on click
        bellIconNode.setOnMouseClicked(e -> {
            if (popup.isShowing()) {
                popup.hide();
            } else {
                Window window = bellIconNode.getScene().getWindow();
                Bounds bounds = bellIconNode.localToScreen(bellIconNode.getBoundsInLocal());
                // Align right
                double x = bounds.getMaxX() - 350; // Popup width is 350
                double y = bounds.getMaxY() + 10;
                popup.show(window, x, y);
            }
        });
    }

    private static void updateBadge(Label badgeLabel, int count) {
        if (count <= 0) {
            badgeLabel.setVisible(false);
            badgeLabel.setManaged(false);
        } else {
            badgeLabel.setVisible(true);
            badgeLabel.setManaged(true);
            badgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
        }
    }

    private static Popup createPopup(NotificationCenterService service) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox root = new VBox();
        root.setPrefWidth(350);
        root.setMaxHeight(500);
        root.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; " +
                "-fx-border-color: #f2e8f2; -fx-border-radius: 12px; -fx-border-width: 1px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.15), 15, 0, 0, 5);");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 10, 20));
        header.setStyle("-fx-border-color: #f2e8f2; -fx-border-width: 0 0 1px 0;");

        Label title = new Label("Notifications");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#2e1a28"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button markReadBtn = new Button("Mark all as read");
        markReadBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e040a0; -fx-cursor: hand; -fx-font-size: 13px;");
        markReadBtn.setOnAction(e -> service.markAllAsRead());

        header.getChildren().addAll(title, spacer, markReadBtn);

        // Content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scrollPane.setPrefHeight(350);
        
        VBox notificationList = new VBox();
        notificationList.setSpacing(0);
        scrollPane.setContent(notificationList);

        Runnable updateList = () -> {
            notificationList.getChildren().clear();
            if (service.getNotifications().isEmpty()) {
                Label emptyLabel = new Label("No notifications yet.");
                emptyLabel.setStyle("-fx-text-fill: #907898; -fx-font-size: 14px; -fx-padding: 30px;");
                emptyLabel.setMaxWidth(Double.MAX_VALUE);
                emptyLabel.setAlignment(Pos.CENTER);
                notificationList.getChildren().add(emptyLabel);
            } else {
                for (AppNotification n : service.getNotifications()) {
                    notificationList.getChildren().add(createNotificationCard(n, service));
                }
            }
        };

        service.getNotifications().addListener((ListChangeListener.Change<? extends AppNotification> c) -> {
            Platform.runLater(updateList);
        });
        updateList.run();

        root.getChildren().addAll(header, scrollPane);
        popup.getContent().add(root);

        return popup;
    }

    private static Node createNotificationCard(AppNotification notification, NotificationCenterService service) {
        HBox card = new HBox();
        card.setSpacing(12);
        card.setPadding(new Insets(12, 20, 12, 20));
        
        Runnable updateBg = () -> {
            if (notification.isRead()) {
                card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #f2e8f2; -fx-border-width: 0 0 1px 0; -fx-cursor: hand;");
            } else {
                card.setStyle("-fx-background-color: #fff3fb; -fx-border-color: #f2e8f2; -fx-border-width: 0 0 1px 0; -fx-cursor: hand;");
            }
        };
        updateBg.run();
        notification.readProperty().addListener((obs, o, n) -> updateBg.run());

        // Icon
        Label iconLabel = new Label(getIconForType(notification.getType()));
        iconLabel.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 24px; -fx-text-fill: " + getColorForSeverity(notification.getSeverity()) + ";");
        iconLabel.setPadding(new Insets(5, 0, 0, 0));

        // Text Content
        VBox textContent = new VBox();
        textContent.setSpacing(4);
        HBox.setHgrow(textContent, Priority.ALWAYS);

        Label title = new Label(notification.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e1a28; -fx-font-size: 14px;");
        title.setWrapText(true);

        Label message = new Label(notification.getMessage());
        message.setStyle("-fx-text-fill: #604868; -fx-font-size: 13px;");
        message.setWrapText(true);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        Label timeLabel = new Label(notification.getCreatedAt().format(dtf));
        timeLabel.setStyle("-fx-text-fill: #907898; -fx-font-size: 11px;");

        textContent.getChildren().addAll(title, message, timeLabel);
        
        card.getChildren().addAll(iconLabel, textContent);

        card.setOnMouseClicked(e -> {
            service.markAsRead(notification.getId());
            // Here we could emit an event to navigate to the auction page if auctionId is present.
            // For now, it just marks as read.
        });

        return card;
    }

    private static String getIconForType(NotificationType type) {
        switch (type) {
            case OUTBID: return "\ue002"; // warning (not exact but close)
            case NEW_BID: return "\ue8d4"; // trending_up or gavel (\ue900)
            case AUCTION_EXTENDED: return "\ue8b5"; // schedule
            case AUCTION_END_WIN:
            case BID_SUCCESS: return "\ue86c"; // check_circle
            case BID_FAILED:
            case AUCTION_END_LOSE:
            case SYSTEM_ERROR: return "\ue000"; // error
            case AUTO_BID_CONFIGURED:
            case AUTO_BID_PLACED: return "\uea0b"; // bolt
            default: return "\ue7f4"; // notifications
        }
    }

    private static String getColorForSeverity(com.auction.client.model.notification.NotificationSeverity severity) {
        if (severity == null) return "#e040a0";
        switch (severity) {
            case SUCCESS: return "#4caf50";
            case WARNING: return "#ff9800";
            case DANGER: return "#f44336";
            case INFO:
            default: return "#e040a0"; // App accent color
        }
    }
}
