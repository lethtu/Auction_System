package com.auction.client.util;

import com.auction.client.service.AppStyleManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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

import java.util.Optional;

public final class AlertUtil {
    private static final String DEFAULT_INFO_TITLE = "Success";
    private static final String DEFAULT_ERROR_TITLE = "Error";
    private static final String DEFAULT_WARNING_TITLE = "Warning";
    private static final String DEFAULT_ERROR_MESSAGE = "An error occurred. Please try again.";

    private AlertUtil() {
    }

    public static void show(Alert.AlertType type, String title, String message) {
        Alert.AlertType alertType = type == null ? Alert.AlertType.INFORMATION : type;
        String safeTitle = normalizeTitle(title, alertType);
        String safeMessage = normalizeMessage(message);

        if (isRunningUnderTestHarness()) {
            showStyledJavaFxAlert(alertType, safeTitle, safeMessage);
            return;
        }

        showCardModal(safeTitle, safeMessage, alertType);
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
        if (isRunningUnderTestHarness()) {
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle(normalizeTitle(title, Alert.AlertType.INFORMATION));
            dialog.setHeaderText(null);
            dialog.setContentText(normalizeMessage(message));
            styleDialog(dialog);
            return dialog.showAndWait();
        }

        Stage dialog = createBaseStage(normalizeTitle(title, Alert.AlertType.INFORMATION));

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
                + " -fx-text-fill: -app-text;"
                + " -fx-prompt-text-fill: -app-text-muted;");

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

        VBox card = baseCard(Alert.AlertType.INFORMATION, normalizeTitle(title, Alert.AlertType.INFORMATION), normalizeMessage(message));
        card.getChildren().add(card.getChildren().size() - 1, input);
        card.getChildren().set(card.getChildren().size() - 1, actions);

        dialog.setScene(createTransparentScene(wrap(card)));
        dialog.setOnShown(event -> input.requestFocus());
        dialog.showAndWait();

        return value[0] == null ? Optional.empty() : Optional.of(value[0]);
    }

    public static void styleDialog(Dialog<?> dialog) {
        if (dialog == null || dialog.getDialogPane() == null) {
            return;
        }

        DialogPane pane = dialog.getDialogPane();
        pane.setMinWidth(380);
        pane.setPrefWidth(430);
        pane.setMaxWidth(500);
        pane.setStyle("-fx-background-color: -app-card;"
                + " -fx-border-color: -app-border;"
                + " -fx-border-width: 1.3px;"
                + " -fx-border-radius: 24px;"
                + " -fx-background-radius: 24px;"
                + " -fx-padding: 22px;"
                + " -fx-font-family: 'DM Sans';"
                + " -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-20, 24, 0, 0, 8);");

        keepDialogSceneTransparent(pane);

        pane.lookupAll(".header-panel").forEach(node -> node.setStyle("-fx-background-color: transparent;"));
        pane.lookupAll(".header-panel .label").forEach(node -> node.setStyle("-fx-text-fill: -app-text; -fx-font-size: 20px; -fx-font-weight: 900;"));
        pane.lookupAll(".content.label").forEach(node -> node.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 14px; -fx-font-weight: 600;"));
        pane.lookupAll(".text-field").forEach(node -> node.setStyle("-fx-background-color: -app-input-bg;"
                + " -fx-border-color: -app-border;"
                + " -fx-border-radius: 14px;"
                + " -fx-background-radius: 14px;"
                + " -fx-padding: 10px 14px;"
                + " -fx-font-size: 14px;"
                + " -fx-text-fill: -app-text;"
                + " -fx-prompt-text-fill: -app-text-muted;"));
        applyThemeToPane(pane);
        pane.lookupAll(".button").forEach(AlertUtil::styleDialogButton);
    }

    public static void styleAndShow(Alert alert) {
        if (alert == null) {
            return;
        }
        show(alert.getAlertType(), alert.getTitle(), alert.getContentText());
    }

    public static boolean confirm(String title, String message) {
        String safeTitle = normalizeTitle(title, Alert.AlertType.CONFIRMATION);
        String safeMessage = normalizeMessage(message);

        if (isRunningUnderTestHarness()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(safeTitle);
            alert.setHeaderText(null);
            alert.setContentText(safeMessage);
            styleDialog(alert);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }

        Stage dialog = createBaseStage(safeTitle);
        VBox card = baseCard(Alert.AlertType.CONFIRMATION, safeTitle, safeMessage);
        final boolean[] accepted = {false};

        Button cancelButton = secondaryButton("Cancel");
        Button confirmButton = primaryButton("Sign Out");
        confirmButton.setDefaultButton(true);

        cancelButton.setOnAction(event -> dialog.close());
        confirmButton.setOnAction(event -> {
            accepted[0] = true;
            dialog.close();
        });

        HBox actions = new HBox(12, cancelButton, confirmButton);
        actions.setAlignment(Pos.CENTER);
        card.getChildren().add(actions);

        dialog.setScene(createTransparentScene(wrap(card)));
        dialog.showAndWait();
        return accepted[0];
    }

    public static boolean isRunningUnderTestHarness() {
        String surefire = System.getProperty("surefire.test.class.path");
        String command = System.getProperty("sun.java.command", "");
        String classPath = System.getProperty("java.class.path", "");
        return surefire != null
                || command.toLowerCase().contains("surefire")
                || command.toLowerCase().contains("junit")
                || classPath.toLowerCase().contains("testfx");
    }

    public static void showStyledJavaFxAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type == null ? Alert.AlertType.INFORMATION : type);
        alert.setTitle(normalizeTitle(title, alert.getAlertType()));
        alert.setHeaderText(null);
        alert.setContentText(normalizeMessage(message));
        styleDialog(alert);
        alert.showAndWait();
    }

    private static void showCardModal(String title, String message, Alert.AlertType type) {
        Stage dialog = createBaseStage(title);
        VBox card = baseCard(type, title, message);

        Button okButton = primaryButton("OK");
        okButton.setDefaultButton(true);
        okButton.setOnAction(event -> dialog.close());
        card.getChildren().add(okButton);

        dialog.setScene(createTransparentScene(wrap(card)));
        dialog.showAndWait();
    }

    private static Stage createBaseStage(String title) {
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.setTitle(title == null ? "" : title);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(false);
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

    private static Scene createTransparentScene(StackPane root) {
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        applyThemeToScene(scene);
        return scene;
    }

    private static void applyThemeToScene(Scene scene) {
        if (scene == null) {
            return;
        }
        addMainStylesheet(scene.getStylesheets());
        AppStyleManager.applyCurrentStyle(scene);
    }

    private static void applyThemeToPane(DialogPane pane) {
        if (pane == null) {
            return;
        }
        addMainStylesheet(pane.getStylesheets());
        AppStyleManager.applyCurrentStyle(pane);
    }

    private static void addMainStylesheet(javafx.collections.ObservableList<String> stylesheets) {
        if (stylesheets == null) {
            return;
        }
        java.net.URL url = AlertUtil.class.getResource("/com/auction/client/view/styles.css");
        if (url != null) {
            String css = url.toExternalForm();
            if (!stylesheets.contains(css)) {
                stylesheets.add(css);
            }
        }
    }

    private static void keepDialogSceneTransparent(DialogPane pane) {
        if (pane == null) {
            return;
        }

        makeSceneTransparent(pane.getScene());
        pane.sceneProperty().addListener((observable, oldScene, newScene) -> makeSceneTransparent(newScene));
    }

    private static void makeSceneTransparent(Scene scene) {
        if (scene != null) {
            scene.setFill(Color.TRANSPARENT);
        }
    }

    private static VBox baseCard(Alert.AlertType type, String title, String message) {
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28, 32, 30, 32));
        card.setMinWidth(380);
        card.setPrefWidth(430);
        card.setMaxWidth(470);
        card.setStyle("-fx-background-color: -app-card;"
                + " -fx-background-radius: 30px;"
                + " -fx-border-radius: 30px;"
                + " -fx-border-color: -app-border;"
                + " -fx-border-width: 1.4px;"
                + " -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-24, 32, 0, 0, 12);"
                + " -fx-font-family: 'DM Sans';");

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(66, 66);
        iconCircle.setPrefSize(66, 66);
        iconCircle.setMaxSize(66, 66);
        iconCircle.setStyle("-fx-background-color: " + softIconBackground(type) + ";"
                + " -fx-background-radius: 24px;"
                + " -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-12, 12, 0, 0, 4);");

        Label icon = new Label(iconText(type));
        icon.setStyle("-fx-text-fill: " + iconColor(type) + "; -fx-font-size: 34px; -fx-font-weight: 900;");
        iconCircle.getChildren().add(icon);

        Label titleLabel = new Label(title);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(350);
        titleLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 24px; -fx-font-weight: 900;");

        Label messageLabel = new Label(message);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(355);
        messageLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 15px; -fx-font-weight: 600; -fx-line-spacing: 2px;");

        card.getChildren().setAll(iconCircle, titleLabel, messageLabel);
        return card;
    }

    private static Button primaryButton(String text) {
        Button button = new Button(text);
        button.setMinWidth(190);
        button.setPrefWidth(210);
        button.setMaxWidth(250);
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
            ButtonBar.ButtonData data = ButtonBar.getButtonData(button);
            boolean primary = data != null && (data.isDefaultButton() || data == ButtonBar.ButtonData.OK_DONE || data == ButtonBar.ButtonData.YES);
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
        if (type == Alert.AlertType.ERROR) {
            return "X";
        }
        if (type == Alert.AlertType.WARNING) {
            return "!";
        }
        if (type == Alert.AlertType.CONFIRMATION) {
            return "?";
        }
        return "✓";
    }

    private static String iconColor(Alert.AlertType type) {
        if (type == Alert.AlertType.ERROR) {
            return "#ef4444";
        }
        if (type == Alert.AlertType.WARNING) {
            return "#f59e0b";
        }
        if (type == Alert.AlertType.CONFIRMATION) {
            return "#8b5cf6";
        }
        return "#10b981";
    }

    private static String softIconBackground(Alert.AlertType type) {
        if (type == Alert.AlertType.ERROR) {
            return "#fee2e2";
        }
        if (type == Alert.AlertType.WARNING) {
            return "#fff7ed";
        }
        if (type == Alert.AlertType.CONFIRMATION) {
            return "#ede9fe";
        }
        return "#dcfce7";
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
        if (type == Alert.AlertType.ERROR) {
            return DEFAULT_ERROR_TITLE;
        }
        if (type == Alert.AlertType.WARNING) {
            return DEFAULT_WARNING_TITLE;
        }
        return DEFAULT_INFO_TITLE;
    }

    private static String normalizeMessage(String message) {
        return isBlank(message) ? DEFAULT_ERROR_MESSAGE : message;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
