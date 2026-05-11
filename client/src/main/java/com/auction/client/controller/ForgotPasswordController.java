package com.auction.client.controller;

import com.auction.client.Config;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.HttpRequestUtil;
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
                JSONObject body = new JSONObject();
                body.put("email", email);

                HttpResponse<String> response = HttpRequestUtil.sendJson(
                        "POST",
                        Config.API_URL,
                        "/api/forgot_pass",
                        body
                );

                JSONObject responseBody = new JSONObject(response.body());

                Platform.runLater(() -> handleGetOTPResponse(response, responseBody));
            } catch (Exception e) {
                logger.error("Lỗi khi gửi yêu cầu OTP đến server: {}", e.getMessage(), e);

                Platform.runLater(() -> {
                    btnGetOTP.setDisable(false);
                    btnGetOTP.setText("Gửi lại mã");
                    AlertUtil.showError(e, "Không thể gửi yêu cầu OTP đến server!");
                });
            }
        }).start();
    }

    private void handleGetOTPResponse(HttpResponse<String> response, JSONObject responseBody) {
        if (response.statusCode() == 200 && responseBody.getInt("status") == 200) {
            stepEmail.setDisable(true);
            stepReset.setVisible(true);
            stepReset.setManaged(true);

            AlertUtil.showInfo(responseBody.getString("message"));
            logger.info("Gửi yêu cầu OTP thành công");
            return;
        }

        AlertUtil.showError(responseBody.getString("message"));
        btnGetOTP.setDisable(false);
        btnGetOTP.setText("Gửi lại mã");

        logger.info("Gửi yêu cầu OTP thất bại, message trả về: {}", responseBody.getString("message"));
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
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("code", code);
                body.put("password", newPass);

                HttpResponse<String> response = HttpRequestUtil.sendJson(
                        "POST",
                        Config.API_URL,
                        "/api/check_code",
                        body
                );

                JSONObject responseBody = new JSONObject(response.body());

                Platform.runLater(() -> handleResetPasswordResponse(event, response, responseBody));
            } catch (Exception e) {
                logger.error("Lỗi trong quá trình đổi mật khẩu: {}", e.getMessage(), e);

                Platform.runLater(() ->
                        AlertUtil.showError(e, "Không thể đổi mật khẩu!")
                );
            }
        }).start();
    }

    private void handleResetPasswordResponse(
            ActionEvent event,
            HttpResponse<String> response,
            JSONObject responseBody
    ) {
        if (response.statusCode() != 200) {
            AlertUtil.showError("Lỗi mạng");
            logger.info("Lỗi phản hồi từ server - status: {}", response.statusCode());
            return;
        }

        if (responseBody.getInt("status") != 200) {
            AlertUtil.showError(responseBody.getString("message"));
            logger.info("Yêu cầu đổi mật khẩu thất bại");
            return;
        }

        AlertUtil.showInfo(responseBody.getString("message"));
        logger.info("Đổi mật khẩu thành công");

        try {
            goToLogin(event);
        } catch (Exception e) {
            logger.error("Lỗi khi chuyển giao diện: {}", e.getMessage(), e);
            AlertUtil.showError(e, "Không thể chuyển về màn hình đăng nhập!");
        }
    }

    @FXML
    public void goToLogin(ActionEvent event) throws Exception {
        SceneSwitcher.switchScene(event, "Login.fxml", Config.Width, Config.Height);
    }
}