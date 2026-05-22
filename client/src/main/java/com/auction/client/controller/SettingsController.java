package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.util.SettingsDialog;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class SettingsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    private static final Preferences prefs = Preferences.userNodeForPackage(SettingsDialog.class);

    @FXML private CheckBox chkOutbid;
    @FXML private CheckBox chkSound;
    @FXML private CheckBox chkCollapse;
    @FXML private ComboBox<String> cbLang;
    @FXML private ComboBox<String> cbColor;
    @FXML private Label lblStatus;

    @FXML private VBox passwordSetBox;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnSetPassword;

    @FXML private Button btnSaveSettings;
    @FXML private Button btnResetFilters;
    @FXML private Button btnReloadData;

    @FXML private SidebarController sidebarController;
    @FXML private TopbarController topbarController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (topbarController != null) {
            topbarController.setSearchVisible(false);
            topbarController.highlightSettingsButton();
            if (sidebarController != null) {
                topbarController.setSidebarController(sidebarController);
            }
        }

        // Search on settings page: bind enter key to switch to main page and search
        if (topbarController != null && topbarController.getTxtSearch() != null) {
            topbarController.getTxtSearch().setOnAction(e -> {
                try {
                    String query = topbarController.getTxtSearch().getText();
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

        // Load Prefs
        chkOutbid.setSelected(prefs.getBoolean(SettingsDialog.KEY_OUTBID, true));
        chkSound.setSelected(prefs.getBoolean(SettingsDialog.KEY_SOUND, true));
        chkCollapse.setSelected(prefs.getBoolean(SettingsDialog.KEY_AUTO_COLLAPSE, false));

        cbLang.setItems(FXCollections.observableArrayList("English"));
        cbLang.setValue(prefs.get(SettingsDialog.KEY_LANGUAGE, "English"));

        cbColor.setItems(FXCollections.observableArrayList("Rose Pink (Default)", "Royal Purple", "Emerald Green"));
        cbColor.setValue(prefs.get(SettingsDialog.KEY_ACCENT_COLOR, "Rose Pink (Default)"));

        if (com.auction.client.model.User.isPasswordSet()) {
            passwordSetBox.setVisible(false);
            passwordSetBox.setManaged(false);
        } else {
            passwordSetBox.setVisible(true);
            passwordSetBox.setManaged(true);
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
            if (sidebarController != null) {
                sidebarController.toggleSidebar();
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

    @FXML
    private void handleSetPassword(ActionEvent event) {
        String newPass = txtNewPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (newPass == null || newPass.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Mật khẩu không được để trống.");
            return;
        }

        if (newPass.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Error", "Mật khẩu phải chứa ít nhất 6 ký tự.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Xác nhận mật khẩu không khớp.");
            return;
        }

        // Call backend API
        try {
            int userId = com.auction.client.model.User.getId();
            String token = com.auction.client.model.User.getSessionToken();

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            org.json.JSONObject payload = new org.json.JSONObject();
            payload.put("password", newPass);

            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(com.auction.client.Config.API_URL + "/api/users/" + userId + "/set-password"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                com.auction.client.model.User.setPasswordSet(true);
                passwordSetBox.setVisible(false);
                passwordSetBox.setManaged(false);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Thiết lập mật khẩu thành công! Bạn có thể sử dụng mật khẩu này từ bây giờ.");
            } else {
                org.json.JSONObject errObj = new org.json.JSONObject(resp.body());
                String errMsg = errObj.optString("message", "Đã xảy ra lỗi khi lưu mật khẩu.");
                showAlert(Alert.AlertType.ERROR, "Error", errMsg);
            }
        } catch (Exception e) {
            logger.error("Failed to set password: ", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Không thể kết nối đến máy chủ: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setStyle("-fx-font-family: 'DM Sans';");
        alert.showAndWait();
    }
}