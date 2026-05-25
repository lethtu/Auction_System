package com.auction.client.controller;

import javafx.application.Platform;
import com.auction.client.Config;
import com.auction.client.model.User;
import com.auction.client.model.audio.SoundEvent;
import com.auction.client.service.AppStyleManager;
import com.auction.client.service.SettingsService;
import com.auction.client.service.SoundManager;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @FXML private SidebarController sidebarController;
    @FXML private TopbarController topbarController;
    @FXML private VBox toastContainer;

    @FXML private CheckBox chkNotificationsEnabled;
    @FXML private CheckBox chkOutbid;
    @FXML private CheckBox chkEndingSoon;
    @FXML private CheckBox chkAuctionResult;

    @FXML private CheckBox chkSound;
    @FXML private Slider sliderVolume;
    @FXML private Label lblVolumeValue;

    @FXML private ComboBox<String> cbTheme;
    @FXML private ComboBox<String> cbColor;

    @FXML private CheckBox chkConfirmBid;
    @FXML private TextField txtHighBidWarning;

    @FXML private CheckBox chkShowEnded;
    @FXML private CheckBox chkSortActive;
    @FXML private CheckBox chkShowCountdown;

    @FXML private VBox passwordSetBox;
    @FXML private VBox passwordChangeBox;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtChangeNewPassword;
    @FXML private PasswordField txtChangeConfirmPassword;
    @FXML private Button btnSetPassword;
    @FXML private Button btnChangePassword;

    private SettingsService settingsService;
    private boolean loadingSettings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settingsService = SettingsService.getInstance();

        if (topbarController != null) {
            topbarController.setSearchVisible(false);
            topbarController.highlightSettingsButton();
            if (sidebarController != null) {
                topbarController.setSidebarController(sidebarController);
            }
            if (topbarController.getTxtSearch() != null) {
                topbarController.getTxtSearch().setOnAction(e -> {
                    try {
                        String query = topbarController.getTxtSearch().getText();
                        if (query != null && !query.trim().isEmpty()) {
                            MainController.initialHomeFilterMode = "SEARCH:" + query.trim();
                            SceneSwitcher.switchScene(e, "MainTemplate.fxml", 1280, 800);
                        }
                    } catch (IOException ex) {
                        logger.error("Error switching page: ", ex);
                    }
                });
            }
        }

        if (sidebarController != null) {
            sidebarController.setActiveSettings();
        }

        setupPasswordSection();
        loadSettingsToUI();
        setupSettingInteractions();

        sliderVolume.valueProperty().addListener((obs, oldVal, newVal) ->
                lblVolumeValue.setText(String.format("%.0f%%", newVal.doubleValue())));
    }

    private void setupSettingInteractions() {
        chkNotificationsEnabled.selectedProperty().addListener((obs, oldVal, enabled) -> {
            chkOutbid.setDisable(!enabled);
            chkEndingSoon.setDisable(!enabled);
            chkAuctionResult.setDisable(!enabled);
        });

        chkSound.selectedProperty().addListener((obs, oldVal, enabled) -> {
            sliderVolume.setDisable(!enabled);
        });

        cbTheme.setOnAction(event -> handleImmediateAppearanceChange());
        cbColor.setOnAction(event -> handleImmediateAppearanceChange());

        boolean notificationsEnabled = chkNotificationsEnabled.isSelected();
        chkOutbid.setDisable(!notificationsEnabled);
        chkEndingSoon.setDisable(!notificationsEnabled);
        chkAuctionResult.setDisable(!notificationsEnabled);

        boolean soundEnabled = chkSound.isSelected();
        sliderVolume.setDisable(!soundEnabled);
    }

    private void handleImmediateAppearanceChange() {
        if (loadingSettings || cbTheme.getValue() == null || cbColor.getValue() == null) {
            return;
        }
        settingsService.setTheme(cbTheme.getValue());
        settingsService.setPrimaryColor(cbColor.getValue());
        settingsService.flush();
        applyCurrentStyle();
    }

    private void setupPasswordSection() {
        boolean needsPassword = !User.isPasswordSet();
        if (passwordSetBox != null) {
            passwordSetBox.setVisible(needsPassword);
            passwordSetBox.setManaged(needsPassword);
        }
        if (passwordChangeBox != null) {
            passwordChangeBox.setVisible(!needsPassword);
            passwordChangeBox.setManaged(!needsPassword);
        }
    }

    private void loadSettingsToUI() {
        loadingSettings = true;
        chkNotificationsEnabled.setSelected(settingsService.isNotificationsEnabled());
        chkOutbid.setSelected(settingsService.isOutbidNotificationEnabled());
        chkEndingSoon.setSelected(settingsService.isEndingSoonNotificationEnabled());
        chkAuctionResult.setSelected(settingsService.isAuctionResultNotificationEnabled());

        chkSound.setSelected(settingsService.isSoundEnabled());
        sliderVolume.setValue(settingsService.getSoundVolume() * 100.0);
        lblVolumeValue.setText(String.format("%.0f%%", sliderVolume.getValue()));

        cbTheme.getItems().setAll("Light", "Dark");
        cbTheme.setValue(settingsService.getTheme());

        cbColor.getItems().setAll("Rose Pink (Default)", "Purple", "Emerald", "Blue", "Orange");
        cbColor.setValue(settingsService.getPrimaryColor());

        chkConfirmBid.setSelected(settingsService.isConfirmBeforeBid());
        txtHighBidWarning.setText(String.valueOf(settingsService.getHighBidWarningThreshold()));

        chkShowEnded.setSelected(settingsService.isShowEndedAuctions());
        chkSortActive.setSelected(settingsService.isSortActiveFirst());
        chkShowCountdown.setSelected(settingsService.isShowCountdownTimer());
        loadingSettings = false;
    }

    @FXML
    public void handleSaveSettings(ActionEvent event) {
        long highBidWarning;
        try {
            highBidWarning = Long.parseLong(txtHighBidWarning.getText().trim());
        } catch (NumberFormatException e) {
            showToast("Invalid high bid warning amount.", true);
            return;
        }
        if (highBidWarning < 0) {
            showToast("High bid warning must be 0 or greater.", true);
            return;
        }

        settingsService.setNotificationsEnabled(chkNotificationsEnabled.isSelected());
        settingsService.setOutbidNotificationEnabled(chkOutbid.isSelected());
        settingsService.setEndingSoonNotificationEnabled(chkEndingSoon.isSelected());
        settingsService.setAuctionResultNotificationEnabled(chkAuctionResult.isSelected());

        settingsService.setSoundEnabled(chkSound.isSelected());
        settingsService.setSoundVolume(sliderVolume.getValue() / 100.0);

        settingsService.setTheme(cbTheme.getValue());
        settingsService.setPrimaryColor(cbColor.getValue());

        settingsService.setConfirmBeforeBid(chkConfirmBid.isSelected());
        settingsService.setHighBidWarningThreshold(highBidWarning);

        settingsService.setShowEndedAuctions(chkShowEnded.isSelected());
        settingsService.setSortActiveFirst(chkSortActive.isSelected());
        settingsService.setShowCountdownTimer(chkShowCountdown.isSelected());
        settingsService.flush();

        applyCurrentStyle();

        showToast("Settings saved successfully.", false);
    }

    @FXML
    public void handleTestSound(ActionEvent event) {
        if (!chkSound.isSelected()) {
            showToast("Enable sounds to test audio.", true);
            return;
        }

        double vol = sliderVolume.getValue() / 100.0;
        boolean result = SoundManager.getInstance().playSound(SoundEvent.NOTIFICATION, vol, true);
        if (result) {
            showToast("Notification sound played.", false);
        } else {
            showToast("The notification sound file is missing or invalid.", true);
        }
    }

    @FXML
    private void handleSetPassword(ActionEvent event) {
        String newPass = txtNewPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (newPass == null || newPass.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Mat khau khong duoc de trong.");
            return;
        }

        if (newPass.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Error", "Mat khau phai chua it nhat 6 ky tu.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Xac nhan mat khau khong khop.");
            return;
        }

        try {
            Integer userId = User.getId();
            String token = User.getSessionToken();

            JSONObject payload = new JSONObject();
            payload.put("password", newPass);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/users/" + userId + "/set-password"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()));
            if (token != null) {
                builder.header("X-Auth-Token", token);
            }
            HttpRequest req = builder.build();

            HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                User.setPasswordSet(true);
                setupPasswordSection();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Password set successfully.");
            } else {
                JSONObject errObj = new JSONObject(resp.body());
                String errMsg = errObj.optString("message", "Could not save password.");
                showAlert(Alert.AlertType.ERROR, "Error", errMsg);
            }
        } catch (Exception e) {
            logger.error("Failed to set password: ", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot connect to server: " + e.getMessage());
        }
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        String oldPass = txtCurrentPassword.getText();
        String newPass = txtChangeNewPassword.getText();
        String confirmPass = txtChangeConfirmPassword.getText();

        if (oldPass == null || oldPass.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Current password cannot be empty.");
            return;
        }

        if (newPass == null || newPass.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Error", "New password must be at least 6 characters long.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Error", "New password confirmation does not match.");
            return;
        }

        try {
            Integer userId = User.getId();
            String token = User.getSessionToken();

            JSONObject payload = new JSONObject();
            payload.put("oldPassword", oldPass);
            payload.put("newPassword", newPass);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/users/" + userId + "/change-password"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()));
            if (token != null) {
                builder.header("X-Auth-Token", token);
            }

            HttpResponse<String> resp = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JSONObject body = new JSONObject(resp.body());
            int status = body.optInt("status", resp.statusCode());
            if (resp.statusCode() == 200 && status == 200) {
                txtCurrentPassword.clear();
                txtChangeNewPassword.clear();
                txtChangeConfirmPassword.clear();
                showAlert(Alert.AlertType.INFORMATION, "Success", body.optString("message", "Password changed successfully."));
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", body.optString("message", "Could not change password."));
            }
        } catch (Exception e) {
            logger.error("Failed to change password: ", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot connect to server: " + e.getMessage());
        }
    }

    @FXML
    public void handleResetFilters(ActionEvent event) {
        settingsService.resetDashboardDisplayDefaults();
        settingsService.flush();
        chkShowEnded.setSelected(settingsService.isShowEndedAuctions());
        chkSortActive.setSelected(settingsService.isSortActiveFirst());
        chkShowCountdown.setSelected(settingsService.isShowCountdownTimer());
        MainController.initialHomeFilterMode = "ALL";
        showToast("Dashboard filters reset to default.", false);
    }

    @FXML
    public void handleReloadData(ActionEvent event) {
        showToast("Synchronizing data from server...", false);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.API_URL + "/api/auctions/all"))
                .GET()
                .build();
        HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        logger.warn("Settings data sync failed", error);
                        showToast("Data sync failed. Please check the server connection.", true);
                    } else if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        MainController.initialHomeFilterMode = "ALL";
                        showToast("Latest auction data synced.", false);
                    } else {
                        showToast("Data sync failed with server status " + response.statusCode() + ".", true);
                    }
                }));
    }

    private void applyCurrentStyle() {
        Node anchor = toastContainer != null ? toastContainer : btnChangePassword;
        if (anchor != null && anchor.getScene() != null) {
            anchor.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);
            AppStyleManager.applyCurrentStyle(anchor.getScene());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showAlert(type, title, content));
            return;
        }
        com.auction.client.util.AlertUtil.show(type, title, content);
    }

    private void showToast(String message, boolean isError) {
        if (toastContainer == null) {
            return;
        }

        Label toast = new Label(message);
        toast.getStyleClass().addAll("settings-toast",
                isError ? "settings-toast-error" : "settings-toast-success");

        toastContainer.getChildren().add(toast);

        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> toastContainer.getChildren().remove(toast));
        delay.play();
    }
}
