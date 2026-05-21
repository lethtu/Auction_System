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

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (User.getFullname() != null) {
            createUserOption("Chào, " + User.getFullname());
        }

        if (btnNotificationBell != null && notificationBadge != null) {
            com.auction.client.util.NotificationBellBinder.bind(btnNotificationBell, notificationBadge);
        }

        if (btnSettings != null) {
            btnSettings.setOnAction(e -> {
                try {
                    com.auction.client.controller.SceneSwitcher.switchScene(e, "Settings.fxml", 1280, 800);
                } catch (IOException ex) {
                    logger.error("Lỗi chuyển sang trang Settings.fxml: ", ex);
                }
            });
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
                    } catch (IOException e) {
                        logger.error("Lỗi điều hướng:", e);
                    }
                }

                @Override
                public void onResetFilter(ActionEvent event) {
                    try {
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (IOException e) {
                        logger.error("Lỗi điều hướng:", e);
                    }
                }
            });
        }
    }

    private void createUserOption(String text) {
        MenuItem accountItem = new MenuItem("Tài Khoản Của Tôi");
        MenuItem depositMoney = new MenuItem("Nạp tiền");
        MenuItem logoutItem = new MenuItem("Đăng Xuất");

        accountItem.setOnAction(event -> {
            try {
                MainController.initialShowAccount = true;
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Lỗi khi chuyển sang trang tài khoản: ", e);
            }
        });

        depositMoney.setOnAction(event -> {
            try {
                SceneSwitcher.switchScene(event, "Deposit.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Lỗi khi chuyển sang trang nạp tiền: ", e);
            }
        });

        logoutItem.setOnAction(event -> {
            try {
                handleLogout(event);
            } catch (IOException e) {
                logger.error("Lỗi khi chuyển sang màn hình Login!", e);
            }
        });

        userMenuButton.getItems().addAll(accountItem, depositMoney, new SeparatorMenuItem(), logoutItem);
    }

    public void handleLogout(ActionEvent event) throws IOException {
        User.clearSession();
        SceneSwitcher.switchScene(event, "Login.fxml", 400, 500);
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
            logger.error("Lỗi khi chuyển về trang chính: ", e);
        }
    }



    @FXML
    public void handleSendSupport(ActionEvent event) {
        String email = txtEmail.getText().trim();
        String subject = txtSubject.getText().trim();
        String message = txtAreaMessage.getText().trim();

        if (email.isEmpty() || subject.isEmpty() || message.isEmpty()) {
            showStatus("Vui lòng điền đầy đủ tất cả các thông tin có dấu *!", true);
            return;
        }

        // Validate basic email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showStatus("Định dạng Email không hợp lệ!", true);
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
                    showStatus("Gửi yêu cầu thành công! Chúng tôi sẽ phản hồi bạn sớm nhất.", false);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Gửi thành công");
                    alert.setHeaderText("Cảm ơn bạn đã liên hệ!");
                    alert.setContentText("Yêu cầu hỗ trợ của bạn đã được ghi nhận. Chúng tôi sẽ phản hồi sớm nhất qua email: " + email);

                    DialogPane dialogPane = alert.getDialogPane();
                    dialogPane.setStyle("-fx-font-family: 'DM Sans'; -fx-background-color: #fcf8ff; -fx-border-color: #e040a0; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px;");
                    alert.showAndWait();
                });
            } catch (InterruptedException e) {
                logger.error("Lỗi gửi yêu cầu hỗ trợ:", e);
                Platform.runLater(() -> {
                    btnSendSupport.setDisable(false);
                    showStatus("Gặp lỗi khi gửi yêu cầu. Vui lòng thử lại sau!", true);
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
