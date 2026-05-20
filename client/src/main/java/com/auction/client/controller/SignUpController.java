package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.auction.client.Config;
import com.auction.client.HttpClientSingleton;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import org.json.JSONObject;

public class SignUpController {
    private HttpClient client = HttpClientSingleton.getInstance().getHttpClient();
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
    @FXML private javafx.scene.layout.Region strength1;
    @FXML private javafx.scene.layout.Region strength2;
    @FXML private javafx.scene.layout.Region strength3;
    @FXML private javafx.scene.control.Label lblStrength;
    @FXML private javafx.scene.control.CheckBox chkTerms;

    @FXML
    public void initialize() {
        if (txtPassword != null) {
            txtPassword.textProperty().addListener((obs, oldV, newV) -> updatePasswordStrength(newV));
        }
    }

    private void updatePasswordStrength(String password) {
        if (strength1 == null) return;
        strength1.getStyleClass().removeAll("strength-empty", "strength-weak", "strength-medium", "strength-strong");
        strength2.getStyleClass().removeAll("strength-empty", "strength-weak", "strength-medium", "strength-strong");
        strength3.getStyleClass().removeAll("strength-empty", "strength-weak", "strength-medium", "strength-strong");
        
        if (password == null || password.length() < 6) {
            strength1.getStyleClass().add("strength-weak");
            strength2.getStyleClass().add("strength-empty");
            strength3.getStyleClass().add("strength-empty");
            lblStrength.setText("Mật khẩu quá yếu (cần ít nhất 6 ký tự)");
            lblStrength.setStyle("-fx-text-fill: #e53e3e; -fx-font-family: 'DM Sans'; -fx-font-size: 11px;");
        } else if (password.length() < 10 || !password.matches(".*\\d.*") || !password.matches(".*[a-zA-Z].*")) {
            strength1.getStyleClass().add("strength-medium");
            strength2.getStyleClass().add("strength-medium");
            strength3.getStyleClass().add("strength-empty");
            lblStrength.setText("Mật khẩu trung bình (thêm số & ký tự)");
            lblStrength.setStyle("-fx-text-fill: #eab308; -fx-font-family: 'DM Sans'; -fx-font-size: 11px;");
        } else {
            strength1.getStyleClass().add("strength-strong");
            strength2.getStyleClass().add("strength-strong");
            strength3.getStyleClass().add("strength-strong");
            lblStrength.setText("Mật khẩu mạnh");
            lblStrength.setStyle("-fx-text-fill: #22c55e; -fx-font-family: 'DM Sans'; -fx-font-size: 11px;");
        }
    }

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

        if (chkTerms != null && !chkTerms.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Bạn phải đồng ý với các điều khoản!");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Mật khẩu phải có ít nhất 6 ký tự!");
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
        logger.info(jsonBody);
        //Chạy luồng riêng để không làm đơ giao diện
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/signup"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

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

    public void setHttpClient(HttpClient httpClient) {
        this.client = httpClient;
    }

    @FXML
    public void goToLogin(ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "Login.fxml", 1000, 650);
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