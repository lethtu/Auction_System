package com.auction.client.controller;

import com.auction.client.Config;
import com.auction.client.model.User;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.HttpRequestUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpResponse;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    public void handleLogin(ActionEvent event) {
        String loginField = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (loginField.isEmpty() || password.isEmpty()) {
            AlertUtil.showWarning("Lỗi", "Vui lòng nhập Username/Email và Mật khẩu!");
            return;
        }

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("username", loginField);
                body.put("password", password);

                HttpResponse<String> response = HttpRequestUtil.sendJson(
                        "POST",
                        Config.API_URL,
                        "/api/login",
                        body
                );

                JSONObject responseJson = response.statusCode() == 200
                        ? new JSONObject(response.body())
                        : null;

                Platform.runLater(() -> handleLoginResponse(event, response, responseJson));
            } catch (Exception e) {
                logger.error("Không thể kết nối máy chủ: {}", e.getMessage(), e);

                Platform.runLater(() ->
                        AlertUtil.showError(e, "Không thể kết nối đến máy chủ!")
                );
            }
        }).start();
    }

    private void handleLoginResponse(
            ActionEvent event,
            HttpResponse<String> response,
            JSONObject responseJson
    ) {
        if (response.statusCode() != 200 || responseJson == null) {
            AlertUtil.showError("Sai tài khoản hoặc mật khẩu!");
            logger.error("Lỗi khi connect đến server - status: {}", response.statusCode());
            return;
        }

        if (responseJson.getInt("status") != 200) {
            AlertUtil.showError(responseJson.optString("message", "Đăng nhập thất bại!"));
            logger.error("Đăng nhập thất bại - status code: {}", responseJson.getInt("status"));
            return;
        }

        JSONObject data = responseJson.getJSONObject("data");

        int id = data.getInt("id");
        String username = data.optString("username", txtUsername.getText().trim());
        String fullname = data.optString("fullname", username);
        String email = data.optString("email", "");
        String dob = data.optString("dob", null);
        String placeOfBirth = data.optString("place_of_birth", null);

        String role = data.optString("role",
                data.optString("accountType", "USER"));

        User.setSession(id, username, fullname, email, dob, placeOfBirth, role);

        AlertUtil.showInfo("Chào mừng bạn đã quay lại!");
        logger.info("Đăng nhập thành công");

        switchSceneByRole(event, role);
    }

    private void switchSceneByRole(ActionEvent event, String role) {
        String normalizedRole = role.trim().toUpperCase();

        try {
            if (normalizedRole.equals("SELLER")) {
                SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1000, 650);
            } else if (normalizedRole.equals("ADMIN")
                    || normalizedRole.equals("SUPER_ADMIN")) {
                SceneSwitcher.switchScene(event, "AdminDashboard.fxml", 1000, 650);
            } else {
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1024, 768);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi chuyển giao diện sau đăng nhập: {}", e.getMessage(), e);
            AlertUtil.showError(e, "Không thể chuyển giao diện!");
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
