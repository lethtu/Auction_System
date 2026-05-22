package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import com.auction.client.model.User;
import com.auction.client.util.NotificationBellBinder;
import com.auction.client.util.SettingsDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class SettingsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    private static final Preferences prefs = Preferences.userNodeForPackage(SettingsDialog.class);

    @FXML private Button btnHamburger;
    @FXML private Button btnNotificationBell;
    @FXML private Label notificationBadge;
    @FXML private Button btnSettings;
    @FXML private MenuButton userMenuButton;
    @FXML private TextField txtSearch;
    @FXML private StackPane topBarAvatarPane;

    @FXML private CheckBox chkOutbid;
    @FXML private CheckBox chkSound;
    @FXML private CheckBox chkCollapse;
    @FXML private ComboBox<String> cbLang;
    @FXML private ComboBox<String> cbColor;
    @FXML private Label lblStatus;

    @FXML private Button btnSaveSettings;
    @FXML private Button btnResetFilters;
    @FXML private Button btnReloadData;

    @FXML private SidebarController sidebarController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize User Menu Options
        if (User.getFullname() != null) {
            createUserOption("Hello, " + User.getFullname());
        }

        Platform.runLater(() -> updateTopBarAvatar(User.getAvatarUrl()));
        
        // Bind Bell
        if (btnNotificationBell != null && notificationBadge != null) {
            NotificationBellBinder.bind(btnNotificationBell, notificationBadge);
        }

        // Disable search on settings page or just bind enter key to search on main page
        if (txtSearch != null) {
            txtSearch.setOnAction(e -> {
                try {
                    String query = txtSearch.getText();
                    if (query != null && !query.trim().isEmpty()) {
                        // Switch back to MainTemplate and filter
                        MainController.initialHomeFilterMode = "SEARCH:" + query.trim();
                        SceneSwitcher.switchScene(e, "MainTemplate.fxml", 1280, 800);
                    }
                } catch (IOException ex) {
                    logger.error("Error switching page: ", ex);
                }
            });
        }

        // Highlight Settings active button in sidebar
        if (sidebarController != null) {
            sidebarController.setActiveSettings();
        }

        // Initialize Gear button in header to just refresh/do nothing on this page since we're already here
        if (btnSettings != null) {
            btnSettings.setOnAction(e -> {
                lblStatus.setText("You are on the settings page.");
                lblStatus.setStyle("-fx-text-fill: #1a73e8;");
                lblStatus.setVisible(true);
                lblStatus.setManaged(true);
            });
        }

        // Load Prefs
        chkOutbid.setSelected(prefs.getBoolean(SettingsDialog.KEY_OUTBID, true));
        chkSound.setSelected(prefs.getBoolean(SettingsDialog.KEY_SOUND, true));
        chkCollapse.setSelected(prefs.getBoolean(SettingsDialog.KEY_AUTO_COLLAPSE, false));

        cbLang.setItems(FXCollections.observableArrayList("English"));
        cbLang.setValue(prefs.get(SettingsDialog.KEY_LANGUAGE, "English"));

        cbColor.setItems(FXCollections.observableArrayList("Rose Pink (Default)", "Royal Purple", "Emerald Green"));
        cbColor.setValue(prefs.get(SettingsDialog.KEY_ACCENT_COLOR, "Rose Pink (Default)"));
    }

    @FXML
    private void handleToggleSidebar(ActionEvent event) {
        if (sidebarController != null) {
            sidebarController.toggleSidebar();
        }
    }

    @FXML
    private void handleGoToDashboard(MouseEvent event) {
        try {
            javafx.event.EventTarget target = event.getTarget();
            javafx.event.Event eventWrapper = new javafx.event.ActionEvent(target, null);
            SceneSwitcher.switchScene((javafx.event.ActionEvent) eventWrapper, "MainTemplate.fxml", 1280, 800);
        } catch (IOException e) {
            logger.error("Error switching to MainTemplate: ", e);
        }
    }



    @FXML
    private void handleSaveSettings(ActionEvent event) {
        prefs.putBoolean(SettingsDialog.KEY_OUTBID, chkOutbid.isSelected());
        prefs.putBoolean(SettingsDialog.KEY_SOUND, chkSound.isSelected());
        
        boolean oldAutoCollapse = prefs.getBoolean(SettingsDialog.KEY_AUTO_COLLAPSE, false);
        boolean newAutoCollapse = chkCollapse.isSelected();
        prefs.putBoolean(SettingsDialog.KEY_AUTO_COLLAPSE, newAutoCollapse);
        
        prefs.put(SettingsDialog.KEY_LANGUAGE, cbLang.getValue());
        prefs.put(SettingsDialog.KEY_ACCENT_COLOR, cbColor.getValue());

        if (oldAutoCollapse != newAutoCollapse) {
            SidebarController.isSidebarCollapsed = newAutoCollapse;
            if (sidebarController != null) {
                // Instantly apply
                sidebarController.forceCollapse();
            }
        }

        lblStatus.setText("Settings saved successfully!");
        lblStatus.setStyle("-fx-text-fill: #137333; -fx-font-weight: bold;");
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    @FXML
    private void handleResetFilters(ActionEvent event) {
        MainController.initialHomeFilterMode = "ALL";
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText(null);
        alert.setContentText("All search filters on the homepage have been reset to defaults.");
        alert.getDialogPane().setStyle("-fx-font-family: 'DM Sans';");
        alert.showAndWait();
    }

    @FXML
    private void handleReloadData(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText(null);
        alert.setContentText("Data reload from server requested successfully.");
        alert.getDialogPane().setStyle("-fx-font-family: 'DM Sans';");
        alert.showAndWait();
    }

    private void createUserOption(String text) {
        MenuItem accountItem = new MenuItem("My Account");
        MenuItem depositMoney = new MenuItem("Deposit");
        MenuItem logoutItem = new MenuItem("Logout");

        accountItem.setOnAction(event -> {
            try {
                MainController.initialShowAccount = true;
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Error switching to account page: ", e);
            }
        });

        depositMoney.setOnAction(event -> {
            try {
                SceneSwitcher.switchScene(event, "Deposit.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Error switching to deposit page: ", e);
            }
        });

        logoutItem.setOnAction(event -> {
            try {
                User.clearSession();
                SceneSwitcher.switchScene(event, "Login.fxml", 800, 500);
            } catch (IOException e) {
                logger.error("Error logging out: ", e);
            }
        });

        userMenuButton.getItems().clear();
        userMenuButton.getItems().addAll(accountItem, depositMoney, new SeparatorMenuItem(), logoutItem);
    }

    private void updateTopBarAvatar(String avatarUrl) {
        if (topBarAvatarPane == null) return;
        try {
            topBarAvatarPane.getChildren().clear();
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : Config.API_URL + avatarUrl;
                ImageView imgView = new ImageView(new Image(fullUrl, 36, 36, false, true, true));
                imgView.setFitWidth(36);
                imgView.setFitHeight(36);
                imgView.setSmooth(true);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                imgView.setClip(clip);
                topBarAvatarPane.getChildren().add(imgView);
            } else {
                Label icon = new Label("\uE7FD");
                icon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: white;");
                topBarAvatarPane.getChildren().add(icon);
            }
        } catch (Exception e) {
            logger.warn("Cannot update avatar on top bar: {}", e.getMessage());
        }
    }
}
