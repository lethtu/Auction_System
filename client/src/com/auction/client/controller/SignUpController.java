package com.auction.client.controller;

<<<<<<< HEAD
=======
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
<<<<<<< HEAD
=======

import com.auction.client.Config;
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import org.json.JSONObject; // Đảm bảo bạn đã thêm thư viện org.json vào dự án

public class SignUpController {
<<<<<<< HEAD
=======
    private static final Logger logger = LoggerFactory.getLogger(SignUpController.class);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)

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

<<<<<<< HEAD
        // 1. Kiểm tra đầu vào
=======
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
        if (fullname.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng điền đầy đủ các trường!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp!");
            return;
        }

<<<<<<< HEAD
        // 2. Tạo JSON Body bằng thư viện (Tránh lỗi SQL Null Password và lỗi ký tự đặc
        // biệt)
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password); // Key phải khớp chính xác với biến 'password' ở Server
=======
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
        json.put("email", email);
        json.put("fullname", fullname);
        String jsonBody = json.toString();

<<<<<<< HEAD
        // 3. Chạy luồng riêng để không làm đơ giao diện
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/signup"))
=======
        //Chạy luồng riêng để không làm đơ giao diện
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/signup"))
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
<<<<<<< HEAD
                    // Phân tích kết quả từ ApiResponse của Server
=======
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                    JSONObject resObj = new JSONObject(response.body());
                    String message = resObj.optString("message", "");

                    Platform.runLater(() -> {
                        if (message.contains("thành công")) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký tài khoản thành công!");
<<<<<<< HEAD
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
                    Platform.runLater(
                            () -> showAlert(Alert.AlertType.ERROR, "Lỗi Server", "Mã lỗi: " + response.statusCode()));
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(
                        () -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối tới máy chủ!"));
=======
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
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            }
        }).start();
    }

    @FXML
    public void goToLogin(ActionEvent event) throws IOException {
<<<<<<< HEAD
        // QUAN TRỌNG: Sửa đường dẫn FXML có dấu "/" ở đầu để SceneSwitcher tìm thấy
        // file
        SceneSwitcher.switchScene(event, "Login.fxml", 400, 500);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        // Đảm bảo Alert luôn chạy trên luồng giao diện (UI Thread)
=======
        SceneSwitcher.switchScene(event, "Login.fxml", Config.Width, Config.Height);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        //Alert luôn chạy trên luồng giao diện (UI Thread)
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
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