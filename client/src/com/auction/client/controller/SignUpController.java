package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.auction.client.Config;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import org.json.JSONObject; // Đảm bảo bạn đã thêm thư viện org.json vào dự án

public class SignUpController {
    private static final Logger logger = LoggerFactory.getLogger(SignUpController.class);

    @FXML
    private TextField txtFullName;
    @FXML
    private TextField txtUsername;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    public void handleSignUp(ActionEvent event) {
        String fullname = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        if (fullname.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng điền đầy đủ các trường!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp!");
            return;
        }

        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password);
        json.put("email", email);
        json.put("fullname", fullname);
        String jsonBody = json.toString();

        //Chạy luồng riêng để không làm đơ giao diện
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/signup"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject resObj = new JSONObject(response.body());
                    String message = resObj.optString("message", "");

                    Platform.runLater(() -> {
                        if (message.contains("thành công")) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký tài khoản thành công!");
                            logger.info("Đăng ký tài khoản thành công");
                            try {
                                goToLogin(event);
                            }
                            catch (IOException e) {
                                logger.error("Lỗi khi chuyển giao diện: {}", e.getMessage(), e);
                            }

                        }
                        else {
                            showAlert(Alert.AlertType.ERROR, "Thất bại", message);
                            logger.info("Đăng ký thất bại");
                        }
                    });
                }
                else {
                    Platform.runLater(
                            () -> showAlert(Alert.AlertType.ERROR, "Lỗi Server", "Mã lỗi: " + response.statusCode()));
                    logger.info("Lỗi đăng ký thất bại - status: {}", response.statusCode());
                }

            }
            catch (Exception e) {
                Platform.runLater(
                        () -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối tới máy chủ!"));
                logger.error("Lỗi trong quá trình kết nối đến máy chủ: {}", e.getMessage(), e);
            }
        }).start();
    }

    @FXML
    public void goToLogin(ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "Login.fxml", Config.Width, Config.Height);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        //Alert luôn chạy trên luồng giao diện (UI Thread)
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showAlert(alertType, title, message));
            return;
        }
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}