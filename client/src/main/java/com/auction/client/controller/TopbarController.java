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
            btnSettings.setStyle("-fx-background-color: rgba(224, 64, 160, 0.15); -fx-background-radius: 22px; -fx-cursor: hand;");
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
