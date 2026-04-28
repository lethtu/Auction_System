package com.auction.client.controller;

<<<<<<< HEAD
=======

import com.auction.client.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.*;

public class ForgotPasswordController {
<<<<<<< HEAD
=======
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)

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
<<<<<<< HEAD
                // Sử dụng API 1 để kiểm tra email
=======
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                JSONObject json = new JSONObject();
                json.put("email", email);

                HttpRequest request = HttpRequest.newBuilder()
<<<<<<< HEAD
                        .uri(URI.create("http://localhost:8080/api/forgot_pass"))
=======
                        .uri(URI.create(Config.API_URL + "/api/forgot_pass"))
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
<<<<<<< HEAD
                System.out.println(response.body());
=======
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                JSONObject rq = new JSONObject(response.body());
                Platform.runLater(() -> {
                    if (rq.getInt("status") == 200 && response.statusCode() == 200) {
                        stepEmail.setDisable(true);
                        stepReset.setVisible(true);
                        stepReset.setManaged(true);
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", rq.getString("message"));
<<<<<<< HEAD
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", rq.getString("message"));
                        btnGetOTP.setDisable(false);
                        btnGetOTP.setText("Gửi lại mã");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
=======
                        logger.info("Gửi yêu cầu gửi OTP thành công");
                    }
                    else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", rq.getString("message"));
                        btnGetOTP.setDisable(false);
                        btnGetOTP.setText("Gửi lại mã");
                        logger.info("Gửi yêu cầu thất bại, message trả về: {}", rq.getString("message"));
                    }
                });
            }
            catch (Exception e) {
                logger.error("Lỗi khi gửi yêu cầu đến server: {}", e.getMessage(), e);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
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
<<<<<<< HEAD
                        .uri(URI.create("http://localhost:8080/api/check_code"))
=======
                        .uri(URI.create(Config.API_URL + "/api/check_code"))
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject rq = new JSONObject(response.body());
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        if (rq.getInt("status") == 200){
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", rq.getString("message"));
<<<<<<< HEAD
                            try {
                                goToLogin(event);
                            }
                            catch (Exception e) {}
                        }
                        else{
                            showAlert(Alert.AlertType.ERROR, "Thất bại", rq.getString("message"));
=======
                            logger.info("Đổi mật khẩu thành công");
                            try {
                                goToLogin(event);
                            }
                            catch (Exception e) {
                                logger.error("Lỗi khi chuyển giao diện: {}", e.getMessage(), e);
                            }
                        }
                        else{
                            showAlert(Alert.AlertType.ERROR, "Thất bại", rq.getString("message"));
                            logger.info("Yêu cầu đổi mật khẩu thất bại");
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                        }
                    }
                    else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi mạng");
<<<<<<< HEAD
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
=======
                        logger.info("Lỗi phản hồi từ server - status: {}", response.statusCode());
                    }
                });
            }
            catch (Exception e) {
                logger.error("Lỗi trong quá trình thực thi: {}", e.getMessage(), e);
            }
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
        }).start();
    }

    @FXML
    public void goToLogin(ActionEvent event) throws Exception {
<<<<<<< HEAD
        SceneSwitcher.switchScene(event, "Login.fxml", 400, 500);
=======
        SceneSwitcher.switchScene(event, "Login.fxml", Config.Width, Config.Height);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}