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
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(displayTitle);
        dialog.setHeaderText(null);

        ButtonType agreeButton = new ButtonType("Đồng ý", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(agreeButton);
        dialog.getDialogPane().setContent(buildAlertContent(type, displayTitle, normalizeMessage(message)));

        styleDialog(dialog);
        stylePrimaryButton(dialog.getDialogPane().lookupButton(agreeButton));
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
        pane.setMinWidth(420);
        pane.setPrefWidth(460);
        pane.setMaxWidth(520);
        pane.setStyle("-fx-background-color: white;"
                + " -fx-border-color: #f5a6d8;"
                + " -fx-border-width: 1.5px;"
                + " -fx-border-radius: 24px;"
                + " -fx-background-radius: 24px;"
                + " -fx-padding: 26px 30px 24px 30px;"
                + " -fx-font-family: 'DM Sans';");

        pane.lookupAll(".content.label").forEach(node -> node.setStyle("-fx-text-fill: #5d4268; -fx-font-size: 15px;"));
        pane.lookupAll(".text-field").forEach(node -> node.setStyle("-fx-background-color: #fff5fb;"
                + " -fx-border-color: #f2b6dc;"
                + " -fx-border-radius: 14px;"
                + " -fx-background-radius: 14px;"
                + " -fx-padding: 10px 14px;"
                + " -fx-font-size: 14px;"));
        pane.lookupAll(".button").forEach(AlertUtil::styleSecondaryButton);
    }

    private static VBox buildAlertContent(Alert.AlertType type, String title, String message) {
        VBox root = new VBox(18);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(2, 2, 6, 2));
        root.setMinWidth(360);
        root.setPrefWidth(390);

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(76, 76);
        iconCircle.setPrefSize(76, 76);
        iconCircle.setMaxSize(76, 76);
        iconCircle.setStyle("-fx-background-color: " + softIconBackground(type) + ";"
                + " -fx-background-radius: 24px;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.18), 14, 0, 0, 5);");

        Label icon = new Label(iconText(type));
        icon.setStyle("-fx-text-fill: " + iconColor(type) + "; -fx-font-size: 40px; -fx-font-weight: 900;");
        iconCircle.getChildren().add(icon);

        Label titleLabel = new Label(title);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(360);
        titleLabel.setStyle("-fx-text-fill: #211427; -fx-font-size: 26px; -fx-font-weight: 900;");

        Label messageLabel = new Label(message);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(360);
        messageLabel.setStyle("-fx-text-fill: #5d4268; -fx-font-size: 16px; -fx-font-weight: 500; -fx-line-spacing: 3px;");

        root.getChildren().setAll(iconCircle, titleLabel, messageLabel);
        return root;
    }

    private static void stylePrimaryButton(Node node) {
        if (node instanceof Button button) {
            button.setMinWidth(320);
            button.setPrefWidth(340);
            button.setMinHeight(48);
            button.setStyle("-fx-background-color: #e040a0;"
                    + " -fx-text-fill: white;"
                    + " -fx-font-size: 16px;"
                    + " -fx-font-weight: 900;"
                    + " -fx-background-radius: 22px;"
                    + " -fx-padding: 12px 32px;"
                    + " -fx-cursor: hand;"
                    + " -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.30), 14, 0, 0, 5);");
        }
    }

    private static void styleSecondaryButton(Node node) {
        if (node instanceof Button button) {
            button.setMinWidth(100);
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
