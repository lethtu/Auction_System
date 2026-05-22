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
}