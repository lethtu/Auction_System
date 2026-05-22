package com.auction.client.util;

import java.net.URL;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;

public final class AlertUtil {
    private static final String DEFAULT_INFO_TITLE = "Thành công";
    private static final String DEFAULT_ERROR_TITLE = "Lỗi";
    private static final String DEFAULT_WARNING_TITLE = "Cảnh báo";
    private static final String DEFAULT_ERROR_MESSAGE = "Đã xảy ra lỗi. Vui lòng thử lại.";
    private static final String ADMIN_STYLESHEET_PATH = "/com/auction/client/view/styles.css";
    private static final double DEFAULT_DIALOG_WIDTH = 520;

    private AlertUtil() {
    }

    public static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(normalizeTitle(title, type));
        alert.setHeaderText(null);
        alert.setContentText(normalizeMessage(message));
        alert.setGraphic(createStyledIcon(type));
        styleAlert(alert, type);
        alert.showAndWait();
    }

    public static void styleDialog(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }

        styleDialogPane(dialog.getDialogPane(), null);
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

    private static void styleAlert(Alert alert, Alert.AlertType type) {
        if (alert == null) {
            return;
        }

        String typeClass = type == null ? null : "admin-alert-" + type.name().toLowerCase();
        styleDialogPane(alert.getDialogPane(), typeClass);
    }

    private static void styleDialogPane(DialogPane dialogPane, String typeClass) {
        if (dialogPane == null) {
            return;
        }

        addStyleClass(dialogPane, "admin-alert-dialog");
        if (!isBlank(typeClass)) {
            addStyleClass(dialogPane, typeClass);
        }

        URL stylesheet = AlertUtil.class.getResource(ADMIN_STYLESHEET_PATH);
        if (stylesheet != null) {
            String stylesheetPath = stylesheet.toExternalForm();
            if (!dialogPane.getStylesheets().contains(stylesheetPath)) {
                dialogPane.getStylesheets().add(stylesheetPath);
            }
        }

        dialogPane.setMinWidth(DEFAULT_DIALOG_WIDTH);
        dialogPane.setPrefWidth(DEFAULT_DIALOG_WIDTH);
    }

    private static Label createStyledIcon(Alert.AlertType type) {
        Label icon = new Label(iconText(type));
        icon.getStyleClass().add("admin-alert-icon");
        icon.getStyleClass().add("admin-alert-icon-" + iconType(type));
        return icon;
    }

    private static String iconText(Alert.AlertType type) {
        if (type == Alert.AlertType.INFORMATION) {
            return "✓";
        }
        if (type == Alert.AlertType.ERROR) {
            return "×";
        }
        return "!";
    }

    private static String iconType(Alert.AlertType type) {
        if (type == Alert.AlertType.INFORMATION) {
            return "information";
        }
        if (type == Alert.AlertType.ERROR) {
            return "error";
        }
        return "warning";
    }

    private static void addStyleClass(DialogPane dialogPane, String styleClass) {
        if (!dialogPane.getStyleClass().contains(styleClass)) {
            dialogPane.getStyleClass().add(styleClass);
        }
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
