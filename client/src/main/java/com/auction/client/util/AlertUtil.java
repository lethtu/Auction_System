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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(normalizeTitle(title, type));
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().setContent(buildContent(type, normalizeMessage(message)));
        styleDialog(dialog);
        stylePrimaryButton(dialog.getDialogPane().lookupButton(ButtonType.OK));
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
        pane.setMinWidth(430);
        pane.setPrefWidth(500);
        pane.setStyle("-fx-background-color: #fff9fd;"
                + " -fx-border-color: #f6a6d7;"
                + " -fx-border-width: 1.5px;"
                + " -fx-border-radius: 18px;"
                + " -fx-background-radius: 18px;"
                + " -fx-padding: 18px;"
                + " -fx-font-family: 'DM Sans';");

        pane.lookupAll(".button").forEach(AlertUtil::styleSecondaryButton);
    }

    private static Node buildContent(Alert.AlertType type, String message) {
        HBox root = new HBox(18);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(8, 8, 4, 8));

        StackPane iconBox = new StackPane();
        iconBox.setMinSize(58, 58);
        iconBox.setPrefSize(58, 58);
        iconBox.setMaxSize(58, 58);
        iconBox.setStyle("-fx-background-color: " + iconBackground(type) + ";"
                + " -fx-background-radius: 18px;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.22), 12, 0, 0, 4);");

        Label icon = new Label(iconText(type));
        icon.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 28px;");
        iconBox.getChildren().add(icon);

        Label text = new Label(message);
        text.setWrapText(true);
        text.setMaxWidth(360);
        text.setStyle("-fx-text-fill: #211427; -fx-font-size: 16px; -fx-font-weight: 700; -fx-line-spacing: 2px;");

        root.getChildren().addAll(iconBox, text);
        HBox.setHgrow(text, Priority.ALWAYS);
        return root;
    }

    private static void stylePrimaryButton(Node node) {
        if (node instanceof Button button) {
            button.setMinWidth(96);
            button.setMinHeight(42);
            button.setStyle("-fx-background-color: #e040a0;"
                    + " -fx-text-fill: white;"
                    + " -fx-font-weight: 800;"
                    + " -fx-background-radius: 18px;"
                    + " -fx-padding: 10px 26px;"
                    + " -fx-cursor: hand;"
                    + " -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.25), 12, 0, 0, 4);");
        }
    }

    private static void styleSecondaryButton(Node node) {
        if (node instanceof Button button) {
            button.setStyle("-fx-background-color: #fff1fa;"
                    + " -fx-text-fill: #8a2b66;"
                    + " -fx-font-weight: 700;"
                    + " -fx-background-radius: 16px;"
                    + " -fx-border-color: #f6a6d7;"
                    + " -fx-border-radius: 16px;"
                    + " -fx-padding: 8px 18px;"
                    + " -fx-cursor: hand;");
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

    private static String iconBackground(Alert.AlertType type) {
        return switch (type) {
            case INFORMATION -> "#22c55e";
            case ERROR -> "#ef4444";
            case WARNING -> "#f59e0b";
            default -> "#e040a0";
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
