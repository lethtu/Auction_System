package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import com.auction.client.model.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import com.auction.client.util.NotificationBellBinder;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;

public class UpToSellerController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(UpToSellerController.class);

    @FXML private CheckBox chkAgree;
    @FXML private Label lblMessage;
    @FXML private Button btnUpgrade;

    @FXML private MenuButton userMenuButton;
    @FXML private TextField txtSearch;

    @FXML private SidebarController sidebarController;
    @FXML private Button btnHamburger;
    
    @FXML private Button btnNotificationBell;
    @FXML private Label notificationBadge;
    @FXML private StackPane topBarAvatarPane;
    @FXML private Button btnSettings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (User.getFullname() != null) {
            createUserOption("Hello, " + User.getFullname());
        }

        
        if (btnNotificationBell != null && notificationBadge != null) {
            NotificationBellBinder.bind(btnNotificationBell, notificationBadge);
        }

        Platform.runLater(() -> updateTopBarAvatar(User.getAvatarUrl()));
        if (btnSettings != null) {
            btnSettings.setOnAction(e -> {
                try {
                    com.auction.client.controller.SceneSwitcher.switchScene(e, "Settings.fxml", 1280, 800);
                } catch (IOException ex) {
                    logger.error("Error switching to Settings.fxml: ", ex);
                }
            });
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
    public void handleUpgrade(ActionEvent event) {
        if (User.getId() == null) {
            showError("Please log in to upgrade!");
            return;
        }

        if (!chkAgree.isSelected()) {
            showError("Please agree to the HY Auction terms of sale!");
            return;
        }

        btnUpgrade.setDisable(true);
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);

        new Thread(() -> {
            try {
                String apiUrl = Config.API_URL + "/api/bidder/up-to-seller?userId=" + User.getId();
                HttpURLConnection conn = (HttpURLConnection) java.net.URI.create(apiUrl).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                InputStream stream = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                Scanner scanner = new Scanner(stream).useDelimiter("\\A");
                String responseStr = scanner.hasNext() ? scanner.next() : "";
                scanner.close();

                JSONObject jsonResponse = new JSONObject(responseStr);

                Platform.runLater(() -> {
                    btnUpgrade.setDisable(false);
                    if (responseCode >= 200 && responseCode < 300 && jsonResponse.optInt("status", 400) == 200) {
                        // Success
                        User.setSession(User.getId(), User.getUsername(), User.getFullname(), User.getEmail(), User.getDob(), User.getPlace_of_birth(), "SELLER", User.getAvatarUrl());

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Upgrade successful");
                        alert.setHeaderText("Congratulations, you are now a Seller!");
                        alert.setContentText("Selling features are now unlocked. You can now list your own products.");

                        DialogPane dialogPane = alert.getDialogPane();
                        dialogPane.setStyle("-fx-font-family: 'DM Sans'; -fx-background-color: #fcf8ff; -fx-border-color: -fx-accent; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px;");
                        alert.showAndWait();

                        try {
                            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                        } catch (Exception ex) {
                            logger.error("Error switching scenes after upgrade: ", ex);
                        }
                    } else {
                        showError(jsonResponse.optString("message", "Upgrade failed or account is already a SELLER!"));
                    }
                });

            } catch (Exception e) {
                logger.error("Error when upgrading to seller: ", e);
                Platform.runLater(() -> {
                    btnUpgrade.setDisable(false);
                    showError("Server connection error during upgrade!");
                });
            }
        }).start();
    }

    @FXML
    public void handleGoBack(MouseEvent event) {
        try {
            ActionEvent actionEvent = new ActionEvent(event.getSource(), event.getTarget());
            SceneSwitcher.switchScene(actionEvent, "MainTemplate.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Error going back to main page: ", e);
        }
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Error going back to main page: ", e);
        }
    }

    private void showError(String message) {
        lblMessage.setText(message);
        lblMessage.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
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