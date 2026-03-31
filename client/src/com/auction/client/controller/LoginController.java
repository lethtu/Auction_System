package com.auction.client.controller;

import com.auction.client.model.user;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.scene.control.Alert;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import org.json.JSONObject;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    public void handleLogin(ActionEvent event) {
        String loginField = txtUsername.getText();
        String password = txtPassword.getText();

        if (loginField.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập Username/Email và Mật khẩu!");
            return;
        }
        String jsonBody = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", loginField, password);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject rq = new JSONObject(response.body());
            if (response.statusCode() == 200) {
                if (rq.getInt("status") == 200) {
                    System.out.println("Đăng nhập thành công: " + response.body());
                    JSONObject json = new JSONObject(response.body());
                    json = json.getJSONObject("data");
                    user.setSession(json.getInt("id"), json.getString("fullname"), json.getString("fullname"), json.getString("email"), json.optString("dob", null), json.optString("place_of_birth", null), json.getString("role"));
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chào mừng bạn đã quay lại!");

                    SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1024, 768);
                }

            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Sai tài khoản hoặc mật khẩu!");
            }

        } catch (Exception e) {
            e.printStackTrace();
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