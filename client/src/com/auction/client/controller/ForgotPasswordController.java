package com.auction.client.controller;

import com.auction.client.Config;
import com.auction.client.util.AlertUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ForgotPasswordController {
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtOTP;

    @FXML
    private PasswordField txtNewPassword;

    @FXML
    private PasswordField txtConfirmNewPassword;

    @FXML
    private VBox stepEmail;

    @FXML
    private VBox stepReset;

    @FXML
    private Button btnGetOTP;

    @FXML
    public void handleGetOTP(ActionEvent event) {
        String email = txtEmail.getText().trim();

        if (email.isEmpty()) {
            AlertUtil.showWarning("Lỗi", "Vui lòng nhập Email!");
            return;
        }

        btnGetOTP.setDisable(true);
        btnGetOTP.setText("Đang xử lý...");

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/forgot_pass"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());

                JSONObject rq = new JSONObject(response.body());

                Platform.runLater(() -> {
                    if (rq.getInt("status") == 200 && response.statusCode() == 200) {
                        stepEmail.setDisable(true);
                        stepReset.setVisible(true);
                        stepReset.setManaged(true);

                        AlertUtil.showInfo(rq.getString("message"));
                        logger.info("Gửi yêu cầu gửi OTP thành công");
                    } else {
                        AlertUtil.showError(rq.getString("message"));

                        btnGetOTP.setDisable(false);
                        btnGetOTP.setText("Gửi lại mã");

                        logger.info("Gửi yêu cầu thất bại, message trả về: {}", rq.getString("message"));
                    }
                });
            } catch (Exception e) {
                logger.error("Lỗi khi gửi yêu cầu đến server: {}", e.getMessage(), e);

                Platform.runLater(() -> {
                    btnGetOTP.setDisable(false);
                    btnGetOTP.setText("Gửi lại mã");
                    AlertUtil.showError(e, "Không thể gửi yêu cầu đến server!");
                });
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
            AlertUtil.showError("Thông tin không hợp lệ hoặc mật khẩu không khớp!");
            return;
        }

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("code", code);
                json.put("password", newPass);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/check_code"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());

                JSONObject rq = new JSONObject(response.body());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        if (rq.getInt("status") == 200) {
                            AlertUtil.showInfo(rq.getString("message"));
                            logger.info("Đổi mật khẩu thành công");

                            try {
                                goToLogin(event);
                            } catch (Exception e) {
                                logger.error("Lỗi khi chuyển giao diện: {}", e.getMessage(), e);
                                AlertUtil.showError(e, "Không thể chuyển về màn hình đăng nhập!");
                            }
                        } else {
                            AlertUtil.showError(rq.getString("message"));
                            logger.info("Yêu cầu đổi mật khẩu thất bại");
                        }
                    } else {
                        AlertUtil.showError("Lỗi mạng");
                        logger.info("Lỗi phản hồi từ server - status: {}", response.statusCode());
                    }
                });
            } catch (Exception e) {
                logger.error("Lỗi trong quá trình thực thi: {}", e.getMessage(), e);

                Platform.runLater(() ->
                        AlertUtil.showError(e, "Không thể đổi mật khẩu!")
                );
            }
        }).start();
    }

    @FXML
    public void goToLogin(ActionEvent event) throws Exception {
        SceneSwitcher.switchScene(event, "Login.fxml", Config.Width, Config.Height);
    }
}