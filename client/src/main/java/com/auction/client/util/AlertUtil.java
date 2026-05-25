package com.auction.client.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.scene.image.Image;
import com.auction.client.Config;

import java.util.Optional;

public final class AlertUtil {
    private static final String DEFAULT_INFO_TITLE = "Success";
    private static final String DEFAULT_ERROR_TITLE = "Error";
    private static final String DEFAULT_WARNING_TITLE = "Warning";
    private static final String DEFAULT_ERROR_MESSAGE = "An error occurred. Please try again.";

    private AlertUtil() {
    }

    public static void show(Alert.AlertType type, String title, String message) {
        showCardModal(normalizeTitle(title, type), normalizeMessage(message), type);
    }

    public static void showInfo(String message) {
        show(Alert.AlertType.INFORMATION, DEFAULT_INFO_TITLE, message);
    }

    public static void showInfo(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static void showSuccess(String message) {
        showInfo(DEFAULT_INFO_TITLE, message);
    }

    public static void showSuccess(String title, String message) {
        showInfo(title, message);
    }

    public static void showWarning(String message) {
        show(Alert.AlertType.WARNING, DEFAULT_WARNING_TITLE, message);
    }

    public static void showWarning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    public static void showError(String message) {
        show(Alert.AlertType.ERROR, DEFAULT_ERROR_TITLE, message);
    }

    public static void showError(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    public static void showError(Exception e, String defaultMessage) {
        showError(extractErrorMessage(e, defaultMessage));
    }

    public static Optional<String> promptText(String title, String message) {
        Stage dialog = createBaseStage(title);

        TextField input = new TextField();
        input.setPromptText("Enter content...");
        input.setMaxWidth(330);
        input.setStyle("-fx-background-color: -app-input-bg;"
                + " -fx-border-color: -app-border;"
                + " -fx-border-width: 1.2px;"
                + " -fx-border-radius: 16px;"
                + " -fx-background-radius: 16px;"
                + " -fx-padding: 11px 14px;"
                + " -fx-font-size: 14px;"
                + " -fx-text-fill: -app-text;");

        final String[] value = {null};

        Button cancelButton = secondaryButton("Cancel");
        Button okButton = primaryButton("OK");
        cancelButton.setOnAction(event -> dialog.close());
        okButton.setOnAction(event -> {
            value[0] = input.getText();
            dialog.close();
        });

        HBox actions = new HBox(12, cancelButton, okButton);
        actions.setAlignment(Pos.CENTER);

        VBox card = baseCard(Alert.AlertType.INFORMATION, title, message);
        card.getChildren().addAll(input, actions);

        Scene scene = new Scene(wrap(card));
        scene.setFill(Color.TRANSPARENT);
        if (AlertUtil.class.getResource("/com/auction/client/view/styles.css") != null) {
            scene.getStylesheets().add(AlertUtil.class.getResource("/com/auction/client/view/styles.css").toExternalForm());
        }
        com.auction.client.service.AppStyleManager.applyCurrentStyle(scene);
        dialog.setScene(scene);
        dialog.setOnShown(event -> input.requestFocus());
        dialog.showAndWait();

        return value[0] == null ? Optional.empty() : Optional.of(value[0]);
    }

    public static boolean showConfirmation(String title, String message) {
        return showDecision(title, message, Alert.AlertType.WARNING, "OK", "Cancel");
    }

    public static boolean showBidConfirmation(String title, String message, boolean highBidWarning) {
        return showDecision(
                title,
                message,
                highBidWarning ? Alert.AlertType.WARNING : Alert.AlertType.CONFIRMATION,
                "Confirm",
                "Cancel");
    }

    private static boolean showDecision(String title, String message, Alert.AlertType type, String confirmText, String cancelText) {
        Stage dialog = createBaseStage(title);
        VBox card = baseCard(type, title, message);
        final boolean[] result = {false};

        Button cancelButton = secondaryButton(cancelText == null || cancelText.isBlank() ? "Cancel" : cancelText);
        Button okButton = primaryButton(confirmText == null || confirmText.isBlank() ? "OK" : confirmText);
        cancelButton.setCancelButton(true);
        okButton.setDefaultButton(true);
        cancelButton.setOnAction(event -> dialog.close());
        okButton.setOnAction(event -> {
            result[0] = true;
            dialog.close();
        });

        HBox actions = new HBox(12, cancelButton, okButton);
        actions.setAlignment(Pos.CENTER);
        card.getChildren().add(actions);

        Scene scene = new Scene(wrap(card));
        scene.setFill(Color.TRANSPARENT);
        if (AlertUtil.class.getResource("/com/auction/client/view/styles.css") != null) {
            scene.getStylesheets().add(AlertUtil.class.getResource("/com/auction/client/view/styles.css").toExternalForm());
        }
        com.auction.client.service.AppStyleManager.applyCurrentStyle(scene);
        dialog.setScene(scene);
        dialog.showAndWait();

        return result[0];
    }

    public static void styleDialog(Dialog<?> dialog) {
        if (dialog == null || dialog.getDialogPane() == null) {
            return;
        }

        DialogPane pane = dialog.getDialogPane();
        pane.setMinWidth(360);
        pane.setPrefWidth(390);
        pane.setMaxWidth(420);
        pane.setStyle("-fx-background-color: -app-card;"
                + " -fx-border-color: -app-border;"
                + " -fx-border-width: 1.2px;"
                + " -fx-border-radius: 22px;"
                + " -fx-background-radius: 22px;"
                + " -fx-padding: 20px;"
                + " -fx-font-family: 'DM Sans';");

        if (AlertUtil.class.getResource("/com/auction/client/view/styles.css") != null) {
            String cssPath = AlertUtil.class.getResource("/com/auction/client/view/styles.css").toExternalForm();
            if (!pane.getStylesheets().contains(cssPath)) {
                pane.getStylesheets().add(cssPath);
            }
        }

        pane.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                com.auction.client.service.AppStyleManager.applyCurrentStyle(newScene);
                javafx.application.Platform.runLater(() -> {
                    pane.lookupAll(".header-panel").forEach(node -> node.setStyle("-fx-background-color: transparent;"));
                    pane.lookupAll(".header-panel .label").forEach(node -> node.setStyle("-fx-text-fill: -app-text; -fx-font-size: 20px; -fx-font-weight: 900;"));
                    pane.lookupAll(".content.label").forEach(node -> {
                        node.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 14px; -fx-font-weight: 600;");
                        if (node instanceof javafx.scene.control.Label label) {
                            label.setWrapText(true);
                            label.setMaxWidth(360);
                        }
                    });
                    pane.lookupAll(".text-field").forEach(node -> node.setStyle("-fx-background-color: -app-input-bg;"
                            + " -fx-border-color: -app-border;"
                            + " -fx-border-radius: 14px;"
                            + " -fx-background-radius: 14px;"
                            + " -fx-padding: 10px 14px;"
                            + " -fx-font-size: 14px;"
                            + " -fx-text-fill: -app-text;"));
                    pane.lookupAll(".button").forEach(AlertUtil::styleDialogButton);
                });
            }
        });

        if (pane.getScene() != null) {
            com.auction.client.service.AppStyleManager.applyCurrentStyle(pane.getScene());
            javafx.application.Platform.runLater(() -> {
                pane.lookupAll(".header-panel").forEach(node -> node.setStyle("-fx-background-color: transparent;"));
                pane.lookupAll(".header-panel .label").forEach(node -> node.setStyle("-fx-text-fill: -app-text; -fx-font-size: 20px; -fx-font-weight: 900;"));
                pane.lookupAll(".content.label").forEach(node -> {
                    node.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 14px; -fx-font-weight: 600;");
                    if (node instanceof javafx.scene.control.Label label) {
                        label.setWrapText(true);
                        label.setMaxWidth(360);
                    }
                });
                pane.lookupAll(".text-field").forEach(node -> node.setStyle("-fx-background-color: -app-input-bg;"
                        + " -fx-border-color: -app-border;"
                        + " -fx-border-radius: 14px;"
                        + " -fx-background-radius: 14px;"
                        + " -fx-padding: 10px 14px;"
                        + " -fx-font-size: 14px;"
                        + " -fx-text-fill: -app-text;"));
                pane.lookupAll(".button").forEach(AlertUtil::styleDialogButton);
            });
        }
    }


    public static void styleAndShow(Alert alert) {
        if (alert == null) {
            return;
        }

        String title = alert.getTitle();
        String message = alert.getContentText();
        String header = alert.getHeaderText();
        if (header != null && !header.isBlank()) {
            if (isBlank(message)) {
                message = header;
            } else {
                message = header + "\n" + message;
            }
        }

        if (isBlank(title)) {
            if (alert.getAlertType() == Alert.AlertType.ERROR) {
                title = DEFAULT_ERROR_TITLE;
            } else if (alert.getAlertType() == Alert.AlertType.WARNING) {
                title = DEFAULT_WARNING_TITLE;
            } else {
                title = DEFAULT_INFO_TITLE;
            }
        }

        if (isBlank(message)) {
            message = DEFAULT_ERROR_MESSAGE;
        }

        show(alert.getAlertType(), title, message);
    }

    private static void showCardModal(String title, String message, Alert.AlertType type) {
        Stage dialog = createBaseStage(title);
        VBox card = baseCard(type, title, message);

        Button okButton = primaryButton("OK");
        okButton.setDefaultButton(true);
        okButton.setOnAction(event -> dialog.close());
        card.getChildren().add(okButton);

        Scene scene = new Scene(wrap(card));
        scene.setFill(Color.TRANSPARENT);
        if (AlertUtil.class.getResource("/com/auction/client/view/styles.css") != null) {
            scene.getStylesheets().add(AlertUtil.class.getResource("/com/auction/client/view/styles.css").toExternalForm());
        }
        com.auction.client.service.AppStyleManager.applyCurrentStyle(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static Stage createBaseStage(String title) {
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.setTitle(title == null ? "" : title);
        try {
            dialog.getIcons().add(new Image(AlertUtil.class.getResourceAsStream(Config.LOGO_PATH)));
        } catch (Exception e) {
            // Ignored safely
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        Window owner = findOwnerWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        return dialog;
    }

    private static StackPane wrap(Node content) {
        StackPane wrapper = new StackPane(content);
        wrapper.setPadding(new Insets(18));
        wrapper.setStyle("-fx-background-color: transparent;");
        return wrapper;
    }

    private static VBox baseCard(Alert.AlertType type, String title, String message) {
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(26, 30, 28, 30));
        card.setMinWidth(360);
        card.setPrefWidth(390);
        card.setMaxWidth(410);
        card.setStyle("-fx-background-color: -app-card;"
                + " -fx-background-radius: 28px;"
                + " -fx-border-radius: 28px;"
                + " -fx-border-color: -app-border;"
                + " -fx-border-width: 1.3px;"
                + " -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-16, 26, 0, 0, 9);"
                + " -fx-font-family: 'DM Sans';");

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(64, 64);
        iconCircle.setPrefSize(64, 64);
        iconCircle.setMaxSize(64, 64);
        iconCircle.setStyle("-fx-background-color: " + softIconBackground(type) + ";"
                + " -fx-background-radius: 24px;"
                + " -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-12, 12, 0, 0, 4);");

        Label icon = new Label(iconText(type));
        icon.setStyle("-fx-text-fill: " + iconColor(type) + "; -fx-font-size: 34px; -fx-font-weight: 900;");
        iconCircle.getChildren().add(icon);

        Label titleLabel = new Label(title);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(330);
        titleLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 24px; -fx-font-weight: 900;");

        Label messageLabel = new Label(message);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(325);
        messageLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 15px; -fx-font-weight: 500; -fx-line-spacing: 2px;");

        card.getChildren().setAll(iconCircle, titleLabel, messageLabel);
        return card;
    }

    private static Button primaryButton(String text) {
        Button button = new Button(text);
        button.setMinWidth(190);
        button.setPrefWidth(210);
        button.setMaxWidth(230);
        button.setMinHeight(44);
        button.setPrefHeight(44);
        button.setStyle("-fx-background-color: -fx-accent;"
                + " -fx-text-fill: white;"
                + " -fx-font-size: 15px;"
                + " -fx-font-weight: 900;"
                + " -fx-background-radius: 22px;"
                + " -fx-padding: 10px 26px;"
                + " -fx-cursor: hand;"
                + " -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-24, 12, 0, 0, 4);");
        return button;
    }

    private static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setMinWidth(104);
        button.setMinHeight(42);
        button.setStyle("-fx-background-color: -app-accent-opacity-08;"
                + " -fx-text-fill: -fx-accent;"
                + " -fx-font-size: 14px;"
                + " -fx-font-weight: 800;"
                + " -fx-background-radius: 20px;"
                + " -fx-border-color: -fx-accent;"
                + " -fx-border-radius: 20px;"
                + " -fx-padding: 9px 22px;"
                + " -fx-cursor: hand;");
        return button;
    }

    private static void styleDialogButton(Node node) {
        if (node instanceof Button button) {
            boolean primary = ButtonBar.getButtonData(button) != null && ButtonBar.getButtonData(button).isDefaultButton();
            if (primary || "OK".equalsIgnoreCase(button.getText())) {
                button.setMinWidth(112);
                button.setMinHeight(40);
                button.setStyle("-fx-background-color: -fx-accent;"
                        + " -fx-text-fill: white;"
                        + " -fx-font-size: 14px;"
                        + " -fx-font-weight: 900;"
                        + " -fx-background-radius: 18px;"
                        + " -fx-padding: 9px 24px;"
                        + " -fx-cursor: hand;"
                        + " -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-20, 10, 0, 0, 3);");
            } else {
                button.setMinWidth(92);
                button.setMinHeight(40);
                button.setStyle("-fx-background-color: -app-accent-opacity-08;"
                        + " -fx-text-fill: -fx-accent;"
                        + " -fx-font-size: 13px;"
                        + " -fx-font-weight: 800;"
                        + " -fx-background-radius: 18px;"
                        + " -fx-border-color: -fx-accent;"
                        + " -fx-border-radius: 18px;"
                        + " -fx-padding: 9px 20px;"
                        + " -fx-cursor: hand;");
            }
        }
    }

    private static Window findOwnerWindow() {
        return Window.getWindows()
                .stream()
                .filter(Window::isShowing)
                .filter(window -> window instanceof Stage)
                .findFirst()
                .orElse(null);
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
            default -> "-fx-accent";
        };
    }


    private static String softIconBackground(Alert.AlertType type) {
        boolean isDark = false;
        try {
            isDark = com.auction.client.service.SettingsService.getInstance().getTheme().toLowerCase().contains("dark");
        } catch (Exception e) {
            // Ignored
        }
        if (isDark) {
            return switch (type) {
                case INFORMATION -> "rgba(16, 185, 129, 0.15)";
                case ERROR -> "rgba(239, 68, 68, 0.15)";
                case WARNING -> "rgba(245, 158, 11, 0.15)";
                default -> "rgba(224, 64, 160, 0.15)";
            };
        }
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
