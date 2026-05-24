package com.auction.client.util;

import com.auction.client.Config;
import com.auction.client.model.User;
import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationSeverity;
import com.auction.client.model.notification.NotificationType;
import com.auction.client.service.AppStyleManager;
import com.auction.client.service.NotificationCenterService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.prefs.Preferences;

public final class ShippingInfoDialog {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private ShippingInfoDialog() {
    }

    public static void show(Integer auctionId, String itemName) {
        Stage dialog = createStage();
        VBox card = new VBox(18);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(25, 28, 26, 28));
        card.setMinWidth(470);
        card.setPrefWidth(500);
        card.setMaxWidth(520);
        card.setStyle("-fx-background-color: -app-card; -fx-background-radius: 28px;"
                + " -fx-border-radius: 28px; -fx-border-color: -app-border; -fx-border-width: 1.3px;"
                + " -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-16, 26, 0, 0, 9);"
                + " -fx-font-family: 'DM Sans';");

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(58, 58);
        iconCircle.setPrefSize(58, 58);
        iconCircle.setMaxSize(58, 58);
        iconCircle.setStyle("-fx-background-color: -app-accent-opacity-16; -fx-background-radius: 22px;");
        Label icon = new Label("\ue558");
        icon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 29px; -fx-text-fill: -fx-accent;");
        iconCircle.getChildren().add(icon);

        Label title = new Label("Delivery details");
        title.setStyle("-fx-font-size: 23px; -fx-font-weight: 900; -fx-text-fill: -app-text;");

        Label subtitle = new Label("Your winning item: " + safeItemName(itemName) + "  |  Auction #" + safeAuctionId(auctionId));
        subtitle.setWrapText(true);
        subtitle.setAlignment(Pos.CENTER);
        subtitle.setMaxWidth(420);
        subtitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -app-text-muted;");

        VBox header = new VBox(8, iconCircle, title, subtitle);
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(444);
        header.setStyle("-fx-cursor: move; -fx-padding: 0 0 2px 0;");
        enableDragging(header, dialog);

        HBox contactRow = new HBox(12);
        TextField fullName = createField("Full name");
        fullName.setText(safe(User.getFullname()));
        TextField phone = createField("Phone number");
        VBox.setVgrow(fullName, Priority.NEVER);
        HBox.setHgrow(fullName, Priority.ALWAYS);
        HBox.setHgrow(phone, Priority.ALWAYS);
        contactRow.getChildren().addAll(fieldGroup("Recipient", fullName), fieldGroup("Phone number", phone));

        TextField address = createField("Street address, ward, district, city");
        TextArea note = new TextArea();
        note.setPromptText("Notes for delivery (optional)");
        note.setWrapText(true);
        note.setPrefRowCount(2);
        note.setMaxHeight(72);
        note.setStyle(fieldStyle("16px"));

        VBox form = new VBox(13,
                contactRow,
                fieldGroup("Delivery address", address),
                fieldGroup("Note", note));
        form.setMaxWidth(444);

        Label validation = new Label();
        validation.setVisible(false);
        validation.setManaged(false);
        validation.setWrapText(true);
        validation.setMaxWidth(444);
        validation.setStyle("-fx-background-color: rgba(239, 68, 68, 0.10); -fx-text-fill: #ef4444;"
                + " -fx-background-radius: 12px; -fx-font-size: 12px; -fx-font-weight: 700;"
                + " -fx-padding: 9px 12px;");

        Button later = secondaryButton("Later");
        Button submit = primaryButton("Submit delivery info");
        HBox buttons = new HBox(12, later, submit);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(3, 0, 0, 0));

        later.setOnAction(event -> dialog.close());
        submit.setOnAction(event -> {
            if (safe(fullName.getText()).trim().isEmpty() || safe(phone.getText()).trim().isEmpty()
                    || safe(address.getText()).trim().isEmpty()) {
                validation.setText("Please enter recipient, phone number and delivery address.");
                validation.setManaged(true);
                validation.setVisible(true);
                return;
            }
            saveDeliveryInfo(auctionId, fullName.getText(), phone.getText(), address.getText(), note.getText());
            dialog.close();
            submitDeliveryInfo(auctionId, itemName, fullName.getText(), phone.getText(), address.getText(),
                    note.getText());
        });

        card.getChildren().addAll(header, form, validation, buttons);

        StackPane wrapper = new StackPane(card);
        wrapper.setPadding(new Insets(18));
        wrapper.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(wrapper);
        scene.setFill(Color.TRANSPARENT);
        String styles = ShippingInfoDialog.class.getResource("/com/auction/client/view/styles.css").toExternalForm();
        scene.getStylesheets().add(styles);
        AppStyleManager.applyCurrentStyle(scene);
        dialog.setScene(scene);
        dialog.setOnShown(event -> phone.requestFocus());
        dialog.show();
    }

    private static void enableDragging(Node dragArea, Stage dialog) {
        double[] dragOffset = new double[2];
        dragArea.setOnMousePressed(event -> {
            dragOffset[0] = dialog.getX() - event.getScreenX();
            dragOffset[1] = dialog.getY() - event.getScreenY();
        });
        dragArea.setOnMouseDragged(event -> {
            dialog.setX(event.getScreenX() + dragOffset[0]);
            dialog.setY(event.getScreenY() + dragOffset[1]);
        });
    }

    private static VBox fieldGroup(String labelText, javafx.scene.Node control) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: -app-text-muted;");
        VBox box = new VBox(6, label, control);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private static TextField createField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(43);
        field.setStyle(fieldStyle("999px"));
        return field;
    }

    private static String fieldStyle(String radius) {
        return "-fx-background-color: -app-input-bg; -fx-border-color: -app-border; -fx-border-width: 1.3px;"
                + " -fx-background-radius: " + radius + "; -fx-border-radius: " + radius + ";"
                + " -fx-padding: 10px 14px; -fx-text-fill: -app-text; -fx-font-family: 'DM Sans';"
                + " -fx-font-size: 13px; -fx-prompt-text-fill: -app-placeholder;";
    }

    private static Button primaryButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(43);
        button.setMinWidth(190);
        button.setStyle("-fx-background-color: -fx-accent; -fx-text-fill: white; -fx-font-size: 14px;"
                + " -fx-font-weight: 900; -fx-background-radius: 22px; -fx-padding: 10px 24px;"
                + " -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-24, 12, 0, 0, 4);");
        return button;
    }

    private static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(43);
        button.setMinWidth(100);
        button.setStyle("-fx-background-color: -app-accent-opacity-08; -fx-text-fill: -fx-accent;"
                + " -fx-font-size: 14px; -fx-font-weight: 800; -fx-background-radius: 22px;"
                + " -fx-border-color: -fx-accent; -fx-border-radius: 22px; -fx-padding: 10px 22px; -fx-cursor: hand;");
        return button;
    }

    private static Stage createStage() {
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        Window owner = Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        try {
            dialog.getIcons().add(new Image(ShippingInfoDialog.class.getResourceAsStream(Config.LOGO_PATH)));
        } catch (Exception ignored) {
        }
        return dialog;
    }

    private static void saveDeliveryInfo(Integer auctionId, String fullName, String phone, String address, String note) {
        String key = "auction_" + safeAuctionId(auctionId);
        Preferences prefs = Preferences.userNodeForPackage(ShippingInfoDialog.class).node("delivery");
        prefs.put(key + "_name", safe(fullName).trim());
        prefs.put(key + "_phone", safe(phone).trim());
        prefs.put(key + "_address", safe(address).trim());
        prefs.put(key + "_note", safe(note).trim());
        prefs.putLong(key + "_savedAt", System.currentTimeMillis());
    }

    private static void submitDeliveryInfo(Integer auctionId, String itemName, String fullName,
            String phone, String address, String note) {
        if (auctionId == null || User.getSessionToken() == null || User.getSessionToken().isBlank()) {
            addDeliveryNotification(NotificationSeverity.WARNING, "Delivery saved locally",
                    "Please sign in again to send delivery details to the seller.", auctionId, itemName);
            return;
        }

        JSONObject payload = new JSONObject();
        payload.put("recipientName", safe(fullName).trim());
        payload.put("phoneNumber", safe(phone).trim());
        payload.put("address", safe(address).trim());
        payload.put("note", safe(note).trim());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.API_URL + "/api/auctions/" + auctionId + "/delivery"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .header("X-Auth-Token", User.getSessionToken())
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> Platform.runLater(() -> {
                    if (error == null && response != null && response.statusCode() >= 200
                            && response.statusCode() < 300) {
                        addDeliveryNotification(NotificationSeverity.SUCCESS, "Delivery details submitted",
                                "Delivery information for " + safeItemName(itemName) + " was sent successfully.",
                                auctionId, itemName);
                    } else {
                        addDeliveryNotification(NotificationSeverity.WARNING, "Delivery saved locally",
                                "Server submission could not be confirmed. You can submit it again from My Bids.",
                                auctionId, itemName);
                    }
                }));
    }

    private static void addDeliveryNotification(NotificationSeverity severity, String title, String message,
            Integer auctionId, String itemName) {
        AppNotification notification = new AppNotification(NotificationType.GENERAL, severity, title, message);
        notification.setAuctionId(auctionId);
        notification.setItemName(itemName);
        notification.setSoundMuted(true);
        NotificationCenterService.getInstance().addNotification(notification);
    }

    private static String safeAuctionId(Integer auctionId) {
        return auctionId == null ? "unknown" : String.valueOf(auctionId);
    }

    private static String safeItemName(String itemName) {
        return itemName == null || itemName.isBlank() ? "your winning item" : itemName;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
