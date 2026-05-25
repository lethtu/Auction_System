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
import java.net.URL;
import java.util.ResourceBundle;

public class SupportController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SupportController.class);

    @FXML private TextField txtEmail;
    @FXML private TextField txtSubject;
    @FXML private TextArea txtAreaMessage;
    @FXML private Label lblMessageStatus;
    @FXML private Button btnSendSupport;

    @FXML private SidebarController sidebarController;
    @FXML private TopbarController topbarController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (topbarController != null) {
            topbarController.setSearchVisible(false);
            if (sidebarController != null) {
                topbarController.setSidebarController(sidebarController);
            }
        }

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
                    } catch (java.io.IOException e) {
                        logger.error("Navigation error:", e);
                    }
                }

                @Override
                public void onResetFilter(ActionEvent event) {
                    try {
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (java.io.IOException e) {
                        logger.error("Navigation error:", e);
                    }
                }
            });
        }
    }

    @FXML
    public void handleSendSupport(ActionEvent event) {
        String email = txtEmail.getText().trim();
        String subject = txtSubject.getText().trim();
        String message = txtAreaMessage.getText().trim();

        if (email.isEmpty() || subject.isEmpty() || message.isEmpty()) {
            showStatus("Please fill in all required fields marked with *.", true);
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

                    AlertUtil.show(
                            Alert.AlertType.INFORMATION,
                            "Send Success",
                            "Thank you for contacting us!\nYour support request has been received. We will respond soon via email: " + email
                    );
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
        applySupportStatusStyle(lblMessageStatus.getText());
        lblMessageStatus.setVisible(true);
        lblMessageStatus.setManaged(true);
    }

    private void applySupportStatusStyle(String message) {
        String lower = message == null ? "" : message.toLowerCase();
        boolean success = lower.contains("success")
                || lower.contains("sent")
                || lower.contains("submitted")
                || lower.contains("thank")
                || lower.contains("received");

        String background = success ? "rgba(0, 200, 130, 0.11)" : "rgba(255, 82, 82, 0.11)";
        String border = success ? "rgba(0, 200, 130, 0.55)" : "rgba(255, 82, 82, 0.60)";
        String color = success ? "#00c882" : "#ff6b6b";

        lblMessageStatus.setStyle(
                "-fx-background-color: " + background + ";"
                        + " -fx-border-color: " + border + ";"
                        + " -fx-border-width: 1.2px;"
                        + " -fx-border-radius: 14px;"
                        + " -fx-background-radius: 14px;"
                        + " -fx-padding: 10px 14px;"
                        + " -fx-text-fill: " + color + ";"
                        + " -fx-font-size: 13.5px;"
                        + " -fx-font-weight: 900;"
        );
    }
}