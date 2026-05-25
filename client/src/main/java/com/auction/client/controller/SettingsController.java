package com.auction.client.controller;

import com.auction.client.Config;
import com.auction.client.HttpClientSingleton;
import com.auction.client.model.User;
import com.auction.client.model.audio.SoundEvent;
import com.auction.client.service.AppStyleManager;
import com.auction.client.service.SettingsService;
import com.auction.client.service.SoundManager;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.SettingsDialog;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
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

    @FXML private CheckBox chkNotificationsEnabled;
    @FXML private CheckBox chkOutbid;
    @FXML private CheckBox chkEndingSoon;
    @FXML private CheckBox chkAuctionResult;

    @FXML private CheckBox chkSound;
    @FXML private Slider sliderVolume;
    @FXML private Label lblVolumeValue;

    @FXML private CheckBox chkCollapse;
    @FXML private CheckBox chkRememberPage;
    @FXML private ComboBox<String> cbLang;
    @FXML private ComboBox<String> cbTheme;
    @FXML private ComboBox<String> cbColor;
    @FXML private Label lblStatus;

    @FXML private CheckBox chkConfirmBid;
    @FXML private CheckBox chkQuickBid;
    @FXML private TextField txtHighBidWarning;
    @FXML private TextField txtDefaultIncrement;

    @FXML private CheckBox chkShowEnded;
    @FXML private CheckBox chkSortActive;
    @FXML private CheckBox chkShowCountdown;
    @FXML private CheckBox chkCompactCards;

    @FXML private VBox passwordSetBox;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnSetPassword;

    @FXML private VBox passwordChangeBox;
    @FXML private PasswordField txtOldPassword;
    @FXML private PasswordField txtChangeNewPassword;
    @FXML private PasswordField txtChangeConfirmPassword;
    @FXML private Button btnChangePassword;

    @FXML private Button btnSaveSettings;
    @FXML private Button btnResetFilters;
    @FXML private Button btnReloadData;
    @FXML private VBox toastContainer;

    @FXML private SidebarController sidebarController;
    @FXML private TopbarController topbarController;

    private final HttpClient httpClient = HttpClientSingleton.getInstance().getHttpClient();
    private SettingsService settingsService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settingsService = SettingsService.getInstance();
        configureTopbarAndSidebar();
        setupDisabledControls();
        loadSettingsToUI();
        setupVolumePreview();
        updatePasswordPanels();

        if (btnSaveSettings != null) {
            Platform.runLater(() -> {
                if (btnSaveSettings.getScene() != null) {
                    btnSaveSettings.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);
                    AppStyleManager.applyCurrentStyle(btnSaveSettings.getScene());
            AppStyleManager.applyCurrentStyleToOpenWindows();
                }
            });
        }
    }

    private void configureTopbarAndSidebar() {
        if (topbarController != null) {
            topbarController.setSearchVisible(false);
            topbarController.highlightSettingsButton();
            if (sidebarController != null) {
                topbarController.setSidebarController(sidebarController);
            }
        }

        if (topbarController != null && topbarController.getTxtSearch() != null) {
            topbarController.getTxtSearch().setOnAction(e -> {
                try {
                    String query = topbarController.getTxtSearch().getText();
                    if (query != null && !query.trim().isEmpty()) {
                        MainController.initialHomeFilterMode = "SEARCH:" + query.trim();
                        SceneSwitcher.switchScene(e, "MainTemplate.fxml", 1280, 800);
                    }
                } catch (IOException ex) {
                    logger.error("Error switching page", ex);
                }
            });
        }

        if (sidebarController != null) {
            sidebarController.setActiveSettings();
        }
    }

    private void setupDisabledControls() {
        Tooltip comingSoon = new Tooltip("Coming soon");
        setDisabledWithTooltip(chkRememberPage, comingSoon);
        setDisabledWithTooltip(chkShowEnded, comingSoon);
        setDisabledWithTooltip(chkSortActive, comingSoon);
        setDisabledWithTooltip(chkCompactCards, comingSoon);
    }

    private void setDisabledWithTooltip(CheckBox checkBox, Tooltip tooltip) {
        if (checkBox != null) {
            checkBox.setDisable(true);
            checkBox.setTooltip(tooltip);
        }
    }

    private void loadSettingsToUI() {
        if (chkNotificationsEnabled != null) chkNotificationsEnabled.setSelected(settingsService.isNotificationsEnabled());
        if (chkOutbid != null) chkOutbid.setSelected(settingsService.isOutbidNotificationEnabled());
        if (chkEndingSoon != null) chkEndingSoon.setSelected(settingsService.isEndingSoonNotificationEnabled());
        if (chkAuctionResult != null) chkAuctionResult.setSelected(settingsService.isAuctionResultNotificationEnabled());

        if (chkSound != null) chkSound.setSelected(settingsService.isSoundEnabled());
        if (sliderVolume != null) sliderVolume.setValue(settingsService.getSoundVolume() * 100.0);
        updateVolumeLabel();

        if (chkCollapse != null) chkCollapse.setSelected(settingsService.isAutoCollapseSidebar());
        if (chkRememberPage != null) chkRememberPage.setSelected(settingsService.isRememberLastPage());

        if (cbLang != null) {
            cbLang.setItems(FXCollections.observableArrayList("English"));
            cbLang.setValue("English");
        }
        if (cbTheme != null) {
            cbTheme.setItems(FXCollections.observableArrayList("Light", "Dark"));
            cbTheme.setValue(settingsService.getTheme());
        }
        if (cbColor != null) {
            cbColor.setItems(FXCollections.observableArrayList("Rose Pink (Default)", "Purple", "Emerald", "Blue", "Orange"));
            cbColor.setValue(settingsService.getPrimaryColor());
        }

        if (chkConfirmBid != null) chkConfirmBid.setSelected(settingsService.isConfirmBeforeBid());
        if (chkQuickBid != null) chkQuickBid.setSelected(settingsService.isQuickBidEnabled());
        if (txtHighBidWarning != null) txtHighBidWarning.setText(String.valueOf(settingsService.getHighBidWarningThreshold()));
        if (txtDefaultIncrement != null) txtDefaultIncrement.setText(String.valueOf(settingsService.getDefaultBidIncrement()));

        if (chkShowEnded != null) chkShowEnded.setSelected(settingsService.isShowEndedAuctions());
        if (chkSortActive != null) chkSortActive.setSelected(settingsService.isSortActiveFirst());
        if (chkShowCountdown != null) chkShowCountdown.setSelected(settingsService.isShowCountdownTimer());
        if (chkCompactCards != null) chkCompactCards.setSelected(settingsService.isCompactCards());
    }

    private void setupVolumePreview() {
        if (sliderVolume != null) {
            sliderVolume.valueProperty().addListener((obs, oldValue, newValue) -> updateVolumeLabel());
        }
    }

    private void updateVolumeLabel() {
        if (lblVolumeValue != null && sliderVolume != null) {
            lblVolumeValue.setText(String.format("%.0f%%", sliderVolume.getValue()));
        }
    }

    @FXML
    private void handleSaveSettings(ActionEvent event) {
        long highBidWarning = parsePositiveLong(txtHighBidWarning, settingsService.getHighBidWarningThreshold(), "High bid warning");
        if (highBidWarning < 0) return;
        long defaultIncrement = parsePositiveLong(txtDefaultIncrement, settingsService.getDefaultBidIncrement(), "Default increment");
        if (defaultIncrement < 0) return;

        boolean oldAutoCollapse = settingsService.isAutoCollapseSidebar();
        boolean newAutoCollapse = chkCollapse != null && chkCollapse.isSelected();

        if (chkNotificationsEnabled != null) settingsService.setNotificationsEnabled(chkNotificationsEnabled.isSelected());
        if (chkOutbid != null) settingsService.setOutbidNotificationEnabled(chkOutbid.isSelected());
        if (chkEndingSoon != null) settingsService.setEndingSoonNotificationEnabled(chkEndingSoon.isSelected());
        if (chkAuctionResult != null) settingsService.setAuctionResultNotificationEnabled(chkAuctionResult.isSelected());

        if (chkSound != null) settingsService.setSoundEnabled(chkSound.isSelected());
        if (sliderVolume != null) settingsService.setSoundVolume(sliderVolume.getValue() / 100.0);

        if (cbTheme != null && cbTheme.getValue() != null) settingsService.setTheme(cbTheme.getValue());
        if (cbColor != null && cbColor.getValue() != null) settingsService.setPrimaryColor(cbColor.getValue());
        settingsService.setAutoCollapseSidebar(newAutoCollapse);
        if (chkRememberPage != null) settingsService.setRememberLastPage(chkRememberPage.isSelected());

        if (chkConfirmBid != null) settingsService.setConfirmBeforeBid(chkConfirmBid.isSelected());
        if (chkQuickBid != null) settingsService.setQuickBidEnabled(chkQuickBid.isSelected());
        settingsService.setHighBidWarningThreshold(highBidWarning);
        settingsService.setDefaultBidIncrement(defaultIncrement);

        if (chkShowEnded != null) settingsService.setShowEndedAuctions(chkShowEnded.isSelected());
        if (chkSortActive != null) settingsService.setSortActiveFirst(chkSortActive.isSelected());
        if (chkShowCountdown != null) settingsService.setShowCountdownTimer(chkShowCountdown.isSelected());
        if (chkCompactCards != null) settingsService.setCompactCards(chkCompactCards.isSelected());

        if (oldAutoCollapse != newAutoCollapse && sidebarController != null) {
            sidebarController.toggleSidebar();
        }

        if (btnSaveSettings != null && btnSaveSettings.getScene() != null) {
            btnSaveSettings.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);
            AppStyleManager.applyCurrentStyle(btnSaveSettings.getScene());
            AppStyleManager.applyCurrentStyleToOpenWindows();
        }

        showStatus("Settings saved successfully!", false);
        showToast("Settings saved successfully!", false);
    }

    private long parsePositiveLong(TextField field, long fallback, String label) {
        if (field == null) {
            return fallback;
        }
        try {
            long value = Long.parseLong(field.getText().trim());
            if (value < 0) {
                showToast(label + " must be greater than or equal to 0.", true);
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            showToast(label + " must be a valid number.", true);
            return -1;
        }
    }

    @FXML
    private void handleTestSound(ActionEvent event) {
        if (chkSound != null && !chkSound.isSelected()) {
            showToast("Enable sounds to test audio.", true);
            return;
        }

        double volume = sliderVolume == null ? settingsService.getSoundVolume() : sliderVolume.getValue() / 100.0;
        boolean played = SoundManager.getInstance().playSound(SoundEvent.NOTIFICATION, volume, true);
        showToast(played ? "Notification sound played." : "The notification sound file is missing or invalid.", !played);
    }

    @FXML
    private void handleResetFilters(ActionEvent event) {
        MainController.initialHomeFilterMode = "ALL";
        showToast("Home filters reset to default.", false);
        AlertUtil.show(Alert.AlertType.INFORMATION, "Settings", "All search filters on the homepage have been reset to defaults.");
    }

    @FXML
    private void handleReloadData(ActionEvent event) {
        showToast("Data reload requested successfully.", false);
        AlertUtil.show(Alert.AlertType.INFORMATION, "Settings", "Data reload from server requested successfully.");
    }

    @FXML
    private void handleSetPassword(ActionEvent event) {
        String newPassword = safeText(txtNewPassword);
        String confirmPassword = safeText(txtConfirmPassword);

        if (newPassword.isBlank()) {
            AlertUtil.showError("Password cannot be empty.");
            return;
        }
        if (newPassword.length() < 6) {
            AlertUtil.showError("Password must be at least 6 characters long.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            AlertUtil.showError("Confirm password does not match.");
            return;
        }

        setButtonLoading(btnSetPassword, true);
        new Thread(() -> submitSetPassword(newPassword)).start();
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        String oldPassword = safeText(txtOldPassword);
        String newPassword = safeText(txtChangeNewPassword);
        String confirmPassword = safeText(txtChangeConfirmPassword);

        if (oldPassword.isBlank()) {
            AlertUtil.showError("Current password cannot be empty.");
            return;
        }
        if (newPassword.isBlank()) {
            AlertUtil.showError("New password cannot be empty.");
            return;
        }
        if (newPassword.length() < 6) {
            AlertUtil.showError("New password must be at least 6 characters long.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            AlertUtil.showError("Confirm password does not match.");
            return;
        }

        setButtonLoading(btnChangePassword, true);
        new Thread(() -> submitChangePassword(oldPassword, newPassword)).start();
    }

    private void submitSetPassword(String newPassword) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("password", newPassword);

            HttpResponse<String> response = sendPasswordRequest("/set-password", payload);
            ApiResult result = parseApiResult(response);

            Platform.runLater(() -> {
                setButtonLoading(btnSetPassword, false);
                if (result.success()) {
                    User.setPasswordSet(true);
                    clearSetPasswordFields();
                    updatePasswordPanels();
                    AlertUtil.showSuccess("Password set successfully. You can now sign in with your email or username.");
                } else {
                    AlertUtil.showError(result.message());
                }
            });
        } catch (Exception e) {
            logger.error("Failed to set password", e);
            Platform.runLater(() -> {
                setButtonLoading(btnSetPassword, false);
                AlertUtil.showError("Could not connect to the server while setting the password.");
            });
        }
    }

    private void submitChangePassword(String oldPassword, String newPassword) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("oldPassword", oldPassword);
            payload.put("newPassword", newPassword);

            HttpResponse<String> response = sendPasswordRequest("/change-password", payload);
            ApiResult result = parseApiResult(response);

            Platform.runLater(() -> {
                setButtonLoading(btnChangePassword, false);
                if (result.success()) {
                    clearChangePasswordFields();
                    AlertUtil.showSuccess("Password changed successfully.");
                } else {
                    AlertUtil.showError(result.message());
                }
            });
        } catch (Exception e) {
            logger.error("Failed to change password", e);
            Platform.runLater(() -> {
                setButtonLoading(btnChangePassword, false);
                AlertUtil.showError("Could not connect to the server while changing the password.");
            });
        }
    }

    private HttpResponse<String> sendPasswordRequest(String actionPath, JSONObject payload) throws Exception {
        Integer userId = User.getId();
        if (userId == null || userId <= 0) {
            throw new IllegalStateException("User session is missing.");
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(Config.API_URL + "/api/users/" + userId + actionPath))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()));

        String token = User.getSessionToken();
        if (token != null && !token.isBlank()) {
            builder.header("X-Auth-Token", token);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private ApiResult parseApiResult(HttpResponse<String> response) {
        if (response == null) {
            return new ApiResult(false, "No response from server.");
        }

        String message = response.statusCode() >= 200 && response.statusCode() < 300
                ? "Operation completed successfully."
                : "The server rejected this request.";

        try {
            JSONObject json = new JSONObject(response.body() == null ? "{}" : response.body());
            int status = json.optInt("status", response.statusCode());
            message = json.optString("message", message);
            return new ApiResult(response.statusCode() >= 200 && response.statusCode() < 300 && status == 200, message);
        } catch (Exception ignored) {
            return new ApiResult(response.statusCode() >= 200 && response.statusCode() < 300, message);
        }
    }

    private void updatePasswordPanels() {
        if (passwordSetBox == null || passwordChangeBox == null) {
            return;
        }
        boolean passwordSet = User.isPasswordSet();
        passwordSetBox.setVisible(!passwordSet);
        passwordSetBox.setManaged(!passwordSet);
        passwordChangeBox.setVisible(passwordSet);
        passwordChangeBox.setManaged(passwordSet);
    }

    private void clearSetPasswordFields() {
        if (txtNewPassword != null) txtNewPassword.clear();
        if (txtConfirmPassword != null) txtConfirmPassword.clear();
    }

    private void clearChangePasswordFields() {
        if (txtOldPassword != null) txtOldPassword.clear();
        if (txtChangeNewPassword != null) txtChangeNewPassword.clear();
        if (txtChangeConfirmPassword != null) txtChangeConfirmPassword.clear();
    }

    private void setButtonLoading(Button button, boolean loading) {
        if (button == null) {
            return;
        }
        Platform.runLater(() -> button.setDisable(loading));
    }

    private String safeText(PasswordField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private void showStatus(String message, boolean isError) {
        if (lblStatus == null) {
            return;
        }
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + (isError ? "#ff6b6b" : "-fx-accent") + "; -fx-font-weight: bold;");
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    private void showToast(String message, boolean isError) {
        if (toastContainer == null) {
            showStatus(message, isError);
            return;
        }

        String toneColor = isError ? "#ff6b6b" : "-fx-accent";
        Label toast = new Label(message);
        toast.setStyle(
                "-fx-background-color: -app-card; " +
                "-fx-border-color: " + toneColor + "; " +
                "-fx-border-width: 1.4px; " +
                "-fx-border-radius: 12px; " +
                "-fx-text-fill: -app-text; " +
                "-fx-padding: 12px 24px; " +
                "-fx-background-radius: 12px; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14px; " +
                "-fx-effect: dropshadow(three-pass-box, -app-accent-opacity-16, 10, 0, 0, 4);"
        );

        toastContainer.getChildren().add(toast);
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> toastContainer.getChildren().remove(toast));
        delay.play();
    }

    private record ApiResult(boolean success, String message) {
    }
}
