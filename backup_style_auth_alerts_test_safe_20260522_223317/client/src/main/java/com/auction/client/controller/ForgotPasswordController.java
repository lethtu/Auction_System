package com.auction.client.controller;



import com.auction.client.util.AlertUtil;
import com.auction.client.Config;
import com.auction.client.HttpClientSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.*;

public class ForgotPasswordController {
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);
    private HttpClient client = HttpClientSingleton.getInstance().getHttpClient();

    @FXML private TextField txtEmail, txtOTP;
    @FXML private PasswordField txtNewPassword, txtConfirmNewPassword;
    @FXML private VBox stepEmail, stepReset;
    @FXML private Button btnGetOTP, btnResetPassword;
    @FXML private Label lblStep1, lblStep2;

    @FXML
    public void handleGetOTP(ActionEvent event) {
        String email = txtEmail.getText().trim();
        if (email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Error", "Vui lòng nhập Email!");
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
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject rq = new JSONObject(response.body());
                Platform.runLater(() -> {
                    if (rq.getInt("status") == 200 && response.statusCode() == 200) {
                        stepEmail.setDisable(true);
                        stepReset.setVisible(true);
                        stepReset.setManaged(true);
                        
                        if (lblStep1 != null && lblStep2 != null) {
                            lblStep1.setStyle("-fx-font-family: 'DM Sans'; -fx-font-weight: bold; -fx-text-fill: #907898;");
                            lblStep2.setStyle("-fx-font-family: 'DM Sans'; -fx-font-weight: bold; -fx-text-fill: #e040a0;");
                        }
                        
                        showAlert(Alert.AlertType.INFORMATION, "Success", rq.getString("message"));
                        logger.info("OTP request sent successfully");
                    }
                    else {
                        showAlert(Alert.AlertType.ERROR, "Error", rq.getString("message"));
                        btnGetOTP.setDisable(false);
                        btnGetOTP.setText("Gửi lại mã");
                        logger.info("Request failed, server message: {}", rq.getString("message"));
                    }
                });
            }
            catch (Exception e) {
                logger.error("Error sending request to server: {}", e.getMessage(), e);
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
            showAlert(Alert.AlertType.ERROR, "Error", "Thông tin không hợp lệ hoặc mật khẩu không khớp!");
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

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject rq = new JSONObject(response.body());
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        if (rq.getInt("status") == 200){
                            showAlert(Alert.AlertType.INFORMATION, "Success", rq.getString("message"));
                            logger.info("Password changed successfully");
                            try {
                                goToLogin(event);
                            }
                            catch (Exception e) {
                                logger.error("Error switching view: {}", e.getMessage(), e);
                            }
                        }
                        else{
                            showAlert(Alert.AlertType.ERROR, "Failed", rq.getString("message"));
                            logger.info("Password change request failed");
                        }
                    }
                    else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Network error");
                        logger.info("Server response error - status: {}", response.statusCode());
                    }
                });
            }
            catch (Exception e) {
                logger.error("Error during execution: {}", e.getMessage(), e);
            }
        }).start();
    }

    public void setHttpClient(HttpClient httpClient) {
        this.client = httpClient;
    }

    @FXML
    public void goToLogin(ActionEvent event) throws Exception {
        SceneSwitcher.switchScene(event, "Login.fxml", 1100, 700);
    }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        AlertUtil.styleDialog(alert);
        AlertUtil.styleAndShow(alert);
    }

    @FXML
    private void handleMinimize(javafx.event.ActionEvent event) {
        SceneSwitcher.handleMinimize(event);
    }

    @FXML
    private void handleMaximize(javafx.event.ActionEvent event) {
        SceneSwitcher.handleMaximize(event);
    }

    @FXML
    private void handleClose(javafx.event.ActionEvent event) {
        SceneSwitcher.handleClose(event);
    }
}