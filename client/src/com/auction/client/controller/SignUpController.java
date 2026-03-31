package com.auction.client.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import org.json.JSONObject; // Đảm bảo bạn đã thêm thư viện org.json vào dự án

public class SignUpController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML
    public void handleSignUp(ActionEvent event) {
        String fullname = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        // 1. Kiểm tra đầu vào
        if (fullname.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng điền đầy đủ các trường!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp!");
            return;
        }

        // 2. Tạo JSON Body bằng thư viện (Tránh lỗi SQL Null Password và lỗi ký tự đặc biệt)
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password); // Key phải khớp chính xác với biến 'password' ở Server
        json.put("email", email);
        json.put("fullname", fullname);
        String jsonBody = json.toString();

        // 3. Chạy luồng riêng để không làm đơ giao diện
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/signup"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Phân tích kết quả từ ApiResponse của Server
                    JSONObject resObj = new JSONObject(response.body());
                    String message = resObj.optString("message", "");

                    Platform.runLater(() -> {
                        if (message.contains("thành công")) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký tài khoản thành công!");
                            try {
                                goToLogin(event); // Tự động quay về màn hình đăng nhập
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // Trường hợp trùng Username hoặc Email (Server trả về message lỗi)
                            showAlert(Alert.AlertType.ERROR, "Thất bại", message);
                        }
                    });
                } else {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Lỗi Server", "Mã lỗi: " + response.statusCode())
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối tới máy chủ!")
                );
            }
        }).start();
    }

    @FXML
    public void goToLogin(ActionEvent event) throws IOException {
        // QUAN TRỌNG: Sửa đường dẫn FXML có dấu "/" ở đầu để SceneSwitcher tìm thấy file
        SceneSwitcher.switchScene(event, "/com/auction/client/view/Login.fxml", 400, 500);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        // Đảm bảo Alert luôn chạy trên luồng giao diện (UI Thread)
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