package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.model.User;
import com.auction.client.service.SupportRequestService;
import com.auction.client.util.AlertUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class SupportController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SupportController.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private final SupportRequestService supportRequestService = new SupportRequestService();

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
        String email = getTrimmedText(txtEmail);
        String subject = getTrimmedText(txtSubject);
        String message = getTrimmedText(txtAreaMessage);

        if (!validateSupportForm(email, subject, message)) {
            return;
        }

        setSendingState(true);

        Thread openEmailThread = new Thread(() -> openSupportEmailDraft(email, subject, message), "support-email-draft");
        openEmailThread.setDaemon(true);
        openEmailThread.start();
    }

    private boolean validateSupportForm(String email, String subject, String message) {
        if (email.isEmpty() || subject.isEmpty() || message.isEmpty()) {
            showStatus("Please fill in all required fields marked with *!", true);
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showStatus("Invalid email format!", true);
            return false;
        }

        return true;
    }

    private void openSupportEmailDraft(String email, String subject, String message) {
        try {
            boolean opened = supportRequestService.openSupportEmailDraft(email, subject, message);
            Platform.runLater(() -> handleSupportDraftResult(opened, subject));
        } catch (Exception e) {
            logger.error("Could not open support email draft:", e);
            Platform.runLater(() -> handleSupportDraftResult(false, subject));
        }
    }

    private void handleSupportDraftResult(boolean opened, String subject) {
        setSendingState(false);

        if (opened) {
            txtSubject.clear();
            txtAreaMessage.clear();
            showStatus("Email draft opened. Please review and send it from your email app.", false);
            AlertUtil.show(
                    Alert.AlertType.INFORMATION,
                    "Email Draft Opened",
                    "A support email draft has been opened. Please review and send it from your email app."
            );
            return;
        }

        String instruction = supportRequestService.buildManualSendInstruction(subject);
        showStatus(instruction, true);
        AlertUtil.show(Alert.AlertType.WARNING, "Open Email App Failed", instruction);
    }

    private void setSendingState(boolean sending) {
        btnSendSupport.setDisable(sending);
        btnSendSupport.setText(sending ? "Opening Email..." : "Open Email Draft");
    }

    private String getTrimmedText(TextInputControl control) {
        return control == null || control.getText() == null ? "" : control.getText().trim();
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