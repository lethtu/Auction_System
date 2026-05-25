package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.model.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.auction.client.Config;
import com.auction.client.util.NotificationBellBinder;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class TopbarController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(TopbarController.class);

    @FXML private Button btnHamburger;
    @FXML private HBox searchContainer;
    @FXML private TextField txtSearch;
    @FXML private Button btnNotificationBell;
    @FXML private Label notificationBadge;
    @FXML private Button btnSettings;
    @FXML private MenuButton userMenuButton;
    @FXML private StackPane topBarAvatarPane;

    private SidebarController sidebarController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (User.getFullname() != null) {
            createUserOption("Hello, " + User.getFullname());
        }

        if (btnNotificationBell != null && notificationBadge != null) {
            NotificationBellBinder.bind(btnNotificationBell, notificationBadge);
        }

        if (btnSettings != null) {
            btnSettings.setOnAction(e -> {
                try {
                    com.auction.client.controller.SceneSwitcher.switchScene(e, "Settings.fxml", 1280, 800);
                } catch (IOException ex) {
                    logger.error("Error switching to Settings.fxml: ", ex);
                }
            });
        }

        updateTopBarAvatar(User.getAvatarUrl());
    }

    public void setSidebarController(SidebarController sidebarController) {
        this.sidebarController = sidebarController;
    }

    public void highlightSettingsButton() {
        if (btnSettings != null) {
            btnSettings.setStyle("-fx-background-color: -app-accent-opacity-16; -fx-background-radius: 22px; -fx-cursor: hand;");
            if (btnSettings.getGraphic() instanceof Label) {
                ((Label) btnSettings.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: -fx-accent;");
            }
        }
    }

    public TextField getTxtSearch() {
        return txtSearch;
    }

    public void setSearchVisible(boolean visible) {
        if (searchContainer != null) {
            searchContainer.setVisible(visible);
            searchContainer.setManaged(visible);
        }
    }

    private void createUserOption(String text) {
        if (userMenuButton == null) {
            return;
        }

        userMenuButton.getItems().clear();
        userMenuButton.getStyleClass().add("profile-menu-button");

        CustomMenuItem profileHeader = createProfileHeaderItem();
        MenuItem walletItem = createDisabledMenuItem("Wallet: " + formatMenuMoney(User.getCurrentMoney()), "account-menu-wallet");
        MenuItem accountItem = createActionMenuItem("My Account");
        MenuItem depositMoney = createActionMenuItem("Deposit Funds");
        MenuItem settingsItem = createActionMenuItem("Settings");
        MenuItem logoutItem = createActionMenuItem("Logout");
        logoutItem.getStyleClass().add("account-menu-danger");

        accountItem.setOnAction(event -> {
            try {
                MainController.initialShowAccount = true;
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Error switching to account page: ", e);
            }
        });

        depositMoney.setOnAction(event -> switchTo(event, "Deposit.fxml", "deposit page"));
        settingsItem.setOnAction(event -> switchTo(event, "Settings.fxml", "settings page"));
        logoutItem.setOnAction(this::confirmAndLogout);

        userMenuButton.getItems().addAll(
                profileHeader,
                walletItem,
                new SeparatorMenuItem(),
                accountItem,
                depositMoney,
                settingsItem,
                new SeparatorMenuItem(),
                logoutItem);
    }

    private CustomMenuItem createProfileHeaderItem() {
        VBox box = new VBox(3);
        box.getStyleClass().add("account-menu-header");

        Label name = new Label(safeMenuText(User.getFullname(), safeMenuText(User.getUsername(), "BidPop user")));
        name.getStyleClass().add("account-menu-name");

        Label email = new Label(safeMenuText(User.getEmail(), "No email"));
        email.getStyleClass().add("account-menu-email");

        Label role = new Label(safeMenuText(User.getRole(), "user").toUpperCase());
        role.getStyleClass().add("account-menu-role");

        box.getChildren().addAll(name, email, role);
        return new CustomMenuItem(box, false);
    }

    private MenuItem createActionMenuItem(String text) {
        MenuItem item = new MenuItem(text);
        item.getStyleClass().add("account-menu-action");
        return item;
    }

    private MenuItem createDisabledMenuItem(String text, String styleClass) {
        MenuItem item = new MenuItem(text);
        item.setDisable(true);
        item.getStyleClass().add(styleClass);
        return item;
    }

    private String safeMenuText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatMenuMoney(java.math.BigDecimal value) {
        try {
            java.text.DecimalFormatSymbols symbols = java.text.DecimalFormatSymbols.getInstance(new java.util.Locale("vi", "VN"));
            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,##0", symbols);
            return "\u20AB " + formatter.format(value == null ? java.math.BigDecimal.ZERO : value);
        } catch (Exception e) {
            return "\u20AB 0";
        }
    }

    private void switchTo(ActionEvent event, String fxml, String targetName) {
        try {
            SceneSwitcher.switchScene(event, fxml, 1280, 800);
        } catch (IOException e) {
            logger.error("Error switching to {}: ", targetName, e);
        }
    }

    private void confirmAndLogout(ActionEvent event) {
        boolean confirmed = com.auction.client.util.AlertUtil.confirm(
                "Sign out of BidPop?",
                "You can sign in again anytime.");
        if (confirmed) {
            try {
                handleLogout(event);
            } catch (IOException e) {
                logger.error("Error switching to Login screen!", e);
            }
        }
    }

    public void handleLogout(ActionEvent event) throws IOException {
        User.clearSession();
        SceneSwitcher.switchScene(event, "Login.fxml", 1100, 700);
    }

    @FXML
    public void handleToggleSidebar(ActionEvent event) {
        if (sidebarController != null) {
            sidebarController.toggleSidebar();
        }
    }

    @FXML
    public void handleGoToDashboard(MouseEvent event) {
        try {
            ActionEvent actionEvent = new ActionEvent(event.getSource(), event.getTarget());
            SceneSwitcher.switchScene(actionEvent, "MainTemplate.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Error switching to MainTemplate: ", e);
        }
    }

    private String getAvatarInitial() {
        String name = User.getFullname();
        if (name == null || name.isBlank()) {
            name = User.getUsername();
        }
        if (name == null || name.isBlank()) {
            return "U";
        }
        return name.trim().substring(0, 1).toUpperCase();
    }
    public void updateTopBarAvatar(String avatarUrl) {
        if (topBarAvatarPane == null) return;
        try {
            topBarAvatarPane.getChildren().clear();
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                Image img = null;
                boolean fromCache = false;
                if (avatarUrl.equals(User.getCachedAvatarUrl()) && User.getCachedAvatarImage() != null) {
                    img = User.getCachedAvatarImage();
                    fromCache = true;
                } else {
                    String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : Config.API_URL + avatarUrl;
                    fullUrl = Config.applyCacheBuster(fullUrl);
                    img = new Image(fullUrl, 36, 36, false, true, true);
                }
                ImageView imgView = new ImageView(img);
                imgView.setFitWidth(36);
                imgView.setFitHeight(36);
                imgView.setSmooth(true);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                imgView.setClip(clip);
                if (fromCache) {
                    topBarAvatarPane.getChildren().add(imgView);
                } else {
                    Label placeholder = new Label("\uE7FD");
                    placeholder.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: white;");
                    topBarAvatarPane.getChildren().add(placeholder);

                    final Image loadedImage = img;
                    final String cachedUrl = avatarUrl;
                    img.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                        if (newProgress.doubleValue() >= 1.0 && !loadedImage.isError()) {
                            User.setCachedAvatarImage(loadedImage, cachedUrl);
                            topBarAvatarPane.getChildren().setAll(imgView);
                        }
                    });
                }
            } else {
                Label icon = new Label("\uE7FD");
                icon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: white;");
                topBarAvatarPane.getChildren().add(icon);
            }
        } catch (Exception e) {
            logger.warn("Cannot update avatar on top bar: {}", e.getMessage());
        }
    }

    @FXML
    private void handleMinimize(javafx.event.ActionEvent event) {
        SceneSwitcher.handleMinimize(event);
    }

    @FXML
    private void handleMaximize(javafx.event.ActionEvent event) {
        SceneSwitcher.handleMaximize(event);
    }

    @FXML
    private void handleClose(javafx.event.ActionEvent event) {
        SceneSwitcher.handleClose(event);
    }
}
