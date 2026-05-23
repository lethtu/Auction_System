package com.auction.client.controller;

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
    @FXML private CheckBox chkCollapse;
    @FXML private CheckBox chkRememberPage;

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

    private SettingsService settingsService;

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

        setupDisabledControls();
        setupPasswordSection();
        loadSettingsToUI();

        sliderVolume.valueProperty().addListener((obs, oldVal, newVal) ->
                lblVolumeValue.setText(String.format("%.0f%%", newVal.doubleValue())));
    }

    private void setupDisabledControls() {
        Tooltip comingSoon = new Tooltip("Coming soon");

        chkRememberPage.setDisable(true);
        chkRememberPage.setTooltip(comingSoon);

        chkShowEnded.setDisable(true);
        chkShowEnded.setTooltip(comingSoon);

        chkSortActive.setDisable(true);
        chkSortActive.setTooltip(comingSoon);

        chkCompactCards.setDisable(true);
        chkCompactCards.setTooltip(comingSoon);
    }

    private void setupPasswordSection() {
        if (passwordSetBox == null) {
            return;
        }

        boolean needsPassword = !User.isPasswordSet();
        passwordSetBox.setVisible(needsPassword);
        passwordSetBox.setManaged(needsPassword);
    }

    private void loadSettingsToUI() {
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

        chkCollapse.setSelected(settingsService.isAutoCollapseSidebar());
        chkRememberPage.setSelected(settingsService.isRememberLastPage());

        chkConfirmBid.setSelected(settingsService.isConfirmBeforeBid());
        chkQuickBid.setSelected(settingsService.isQuickBidEnabled());
        txtHighBidWarning.setText(String.valueOf(settingsService.getHighBidWarningThreshold()));
        txtDefaultIncrement.setText(String.valueOf(settingsService.getDefaultBidIncrement()));

        chkShowEnded.setSelected(settingsService.isShowEndedAuctions());
        chkSortActive.setSelected(settingsService.isSortActiveFirst());
        chkShowCountdown.setSelected(settingsService.isShowCountdownTimer());
        chkCompactCards.setSelected(settingsService.isCompactCards());
    }

    @FXML
    public void handleSaveSettings(ActionEvent event) {
        long highBidWarning;
        long defaultInc;
        try {
            highBidWarning = Long.parseLong(txtHighBidWarning.getText().trim());
            defaultInc = Long.parseLong(txtDefaultIncrement.getText().trim());
        } catch (NumberFormatException e) {
            showToast("Invalid number format for Auction Preferences.", true);
            return;
        }

        boolean oldAutoCollapse = settingsService.isAutoCollapseSidebar();

        settingsService.setNotificationsEnabled(chkNotificationsEnabled.isSelected());
        settingsService.setOutbidNotificationEnabled(chkOutbid.isSelected());
        settingsService.setEndingSoonNotificationEnabled(chkEndingSoon.isSelected());
        settingsService.setAuctionResultNotificationEnabled(chkAuctionResult.isSelected());

        settingsService.setSoundEnabled(chkSound.isSelected());
        settingsService.setSoundVolume(sliderVolume.getValue() / 100.0);

        settingsService.setTheme(cbTheme.getValue());
        settingsService.setPrimaryColor(cbColor.getValue());
        settingsService.setAutoCollapseSidebar(chkCollapse.isSelected());
        settingsService.setRememberLastPage(chkRememberPage.isSelected());

        settingsService.setConfirmBeforeBid(chkConfirmBid.isSelected());
        settingsService.setQuickBidEnabled(chkQuickBid.isSelected());
        settingsService.setHighBidWarningThreshold(highBidWarning);
        settingsService.setDefaultBidIncrement(defaultInc);

        settingsService.setShowEndedAuctions(chkShowEnded.isSelected());
        settingsService.setSortActiveFirst(chkSortActive.isSelected());
        settingsService.setShowCountdownTimer(chkShowCountdown.isSelected());
        settingsService.setCompactCards(chkCompactCards.isSelected());

        if (oldAutoCollapse != chkCollapse.isSelected() && sidebarController != null) {
            sidebarController.toggleSidebar();
        }

        if (event.getSource() instanceof Node sourceNode && sourceNode.getScene() != null) {
            sourceNode.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);
            AppStyleManager.applyCurrentStyle(sourceNode.getScene());
        }

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

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/users/" + userId + "/set-password"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

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
    public void handleResetFilters(ActionEvent event) {
        MainController.initialHomeFilterMode = "ALL";
        showToast("Filters reset to default.", false);
    }

    @FXML
    public void handleReloadData(ActionEvent event) {
        showToast("Synchronizing data from server...", false);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setStyle("-fx-font-family: 'DM Sans';");
        alert.showAndWait();
    }

    private void showToast(String message, boolean isError) {
        if (toastContainer == null) {
            return;
        }

        Label toast = new Label(message);
        toast.setStyle(
                "-fx-background-color: " + (isError ? "#fee2e2" : "#dcfce7") + "; " +
                "-fx-text-fill: " + (isError ? "#991b1b" : "#166534") + "; " +
                "-fx-padding: 12px 24px; " +
                "-fx-background-radius: 8px; " +
                "-fx-font-family: 'DM Sans'; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);"
        );

        toastContainer.getChildren().add(toast);

        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> toastContainer.getChildren().remove(toast));
        delay.play();
    }
}
