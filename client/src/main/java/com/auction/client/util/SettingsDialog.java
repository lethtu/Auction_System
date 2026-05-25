package com.auction.client.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.prefs.Preferences;

public final class SettingsDialog {

    private static final Preferences prefs = Preferences.userNodeForPackage(SettingsDialog.class);

    // Settings Keys
    public static final String KEY_SOUND = "sound_notifications";
    public static final String KEY_OUTBID = "outbid_notifications";
    public static final String KEY_AUTO_COLLAPSE = "auto_collapse_sidebar";
    public static final String KEY_ACCENT_COLOR = "accent_color";
    public static final String KEY_LANGUAGE = "app_language";

    private SettingsDialog() {
    }

    public static void show(Window ownerWindow) {
        show(ownerWindow, null, null);
    }

    public static void show(Window ownerWindow, Runnable onResetFilters, Runnable onReloadData) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(ownerWindow);
        dialog.setTitle("System Configuration");
        
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Load styles.css first so variables are available
        java.net.URL stylesUrl = SettingsDialog.class.getResource("/com/auction/client/view/styles.css");
        if (stylesUrl != null) {
            pane.getStylesheets().add(stylesUrl.toExternalForm());
        }
        com.auction.client.service.AppStyleManager.applyCurrentStyle(pane);
        
        // Load Styles
        pane.setStyle("-fx-background-color: -app-card;"
                + " -fx-border-color: -app-border;"
                + " -fx-border-width: 1.5px;"
                + " -fx-border-radius: 20px;"
                + " -fx-background-radius: 20px;"
                + " -fx-padding: 0px;"
                + " -fx-font-family: 'DM Sans';");

        // Custom Titlebar/Header
        VBox mainContainer = new VBox();
        mainContainer.setSpacing(0);
        mainContainer.setMinWidth(480);
        mainContainer.setPrefWidth(480);

        // Header Section
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(24, 28, 20, 28));
        header.setStyle("-fx-background-color: linear-gradient(to right, -app-accent-opacity-05, rgba(124, 82, 170, 0.05));"
                + " -fx-border-color: -app-border;"
                + " -fx-border-width: 0 0 1px 0;");

        Label gearIcon = new Label("\ue8b8");
        gearIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 32px; -fx-text-fill: -fx-accent;");

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label("App Configuration");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -app-text;");
        Label descLabel = new Label("Customize your auction experience and interface.");
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-muted;");
        titleBox.getChildren().addAll(titleLabel, descLabel);

        header.getChildren().addAll(gearIcon, titleBox);
        mainContainer.getChildren().add(header);

        // Content Section
        VBox content = new VBox(20);
        content.setPadding(new Insets(24, 28, 28, 28));

        // Group 1: Notifications
        VBox grpNotifications = new VBox(10);
        Label lblGrpNotif = new Label("NOTIFICATIONS & SOUNDS");
        lblGrpNotif.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -fx-accent; -fx-padding: 0 0 4 0;");
        
        CheckBox chkOutbid = new CheckBox("Receive notification when outbid");
        chkOutbid.setSelected(prefs.getBoolean(KEY_OUTBID, true));
        chkOutbid.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text; -fx-cursor: hand;");

        CheckBox chkSound = new CheckBox("Play notification sounds");
        chkSound.setSelected(prefs.getBoolean(KEY_SOUND, true));
        chkSound.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text; -fx-cursor: hand;");

        grpNotifications.getChildren().addAll(lblGrpNotif, chkOutbid, chkSound);
        content.getChildren().add(grpNotifications);

        // Group 2: UI Experience
        VBox grpUi = new VBox(12);
        Label lblGrpUi = new Label("INTERFACE & EXPERIENCE");
        lblGrpUi.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -fx-accent; -fx-padding: 4 0 4 0;");

        // Language Dropdown
        HBox rowLang = new HBox(10);
        rowLang.setAlignment(Pos.CENTER_LEFT);
        Label lblLang = new Label("App language:");
        lblLang.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text;");
        Region spacerLang = new Region();
        HBox.setHgrow(spacerLang, Priority.ALWAYS);
        
        ComboBox<String> cbLang = new ComboBox<>();
        cbLang.getItems().addAll("English");
        cbLang.setValue(prefs.get(KEY_LANGUAGE, "English"));
        cbLang.setStyle("-fx-background-color: -app-input-bg; -fx-border-color: -app-border; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand;");

        rowLang.getChildren().addAll(lblLang, spacerLang, cbLang);

        // Accent Color Dropdown
        HBox rowColor = new HBox(10);
        rowColor.setAlignment(Pos.CENTER_LEFT);
        Label lblColor = new Label("Primary color tone:");
        lblColor.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text;");
        Region spacerColor = new Region();
        HBox.setHgrow(spacerColor, Priority.ALWAYS);

        ComboBox<String> cbColor = new ComboBox<>();
        cbColor.getItems().addAll("Rose Pink (Default)", "Royal Purple", "Emerald Green");
        cbColor.setValue(prefs.get(KEY_ACCENT_COLOR, "Rose Pink (Default)"));
        cbColor.setStyle("-fx-background-color: -app-input-bg; -fx-border-color: -app-border; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand;");

        rowColor.getChildren().addAll(lblColor, spacerColor, cbColor);

        grpUi.getChildren().addAll(lblGrpUi, rowLang, rowColor);
        content.getChildren().add(grpUi);

        // Group 3: Quick Actions (Optional)
        if (onResetFilters != null || onReloadData != null) {
            VBox grpActions = new VBox(10);
            Label lblGrpActions = new Label("QUICK ACTIONS");
            lblGrpActions.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -fx-accent; -fx-padding: 4 0 4 0;");
            
            HBox actionButtons = new HBox(12);
            actionButtons.setAlignment(Pos.CENTER_LEFT);
            
            if (onResetFilters != null) {
                Button btnReset = new Button("Reset Filters");
                btnReset.setStyle("-fx-background-color: -app-accent-opacity-08; -fx-text-fill: -fx-accent; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-border-color: -fx-accent; -fx-border-radius: 8px; -fx-padding: 6px 14px; -fx-cursor: hand;");
                btnReset.setOnAction(e -> {
                    onResetFilters.run();
                    dialog.setResult(ButtonType.CANCEL);
                    dialog.close();
                });
                actionButtons.getChildren().add(btnReset);
            }
            
            if (onReloadData != null) {
                Button btnReload = new Button("Reload Data");
                btnReload.setStyle("-fx-background-color: -app-accent-opacity-08; -fx-text-fill: -fx-accent; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-border-color: -fx-accent; -fx-border-radius: 8px; -fx-padding: 6px 14px; -fx-cursor: hand;");
                btnReload.setOnAction(e -> {
                    onReloadData.run();
                    dialog.setResult(ButtonType.CANCEL);
                    dialog.close();
                });
                actionButtons.getChildren().add(btnReload);
            }
            
            grpActions.getChildren().addAll(lblGrpActions, actionButtons);
            content.getChildren().add(grpActions);
        }

        mainContainer.getChildren().add(content);
        pane.setContent(mainContainer);

        // Style the buttons
        Button btnOk = (Button) pane.lookupButton(ButtonType.OK);
        btnOk.setText("Save Settings");
        btnOk.setStyle("-fx-background-color: -fx-accent;"
                + " -fx-text-fill: white;"
                + " -fx-font-weight: bold;"
                + " -fx-background-radius: 12px;"
                + " -fx-padding: 8px 20px;"
                + " -fx-cursor: hand;"
                + " -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-20, 8, 0, 0, 3);");
 
        Button btnCancel = (Button) pane.lookupButton(ButtonType.CANCEL);
        btnCancel.setText("Cancel");
        btnCancel.setStyle("-fx-background-color: -app-accent-opacity-08;"
                + " -fx-text-fill: -fx-accent;"
                + " -fx-font-weight: bold;"
                + " -fx-background-radius: 12px;"
                + " -fx-border-color: -fx-accent;"
                + " -fx-border-radius: 12px;"
                + " -fx-padding: 8px 20px;"
                + " -fx-cursor: hand;");

        // Handle save on OK
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                prefs.putBoolean(KEY_OUTBID, chkOutbid.isSelected());
                prefs.putBoolean(KEY_SOUND, chkSound.isSelected());
                
                prefs.put(KEY_LANGUAGE, cbLang.getValue());
                prefs.put(KEY_ACCENT_COLOR, cbColor.getValue());
            }
            return button;
        });

        dialog.showAndWait();
    }

    public static boolean isSoundEnabled() {
        return com.auction.client.service.SettingsService.getInstance().isSoundEnabled();
    }

    public static boolean isOutbidNotificationEnabled() {
        return com.auction.client.service.SettingsService.getInstance().isOutbidNotificationEnabled();
    }

}
