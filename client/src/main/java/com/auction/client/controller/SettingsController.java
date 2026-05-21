package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.model.User;
import com.auction.client.util.NotificationBellBinder;
import com.auction.client.util.SettingsDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

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
    @FXML private Button btnDashboard;
    @FXML private TextField txtSearch;

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
            createUserOption("Chào, " + User.getFullname());
        }

        if (User.getRole() != null && User.getRole().equalsIgnoreCase("seller")) {
            btnDashboard.setVisible(true);
            btnDashboard.setManaged(true);
        }

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
                    logger.error("Lỗi tìm kiếm chuyển trang: ", ex);
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
                lblStatus.setText("Bạn đang ở trang cài đặt.");
                lblStatus.setStyle("-fx-text-fill: #1a73e8;");
                lblStatus.setVisible(true);
                lblStatus.setManaged(true);
            });
        }

        // Load Prefs
        chkOutbid.setSelected(prefs.getBoolean(SettingsDialog.KEY_OUTBID, true));
        chkSound.setSelected(prefs.getBoolean(SettingsDialog.KEY_SOUND, true));
        chkCollapse.setSelected(prefs.getBoolean(SettingsDialog.KEY_AUTO_COLLAPSE, false));

        cbLang.setItems(FXCollections.observableArrayList("Tiếng Việt", "English"));
        cbLang.setValue(prefs.get(SettingsDialog.KEY_LANGUAGE, "Tiếng Việt"));

        cbColor.setItems(FXCollections.observableArrayList("Rose Pink (Mặc định)", "Royal Purple", "Emerald Green"));
        cbColor.setValue(prefs.get(SettingsDialog.KEY_ACCENT_COLOR, "Rose Pink (Mặc định)"));
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
            logger.error("Lỗi chuyển về MainTemplate: ", e);
        }
    }

    @FXML
    private void handleGoToDashboard(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1280, 800);
        } catch (IOException e) {
            logger.error("Lỗi chuyển về SellerDashboard: ", e);
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

        lblStatus.setText("Đã lưu các thiết lập thành công!");
        lblStatus.setStyle("-fx-text-fill: #137333; -fx-font-weight: bold;");
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    @FXML
    private void handleResetFilters(ActionEvent event) {
        MainController.initialHomeFilterMode = "ALL";
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Cài đặt");
        alert.setHeaderText(null);
        alert.setContentText("Đã đặt lại tất cả bộ lọc tìm kiếm trên trang chủ về mặc định.");
        alert.getDialogPane().setStyle("-fx-font-family: 'DM Sans';");
        alert.showAndWait();
    }

    @FXML
    private void handleReloadData(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Cài đặt");
        alert.setHeaderText(null);
        alert.setContentText("Đã yêu cầu tải lại dữ liệu từ máy chủ thành công.");
        alert.getDialogPane().setStyle("-fx-font-family: 'DM Sans';");
        alert.showAndWait();
    }

    private void createUserOption(String text) {
        MenuItem accountItem = new MenuItem("Tài Khoản Của Tôi");
        MenuItem depositMoney = new MenuItem("Nạp tiền");
        MenuItem logoutItem = new MenuItem("Đăng Xuất");

        accountItem.setOnAction(event -> {
            try {
                MainController.initialShowAccount = true;
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Lỗi khi chuyển sang trang tài khoản: ", e);
            }
        });

        depositMoney.setOnAction(event -> {
            try {
                SceneSwitcher.switchScene(event, "Deposit.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Lỗi khi chuyển sang trang nạp tiền: ", e);
            }
        });

        logoutItem.setOnAction(event -> {
            try {
                User.clearSession();
                SceneSwitcher.switchScene(event, "Login.fxml", 800, 500);
            } catch (IOException e) {
                logger.error("Lỗi khi đăng xuất: ", e);
            }
        });

        userMenuButton.getItems().clear();
        userMenuButton.getItems().addAll(accountItem, depositMoney, new SeparatorMenuItem(), logoutItem);
    }
}
