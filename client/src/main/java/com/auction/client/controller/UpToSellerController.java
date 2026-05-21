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
import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.input.MouseEvent;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Scanner;

public class UpToSellerController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(UpToSellerController.class);

    @FXML private CheckBox chkAgree;
    @FXML private Label lblMessage;
    @FXML private Button btnUpgrade;

    @FXML private MenuButton userMenuButton;
    @FXML private TextField txtSearch;
    @FXML private Button btnDashboard;

    @FXML private SidebarController sidebarController;
    @FXML private Button btnHamburger;
    
    @FXML private Button btnNotificationBell;
    @FXML private Label notificationBadge;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (User.getFullname() != null) {
            createUserOption("Chào, " + User.getFullname());
        }

        if (User.getRole() != null && User.getRole().equalsIgnoreCase("seller")) {
            btnDashboard.setVisible(true);
            btnDashboard.setManaged(true);
        }
        
        if (btnNotificationBell != null && notificationBadge != null) {
            NotificationBellBinder.bind(btnNotificationBell, notificationBadge);
        }
    }

    private void createUserOption(String text) {
        MenuItem accountItem = new MenuItem("Tài Khoản Của Tôi");
        MenuItem depositMoney = new MenuItem("Nạp tiền");
        MenuItem logoutItem = new MenuItem("Đăng Xuất");

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

        userMenuButton.getItems().addAll(accountItem, depositMoney, logoutItem);
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
    public void handleGoToDashboard(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Lỗi khi chuyển về trang Quản lý Seller: ", e);
        }
    }

    @FXML
    public void handleUpgrade(ActionEvent event) {
        if (User.getId() == null) {
            showError("Vui lòng đăng nhập để nâng cấp!");
            return;
        }

        if (!chkAgree.isSelected()) {
            showError("Vui lòng đồng ý với điều khoản bán hàng của HY Auction!");
            return;
        }

        btnUpgrade.setDisable(true);
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);

        new Thread(() -> {
            try {
                String apiUrl = Config.API_URL + "/api/bidder/up-to-seller?userId=" + User.getId();
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
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
                        // Thành công
                        User.setSession(User.getId(), User.getUsername(), User.getFullname(), User.getEmail(), User.getDob(), User.getPlace_of_birth(), "SELLER", User.getAvatarUrl());

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Nâng cấp thành công");
                        alert.setHeaderText("Chúc mừng bạn đã trở thành Người bán!");
                        alert.setContentText("Tính năng Bán Hàng đã được mở khóa. Hãy truy cập Kênh Người Bán để đăng bán sản phẩm của riêng bạn.");

                        DialogPane dialogPane = alert.getDialogPane();
                        dialogPane.setStyle("-fx-font-family: 'DM Sans'; -fx-background-color: #fcf8ff; -fx-border-color: #e040a0; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px;");
                        alert.showAndWait();

                        try {
                            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                        } catch (Exception ex) {
                            logger.error("Lỗi chuyển cảnh sau khi nâng cấp: ", ex);
                        }
                    } else {
                        showError(jsonResponse.optString("message", "Nâng cấp thất bại hoặc tài khoản đã là SELLER!"));
                    }
                });

            } catch (Exception e) {
                logger.error("Lỗi khi nâng cấp lên seller: ", e);
                Platform.runLater(() -> {
                    btnUpgrade.setDisable(false);
                    showError("Lỗi kết nối máy chủ khi nâng cấp!");
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
            logger.error("Lỗi quay lại trang chính: ", e);
        }
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Lỗi quay lại trang chính: ", e);
        }
    }

    private void showError(String message) {
        lblMessage.setText(message);
        lblMessage.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }
}
