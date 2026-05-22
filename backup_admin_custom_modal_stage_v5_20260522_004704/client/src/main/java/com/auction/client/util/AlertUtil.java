package com.auction.client.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class AlertUtil {
    private static final String DEFAULT_INFO_TITLE = "Thành công";
    private static final String DEFAULT_ERROR_TITLE = "Lỗi";
    private static final String DEFAULT_WARNING_TITLE = "Cảnh báo";
    private static final String DEFAULT_ERROR_MESSAGE = "Đã xảy ra lỗi. Vui lòng thử lại.";

    private AlertUtil() {
    }

    public static void show(Alert.AlertType type, String title, String message) {
        String displayTitle = normalizeTitle(title, type);
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(displayTitle);
        dialog.setHeaderText(null);
        dialog.setResizable(false);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().clear();
        pane.setGraphic(null);
        pane.setContent(buildCardContent(dialog, type, displayTitle, normalizeMessage(message)));
        styleCardPane(pane);

        dialog.showAndWait();
    }

    public static void showInfo(String message) {
        show(Alert.AlertType.INFORMATION, DEFAULT_INFO_TITLE, message);
    }

    public static void showWarning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    public static void showError(String message) {
        show(Alert.AlertType.ERROR, DEFAULT_ERROR_TITLE, message);
    }

    public static void showError(Exception e, String defaultMessage) {
        String errorMessage = extractErrorMessage(e, defaultMessage);
        showError(errorMessage);
    }

    public static void styleDialog(Dialog<?> dialog) {
        if (dialog == null || dialog.getDialogPane() == null) {
            return;
        }

        DialogPane pane = dialog.getDialogPane();
        pane.setMinWidth(390);
        pane.setPrefWidth(430);
        pane.setMaxWidth(460);
        pane.setStyle("-fx-background-color: white;"
                + " -fx-border-color: #f5a6d8;"
                + " -fx-border-width: 1.4px;"
                + " -fx-border-radius: 22px;"
                + " -fx-background-radius: 22px;"
                + " -fx-padding: 22px;"
                + " -fx-font-family: 'DM Sans';");

        pane.lookupAll(".header-panel").forEach(node -> node.setStyle("-fx-background-color: transparent;"));
        pane.lookupAll(".header-panel .label").forEach(node -> node.setStyle("-fx-text-fill: #211427; -fx-font-size: 21px; -fx-font-weight: 900;"));
        pane.lookupAll(".content.label").forEach(node -> node.setStyle("-fx-text-fill: #6a4a72; -fx-font-size: 14px; -fx-font-weight: 600;"));
        pane.lookupAll(".text-field").forEach(node -> node.setStyle("-fx-background-color: #fff5fb;"
                + " -fx-border-color: #f3b3dd;"
                + " -fx-border-radius: 14px;"
                + " -fx-background-radius: 14px;"
                + " -fx-padding: 10px 14px;"
                + " -fx-font-size: 14px;"));
        pane.lookupAll(".button").forEach(AlertUtil::styleDialogButton);
    }

    private static VBox buildCardContent(Dialog<?> dialog, Alert.AlertType type, String title, String message) {
        VBox root = new VBox(14);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(18, 22, 20, 22));
        root.setMinWidth(340);
        root.setPrefWidth(370);
        root.setMaxWidth(390);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 24px;");

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(58, 58);
        iconCircle.setPrefSize(58, 58);
        iconCircle.setMaxSize(58, 58);
        iconCircle.setStyle("-fx-background-color: " + softIconBackground(type) + ";"
                + " -fx-background-radius: 22px;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.14), 12, 0, 0, 4);");

        Label icon = new Label(iconText(type));
        icon.setStyle("-fx-text-fill: " + iconColor(type) + "; -fx-font-size: 32px; -fx-font-weight: 900;");
        iconCircle.getChildren().add(icon);

        Label titleLabel = new Label(title);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(340);
        titleLabel.setStyle("-fx-text-fill: #211427; -fx-font-size: 23px; -fx-font-weight: 900;");

        Label messageLabel = new Label(message);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(330);
        messageLabel.setStyle("-fx-text-fill: #6a4a72; -fx-font-size: 14.5px; -fx-font-weight: 500; -fx-line-spacing: 2px;");

        Button agreeButton = new Button("Đồng ý");
        agreeButton.setDefaultButton(true);
        agreeButton.setOnAction(event -> dialog.close());
        stylePrimaryCardButton(agreeButton);

        root.getChildren().setAll(iconCircle, titleLabel, messageLabel, agreeButton);
        return root;
    }

    private static void styleCardPane(DialogPane pane) {
        pane.setMinWidth(390);
        pane.setPrefWidth(420);
        pane.setMaxWidth(440);
        pane.setStyle("-fx-background-color: white;"
                + " -fx-border-color: #f4a4d7;"
                + " -fx-border-width: 1.4px;"
                + " -fx-border-radius: 26px;"
                + " -fx-background-radius: 26px;"
                + " -fx-padding: 0;"
                + " -fx-font-family: 'DM Sans';");
    }

    private static void stylePrimaryCardButton(Button button) {
        button.setMinWidth(220);
        button.setPrefWidth(240);
        button.setMaxWidth(260);
        button.setMinHeight(44);
        button.setPrefHeight(44);
        button.setStyle("-fx-background-color: #e040a0;"
                + " -fx-text-fill: white;"
                + " -fx-font-size: 15px;"
                + " -fx-font-weight: 900;"
                + " -fx-background-radius: 22px;"
                + " -fx-padding: 10px 28px;"
                + " -fx-cursor: hand;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.26), 12, 0, 0, 4);");
    }

    private static void styleDialogButton(Node node) {
        if (node instanceof Button button) {
            boolean primary = ButtonBar.getButtonData(button) != null && ButtonBar.getButtonData(button).isDefaultButton();
            if (primary || "Đồng ý".equals(button.getText()) || "OK".equalsIgnoreCase(button.getText())) {
                button.setMinWidth(112);
                button.setMinHeight(40);
                button.setStyle("-fx-background-color: #e040a0;"
                        + " -fx-text-fill: white;"
                        + " -fx-font-size: 14px;"
                        + " -fx-font-weight: 900;"
                        + " -fx-background-radius: 18px;"
                        + " -fx-padding: 9px 24px;"
                        + " -fx-cursor: hand;"
                        + " -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.22), 10, 0, 0, 3);");
            } else {
                button.setMinWidth(92);
                button.setMinHeight(40);
                button.setStyle("-fx-background-color: #fff1fa;"
                        + " -fx-text-fill: #8a2b66;"
                        + " -fx-font-size: 13px;"
                        + " -fx-font-weight: 800;"
                        + " -fx-background-radius: 18px;"
                        + " -fx-border-color: #f2a6d4;"
                        + " -fx-border-radius: 18px;"
                        + " -fx-padding: 9px 20px;"
                        + " -fx-cursor: hand;");
            }
        }
    }

    private static String iconText(Alert.AlertType type) {
        return switch (type) {
            case INFORMATION -> "✓";
            case ERROR -> "×";
            case WARNING -> "!";
            default -> "i";
        };
    }

    private static String iconColor(Alert.AlertType type) {
        return switch (type) {
            case INFORMATION -> "#10b981";
            case ERROR -> "#ef4444";
            case WARNING -> "#f59e0b";
            default -> "#e040a0";
        };
    }

    private static String softIconBackground(Alert.AlertType type) {
        return switch (type) {
            case INFORMATION -> "#dcfce7";
            case ERROR -> "#fee2e2";
            case WARNING -> "#fff7ed";
            default -> "#fce7f3";
        };
    }

    private static String extractErrorMessage(Exception e, String defaultMessage) {
        if (e != null && !isBlank(e.getMessage())) {
            return e.getMessage();
        }

        if (!isBlank(defaultMessage)) {
            return defaultMessage;
        }

        return DEFAULT_ERROR_MESSAGE;
    }

    private static String normalizeTitle(String title, Alert.AlertType type) {
        if (!isBlank(title)) {
            return title;
        }

        return switch (type) {
            case INFORMATION -> DEFAULT_INFO_TITLE;
            case WARNING -> DEFAULT_WARNING_TITLE;
            case ERROR -> DEFAULT_ERROR_TITLE;
            default -> "";
        };
    }

    private static String normalizeMessage(String message) {
        return isBlank(message) ? DEFAULT_ERROR_MESSAGE : message;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

