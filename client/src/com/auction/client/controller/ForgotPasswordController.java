package com.auction.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.*;

public class ForgotPasswordController {

    @FXML private TextField txtEmail, txtOTP;
    @FXML private PasswordField txtNewPassword, txtConfirmNewPassword;
    @FXML private VBox stepEmail, stepReset;
    @FXML private Button btnGetOTP;

    @FXML
    public void handleGetOTP(ActionEvent event) {
        String email = txtEmail.getText().trim();
        if (email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập Email!");
            return;
        }

        btnGetOTP.setDisable(true);
        btnGetOTP.setText("Đang xử lý...");

        new Thread(() -> {
            try {
                // Sử dụng API 1 để kiểm tra email
                JSONObject json = new JSONObject();
                json.put("email", email);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/forgot_pass"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println(response.body());
                JSONObject rq = new JSONObject(response.body());
                Platform.runLater(() -> {
                    if (rq.getInt("status") == 200 && response.statusCode() == 200) {
                        stepEmail.setDisable(true);
                        stepReset.setVisible(true);
                        stepReset.setManaged(true);
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", rq.getString("message"));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", rq.getString("message"));
                        btnGetOTP.setDisable(false);
                        btnGetOTP.setText("Gửi lại mã");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> btnGetOTP.setDisable(false));
            }
        }).start();
    }

    @FXML
    public void handleResetPassword(ActionEvent event) {
        String email = txtEmail.getText().trim();
        String code = txtOTP.getText().trim();
        String newPass = txtNewPassword.getText();
        String confirmPass = txtConfirmNewPassword.getText();

        if (code.isEmpty() || newPass.isEmpty() || !newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Thông tin không hợp lệ hoặc mật khẩu không khớp!");
            return;
        }

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("code", code);
                json.put("password", newPass);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/check_code"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject rq = new JSONObject(response.body());
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        if (rq.getInt("status") == 200){
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", rq.getString("message"));
                            try {
                                goToLogin(event);
                            }
                            catch (Exception e) {}
                        }
                        else{
                            showAlert(Alert.AlertType.ERROR, "Thất bại", rq.getString("message"));
                        }
                    }
                    else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi mạng");
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    public void goToLogin(ActionEvent event) throws Exception {
        SceneSwitcher.switchScene(event, "Login.fxml", 400, 500);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}