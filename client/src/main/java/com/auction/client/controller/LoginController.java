package com.auction.client.controller;

import com.auction.client.Config;
import com.auction.client.HttpClientSingleton;
import com.auction.client.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.application.Platform;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private static final String SELLER_ROLE = "seller";
    private static final String ADMIN_ROLE = "admin";
    private static final String DEFAULT_ROLE = "user";

    private HttpClient client = HttpClientSingleton.getInstance().getHttpClient();

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML private Label lblLiveAuctions;
    @FXML private Label lblActiveBidders;
    @FXML private Button btnGoogle;
    @FXML private Button btnFacebook;

    @FXML
    public void initialize() {
        if (btnGoogle != null) {
            btnGoogle.setTooltip(new Tooltip("Tính năng đăng nhập bằng Google sẽ được bổ sung sau."));
        }
        if (btnFacebook != null) {
            btnFacebook.setTooltip(new Tooltip("Tính năng đăng nhập bằng Facebook sẽ được bổ sung sau."));
        }
        loadPublicStats();
    }

    private void loadPublicStats() {
        if (lblLiveAuctions == null || lblActiveBidders == null) return;

        lblLiveAuctions.setText("Live Auctions Now");
        lblActiveBidders.setText("Auction activity is loading...");

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/auctions/stats"))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject resJson = new JSONObject(response.body());
                    if (resJson.getInt("status") == 200) {
                        JSONObject data = resJson.getJSONObject("data");
                        long activeBidders = data.optLong("activeBidders", 12000);
                        long liveAuctions = data.optLong("liveAuctions", 8);

                        Platform.runLater(() -> {
                            lblLiveAuctions.setText(liveAuctions + " auctions live");
                            lblActiveBidders.setText("Join " + String.format("%,d", activeBidders) + "+ active bidders");
                        });
                        return;
                    }
                }
                
                Platform.runLater(this::setFallbackStats);
                
            } catch (Exception e) {
                logger.error("Failed to load public stats", e);
                Platform.runLater(this::setFallbackStats);
            }
        }).start();
    }

    private void setFallbackStats() {
        if (lblLiveAuctions == null || lblActiveBidders == null) return;
        lblLiveAuctions.setText("Live Auctions Now");
        lblActiveBidders.setText("Join 12,000+ active bidders");
    }

    @FXML
    public void handleGoogleLogin(ActionEvent event) {
        handleComingSoonButton(btnGoogle);
    }

    @FXML
    public void handleFacebookLogin(ActionEvent event) {
        handleComingSoonButton(btnFacebook);
    }

    private void handleComingSoonButton(Button button) {
        if (button == null) return;
        String originalText = button.getText();
        button.setDisable(true);
        button.setText("Chưa hỗ trợ");
        
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            Platform.runLater(() -> {
                button.setText(originalText);
                button.setDisable(false);
            });
        }).start();
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String loginField = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (loginField.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập Username/Email và Mật khẩu!");
            return;
        }

        try {
            HttpRequest request = buildLoginRequest(loginField, password);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Sai tài khoản hoặc mật khẩu!");
                logger.error("Lỗi khi connect đến server");
                return;
            }

            JSONObject responseJson = new JSONObject(response.body());

            if (responseJson.getInt("status") != 200) {
                showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Đăng nhập thất bại!");
                logger.error("Lỗi đăng nhập thất bại - status code: {}", responseJson.getInt("status"));
                return;
            }

            JSONObject data = responseJson.getJSONObject("data");
            String role = saveUserSession(data, loginField);

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chào mừng bạn đã quay lại!");
            logger.info("Đăng nhập thành công");

            if (!isTestEnvironment()) {
                switchSceneByRole(event, role);
            }
            
        } catch (Exception e) {
            logger.error("Không thể kết nối máy chủ: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    @FXML
    public void goToSignUp(ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "SignUp.fxml", 1000, 650);
    }

    @FXML
    public void handleForgotPassword(ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "ForgotPassword.fxml", 1000, 650);
    }

    public void setHttpClient(HttpClient httpClient) {
        this.client = httpClient;
    }

    private boolean isTestEnvironment() {
        return System.getProperty("surefire.test.class.path") != null;
    }

    private HttpRequest buildLoginRequest(String loginField, String password) {
        JSONObject body = new JSONObject();
        body.put("username", loginField);
        body.put("password", password);

        return HttpRequest.newBuilder()
                .uri(URI.create(Config.API_URL + "/api/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
    }

    private String saveUserSession(JSONObject data, String fallbackUsername) {
        int id = data.getInt("id");
        String username = data.optString("username", fallbackUsername);
        String fullname = data.optString("fullname", username);
        String email = data.optString("email", "");
        String dob = data.optString("dob", null);
        String placeOfBirth = data.optString("place_of_birth", null);
        String role = normalizeRole(data.optString("role", data.optString("accountType", DEFAULT_ROLE)));

        User.setSession(id, username, fullname, email, dob, placeOfBirth, role);
        return role;
    }

    private void switchSceneByRole(ActionEvent event, String role) throws IOException {
        String normalizedRole = normalizeRole(role);

        if (SELLER_ROLE.equals(normalizedRole)) {
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1000, 650);
        } else if (ADMIN_ROLE.equals(normalizedRole)) {
            SceneSwitcher.switchScene(event, "AdminDashboard.fxml", 1000, 650);
        } else {
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return DEFAULT_ROLE;
        }

        return role.trim().toLowerCase(Locale.ROOT);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}