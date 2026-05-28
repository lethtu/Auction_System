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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.auction.client.Config;
import com.auction.client.util.CacheManager;
import com.auction.client.util.BalanceDisplayBinder;
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
    @FXML private Label topbarBalanceValue;
    @FXML private Button topbarBalanceToggle;
    @FXML private Button btnSettings;
    @FXML private MenuButton userMenuButton;
    @FXML private StackPane topBarAvatarPane;
 
    private SidebarController sidebarController;
 
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (User.getId() != null) {
            createUserOption("Hello, " + User.getFullname());
        } else {
            createGuestOption();
        }
 
        if (btnNotificationBell != null && notificationBadge != null) {
            NotificationBellBinder.bind(btnNotificationBell, notificationBadge);
        }

        BalanceDisplayBinder.bindAvailableBalance(topbarBalanceValue, topbarBalanceToggle);
 
        if (btnSettings != null) {
            btnSettings.setOnAction(e -> {
                if (User.getId() == null) {
                    com.auction.client.util.AlertUtil.showError("Access Denied", "Please log in to use this feature.");
                    try {
                        SceneSwitcher.switchScene(e, "Login.fxml", 1100, 700);
                    } catch (IOException ex) {
                        logger.error("Error switching to Login screen!", ex);
                    }
                    return;
                }
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
        MenuItem accountItem = new MenuItem("My Account");
        MenuItem depositMoney = new MenuItem("Deposit");
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setId("menuLogout");
 
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
                handleLogout(event);
            } catch (IOException e) {
                logger.error("Error switching to Login screen!", e);
            }
        });
 
        userMenuButton.getItems().addAll(accountItem, depositMoney, new SeparatorMenuItem(), logoutItem);
    }
 
    private void createGuestOption() {
        if (userMenuButton != null) {
            userMenuButton.getItems().clear();
            MenuItem loginItem = new MenuItem("Login");
            loginItem.setOnAction(event -> {
                try {
                    handleLogout(event);
                } catch (IOException e) {
                    logger.error("Error switching to Login screen!", e);
                }
            });
            userMenuButton.getItems().add(loginItem);
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
                    img = CacheManager.getCachedImage(fullUrl, updatedImage -> {
                        User.setCachedAvatarImage(updatedImage, avatarUrl);
                        topBarAvatarPane.getChildren().setAll(createAvatarImageView(updatedImage));
                    });
                }
                ImageView imgView = createAvatarImageView(img);
                if (fromCache) {
                    topBarAvatarPane.getChildren().add(imgView);
                } else {
                    Label placeholder = createAvatarPlaceholder();
                    topBarAvatarPane.getChildren().add(placeholder);
 
                    final Image loadedImage = img;
                    final String cachedUrl = avatarUrl;
                    if (loadedImage.getProgress() >= 1.0 && !loadedImage.isError()) {
                        User.setCachedAvatarImage(loadedImage, cachedUrl);
                        topBarAvatarPane.getChildren().setAll(imgView);
                        return;
                    }
                    img.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                        if (newProgress.doubleValue() >= 1.0 && !loadedImage.isError()) {
                            User.setCachedAvatarImage(loadedImage, cachedUrl);
                            topBarAvatarPane.getChildren().setAll(imgView);
                        }
                    });
                }
            } else {
                topBarAvatarPane.getChildren().add(createAvatarPlaceholder());
            }
        } catch (Exception e) {
            logger.warn("Cannot update avatar on top bar: {}", e.getMessage());
        }
    }
 
    private ImageView createAvatarImageView(Image img) {
        ImageView imgView = new ImageView(img);
        imgView.setFitWidth(40);
        imgView.setFitHeight(40);
        imgView.setSmooth(true);
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(20, 20, 20);
        imgView.setClip(clip);
        return imgView;
    }

    private Label createAvatarPlaceholder() {
        String name = User.getFullname() != null && !User.getFullname().isBlank()
                ? User.getFullname()
                : User.getUsername();
        String initials = "?";
        if (name != null && !name.isBlank()) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length > 1) {
                initials = (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
            } else {
                initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            }
        }
        Label label = new Label(initials);
        label.getStyleClass().add("topbar-avatar-initials");
        return label;
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
