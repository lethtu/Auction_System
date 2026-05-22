package com.auction.client.util;

import javafx.scene.control.Alert;

public final class AlertUtil {
    private static final String DEFAULT_INFO_TITLE = "Thành công";
    private static final String DEFAULT_ERROR_TITLE = "Lỗi";
    private static final String DEFAULT_WARNING_TITLE = "Cảnh báo";
    private static final String DEFAULT_ERROR_MESSAGE = "Đã xảy ra lỗi. Vui lòng thử lại.";

    private AlertUtil() {
    }

    public static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(normalizeTitle(title, type));
        alert.setHeaderText(null);
        alert.setContentText(normalizeMessage(message));
        alert.showAndWait();
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