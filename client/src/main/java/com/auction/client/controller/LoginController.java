package com.auction.client.controller;

import com.auction.client.Config;
import com.auction.client.model.User;
import com.auction.client.util.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String loginField = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (loginField.isEmpty() || password.isEmpty()) {
            AlertUtil.showWarning("Lỗi", "Vui lòng nhập Username/Email và Mật khẩu!");
            return;
        }

        JSONObject body = new JSONObject();
        body.put("username", loginField);
        body.put("password", password);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                AlertUtil.showError("Sai tài khoản hoặc mật khẩu!");
                logger.error("Lỗi khi connect đến server");
                return;
            }

            JSONObject responseJson = new JSONObject(response.body());

            if (responseJson.getInt("status") != 200) {
                AlertUtil.showError(responseJson.optString("message", "Đăng nhập thất bại!"));
                logger.error("Đăng nhập thất bại - status code: {}", responseJson.getInt("status"));
                return;
            }

            JSONObject data = responseJson.getJSONObject("data");

            int id = data.getInt("id");
            String username = data.optString("username", loginField);
            String fullname = data.optString("fullname", username);
            String email = data.optString("email", "");
            String dob = data.optString("dob", null);
            String placeOfBirth = data.optString("place_of_birth", null);
            String role = data.optString("role", data.optString("accountType", "USER"));

            User.setSession(id, username, fullname, email, dob, placeOfBirth, role);

            AlertUtil.showInfo("Chào mừng bạn đã quay lại!");
            logger.info("Đăng nhập thành công");

            switchSceneByRole(event, role);

        } catch (Exception e) {
            logger.error("Không thể kết nối máy chủ: {}", e.getMessage(), e);
            AlertUtil.showError("Không thể kết nối đến máy chủ!");
        }
    }

    private void switchSceneByRole(ActionEvent event, String role) throws IOException {
        String normalizedRole = role.trim().toUpperCase();

        if (normalizedRole.equals("SELLER")) {
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1000, 650);
        } else if (normalizedRole.equals("ADMIN")) {
            SceneSwitcher.switchScene(event, "AdminDashboard.fxml", 1000, 650);
        } else {
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1024, 768);
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
}