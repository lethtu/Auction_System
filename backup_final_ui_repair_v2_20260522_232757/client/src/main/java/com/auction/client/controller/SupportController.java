package com.auction.client.controller;


import com.auction.client.util.AlertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.model.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.layout.StackPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.auction.client.Config;
import com.auction.client.util.NotificationBellBinder;

public class SupportController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SupportController.class);

    @FXML private TextField txtEmail;
    @FXML private TextField txtSubject;
    @FXML private TextArea txtAreaMessage;
    @FXML private Label lblMessageStatus;
    @FXML private Button btnSendSupport;

    @FXML private MenuButton userMenuButton;

    @FXML private Button btnNotificationBell;
    @FXML private Label notificationBadge;
    @FXML private Button btnSettings;
    @FXML private TextField txtSearch;

    @FXML private SidebarController sidebarController;
    @FXML private Button btnHamburger;
    @FXML private StackPane topBarAvatarPane;

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

        Platform.runLater(() -> updateTopBarAvatar(User.getAvatarUrl()));


        if (User.getEmail() != null) {
            txtEmail.setText(User.getEmail());
        }

        // Highlight "Support" button in the sidebar
        Platform.runLater(() -> {
            if (sidebarController != null) {
                sidebarController.setActiveSupport();
            }
        });

        // Setup sidebar listener for navigation resets
        if (sidebarController != null) {
            sidebarController.setSidebarListener(new SidebarController.SidebarListener() {
                @Override
                public void onFilterWatchlist(ActionEvent event) {
                    try {
                        MainController.initialShowWatchlist = true;
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (IOException e) {
                        logger.error("Navigation error:", e);
                    }
                }

                @Override
                public void onResetFilter(ActionEvent event) {
                    try {
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (IOException e) {
                        logger.error("Navigation error:", e);
                    }
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
    public void handleGoToDashboard(MouseEvent event) {
        try {
            ActionEvent actionEvent = new ActionEvent(event.getSource(), event.getTarget());
            SceneSwitcher.switchScene(actionEvent, "MainTemplate.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Error switching to MainTemplate: ", e);
        }
    }



    @FXML
    public void handleSendSupport(ActionEvent event) {
        String email = txtEmail.getText().trim();
        String subject = txtSubject.getText().trim();
        String message = txtAreaMessage.getText().trim();

        if (email.isEmpty() || subject.isEmpty() || message.isEmpty()) {
            showStatus("Please fill in all required fields marked with *!", true);
            return;
        }

        // Validate basic email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showStatus("Invalid email format!", true);
            return;
        }

        btnSendSupport.setDisable(true);

        // Simulation sending request
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate network delay
                Platform.runLater(() -> {
                    btnSendSupport.setDisable(false);
                    txtSubject.clear();
                    txtAreaMessage.clear();
                    showStatus("Request sent successfully! We will respond as soon as possible.", false);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Send Success");
                    alert.setHeaderText("Thank you for contacting us!");
                    alert.setContentText("Your support request has been received. We will respond soon via email: " + email);

                    DialogPane dialogPane = alert.getDialogPane();
                    dialogPane.setStyle("-fx-font-family: 'DM Sans'; -fx-background-color: #fcf8ff; -fx-border-color: #e040a0; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px;");
                    AlertUtil.styleAndShow(alert);
                });
            } catch (InterruptedException e) {
                logger.error("Error sending support request:", e);
                Platform.runLater(() -> {
                    btnSendSupport.setDisable(false);
                    showStatus("Error sending request. Please try again later!", true);
                });
            }
        }).start();
    }

    private void showStatus(String text, boolean isError) {
        lblMessageStatus.setText(text);
        if (isError) {
            lblMessageStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        } else {
            lblMessageStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        }
        lblMessageStatus.setVisible(true);
        lblMessageStatus.setManaged(true);
    }

    @FXML
    public void handleSettings(ActionEvent event) {
        // Can navigate to settings if needed
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