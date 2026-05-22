package com.auction.client.controller;

import com.auction.client.service.AppStyleManager;
import com.auction.client.service.SettingsService;
import com.auction.client.service.SoundManager;
import com.auction.client.model.audio.SoundEvent;
import com.auction.client.util.NotificationBellBinder;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private Button btnHamburger;
    @FXML private Button btnNotificationBell;
    @FXML private Label notificationBadge;
    @FXML private Button btnSettings;
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

    private SettingsService settingsService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settingsService = SettingsService.getInstance();
        
        if (btnNotificationBell != null && notificationBadge != null) {
            NotificationBellBinder.bind(btnNotificationBell, notificationBadge);
        }

        // Disable unsupported settings
        setupDisabledControls();

        // Load values
        loadSettingsToUI();

        // Setup volume slider listener
        sliderVolume.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblVolumeValue.setText(String.format("%.0f%%", newVal.doubleValue()));
        });
    }

    private void setupDisabledControls() {
        Tooltip comingSoon = new Tooltip("Coming soon");
        
        // Features without backend support yet
        chkRememberPage.setDisable(true);
        chkRememberPage.setTooltip(comingSoon);
        
        chkShowEnded.setDisable(true);
        chkShowEnded.setTooltip(comingSoon);
        
        chkSortActive.setDisable(true);
        chkSortActive.setTooltip(comingSoon);
        
        chkCompactCards.setDisable(true);
        chkCompactCards.setTooltip(comingSoon);
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
        // Validate numbers
        long highBidWarning = settingsService.getHighBidWarningThreshold();
        long defaultInc = settingsService.getDefaultBidIncrement();
        try {
            highBidWarning = Long.parseLong(txtHighBidWarning.getText());
            defaultInc = Long.parseLong(txtDefaultIncrement.getText());
        } catch (NumberFormatException e) {
            showToast("Invalid number format for Auction Preferences.", true);
            return;
        }

        // Save Notifications
        settingsService.setNotificationsEnabled(chkNotificationsEnabled.isSelected());
        settingsService.setOutbidNotificationEnabled(chkOutbid.isSelected());
        settingsService.setEndingSoonNotificationEnabled(chkEndingSoon.isSelected());
        settingsService.setAuctionResultNotificationEnabled(chkAuctionResult.isSelected());

        // Save Sounds
        settingsService.setSoundEnabled(chkSound.isSelected());
        settingsService.setSoundVolume(sliderVolume.getValue() / 100.0);

        // Save Appearance
        settingsService.setTheme(cbTheme.getValue());
        settingsService.setPrimaryColor(cbColor.getValue());
        settingsService.setAutoCollapseSidebar(chkCollapse.isSelected());
        settingsService.setRememberLastPage(chkRememberPage.isSelected());

        // Save Auction Prefs
        settingsService.setConfirmBeforeBid(chkConfirmBid.isSelected());
        settingsService.setQuickBidEnabled(chkQuickBid.isSelected());
        settingsService.setHighBidWarningThreshold(highBidWarning);
        settingsService.setDefaultBidIncrement(defaultInc);

        // Save Dashboard Display
        settingsService.setShowEndedAuctions(chkShowEnded.isSelected());
        settingsService.setSortActiveFirst(chkSortActive.isSelected());
        settingsService.setShowCountdownTimer(chkShowCountdown.isSelected());
        settingsService.setCompactCards(chkCompactCards.isSelected());

        // Apply theme immediately - MUST update scene fill first, then stylesheet
        if (btnSettings.getScene() != null) {
            btnSettings.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);
            AppStyleManager.applyCurrentStyle(btnSettings.getScene());
        }

        showToast("Settings saved successfully!", false);
    }

    @FXML
    public void handleTestSound(ActionEvent event) {
        if (!chkSound.isSelected()) {
            showToast("Enable sounds to test audio.", true);
            return;
        }

        double vol = sliderVolume.getValue() / 100.0;
        boolean result = SoundManager.getInstance().playSound(SoundEvent.NOTIFICATION, vol, true);
        if (!result) {
            showToast("The notification sound file is missing or invalid.", true);
        } else {
            showToast("Notification sound played.", false);
        }
    }

    @FXML
    public void handleToggleSidebar(ActionEvent event) {
        // Ignored for settings page
    }

    @FXML
    public void handleGoToDashboard(javafx.scene.input.MouseEvent event) {
        try {
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleMinimize(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    public void handleMaximize(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    public void handleResetFilters(ActionEvent event) {
        showToast("Filters reset to default.", false);
    }

    @FXML
    public void handleReloadData(ActionEvent event) {
        showToast("Synchronizing data from server...", false);
    }

    private void showToast(String message, boolean isError) {
        if (toastContainer == null) return;

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
        delay.setOnFinished(e -> {
            toastContainer.getChildren().remove(toast);
        });
        delay.play();
    }
}