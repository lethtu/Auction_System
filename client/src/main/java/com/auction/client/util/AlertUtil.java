package com.auction.client.util;

import javafx.scene.control.Alert;

public final class AlertUtil {
    private AlertUtil() {
    }

    public static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfo(String message) {
        show(Alert.AlertType.INFORMATION, "Thành công", message);
    }

    public static void showWarning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    public static void showError(String message) {
        show(Alert.AlertType.ERROR, "Lỗi", message);
    }

    public static void showError(Exception e, String defaultMessage) {
        e.printStackTrace();

        String message = e.getMessage();

        showError(message == null || message.isBlank() ? defaultMessage : message);
    }
}