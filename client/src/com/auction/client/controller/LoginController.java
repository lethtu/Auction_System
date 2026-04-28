package com.auction.client.controller;

<<<<<<< HEAD
=======
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
import com.auction.client.model.User;
import javafx.scene.control.Alert;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginController {
<<<<<<< HEAD

=======
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    public void handleLogin(ActionEvent event) {
        String loginField = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (loginField.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập Username/Email và Mật khẩu!");
            return;
        }

        String jsonBody = String.format(
                "{\"username\":\"%s\", \"password\":\"%s\"}",
                loginField, password
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
<<<<<<< HEAD
                    .uri(URI.create("http://localhost:8080/api/login"))
=======
                    .uri(URI.create(Config.API_URL + "/api/login"))
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Sai tài khoản hoặc mật khẩu!");
<<<<<<< HEAD
=======
                logger.error("Lỗi khi connect đến server");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                return;
            }

            JSONObject responseJson = new JSONObject(response.body());

            if (responseJson.getInt("status") != 200) {
                showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Đăng nhập thất bại!");
<<<<<<< HEAD
=======
                logger.error("Lỗi đăng nhập thất bại - status code: {}", responseJson.getInt("status"));
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                return;
            }

            JSONObject data = responseJson.getJSONObject("data");

            int id = data.getInt("id");
            String username = data.optString("username", loginField);
            String fullname = data.optString("fullname", username);
            String email = data.optString("email", "");
            String dob = data.optString("dob", null);
            String placeOfBirth = data.optString("place_of_birth", null);

            String role = data.optString("role",
                    data.optString("accountType", "USER"));

            User.setSession(id, username, fullname, email, dob, placeOfBirth, role);

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chào mừng bạn đã quay lại!");
<<<<<<< HEAD

=======
            logger.info("Đăng nhập thành công");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            String normalizedRole = role.trim().toUpperCase();

            if (normalizedRole.equals("SELLER")) {
                SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1000, 650);
            } else if (normalizedRole.equals("ADMIN")
                    || normalizedRole.equals("SUPER_ADMIN")) {
                SceneSwitcher.switchScene(event, "AdminDashboard.fxml", 1000, 650);
            } else {
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1024, 768);
            }

        } catch (Exception e) {
<<<<<<< HEAD
            e.printStackTrace();
=======
            logger.error("Không thể kết nối máy chủ: {}", e.getMessage(), e);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    @FXML
    public void goToSignUp(ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "SignUp.fxml", 400, 550);
    }

    @FXML
    public void handleForgotPassword(ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "ForgotPassword.fxml", 400, 450);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}