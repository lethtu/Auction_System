package com.auction.client.controller;

import com.auction.client.Config;
import com.auction.client.HttpClientSingleton;
import com.auction.client.model.User;
import com.auction.client.util.AlertUtil;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.Animation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.application.Platform;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
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
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String FALLBACK_PRODUCT_IMAGE = "https://lh3.googleusercontent.com/aida-public/AB6AXuDnjLyjfdx0TIrJ3KClDzo-UUmfFXR3GWEYTCSO6X9ti9b-0RO9z1W7Vx89MBn4k0mRqDddEzvljllw6_p3bb7EAg6b2Yuv8IMQsuaDPQpAPVp_8dc7hJ_3nzCa6Kngylg6UGYDmyhMycZpS5obRFBi1trtMdmnIV1ZHX9cyJ2N3Tlc_hhyxT8t9CQXTk4rQ84n8826ku4yedFwL93b-vWmrtGRNb6yhI0poCfKOiRxzEusfvKiZFPcuMeaaXQMS1em6ZNmS-2K-X6W";
    private static final DateTimeFormatter SERVER_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private HttpClient client = HttpClientSingleton.getInstance().getHttpClient();
    private final List<FeaturedProduct> featuredProducts = new ArrayList<>();
    private int featuredProductIndex = 0;
    private Timeline productCarouselTimeline;
    private boolean isFirstProductShow = true;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML private Label lblLiveAuctions;
    @FXML private Label lblActiveBidders;
    @FXML private Button btnGoogle;
    @FXML private Button btnFacebook;
    @FXML private StackPane activeProductCarousel;
    @FXML private ImageView activeProductImage;
    @FXML private Label activeProductType;
    @FXML private Label activeProductName;
    @FXML private Label activeProductPrice;
    @FXML private javafx.scene.layout.HBox imageSliderHBox;
    @FXML private javafx.scene.layout.VBox activeProductDetailsContainer;

    @FXML
    public void initialize() {
        if (btnGoogle != null) {
            btnGoogle.setTooltip(new Tooltip("Tính năng đăng nhập bằng Google sẽ được bổ sung sau."));
        }
        if (btnFacebook != null) {
            btnFacebook.setTooltip(new Tooltip("Tính năng đăng nhập bằng Facebook sẽ được bổ sung sau."));
        }
        showFallbackProduct();
        loadActiveProducts();
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

                if (response != null && response.statusCode() == 200) {
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

    private void loadActiveProducts() {
        if (activeProductCarousel == null || activeProductImage == null) return;

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/auctions/all"))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response == null || response.statusCode() != 200) {
                    return;
                }

                JSONObject resJson = new JSONObject(response.body());
                if (resJson.optInt("status") != 200) {
                    return;
                }

                JSONArray sessions = resJson.optJSONArray("data");
                if (sessions == null || sessions.isEmpty()) {
                    return;
                }

                List<FeaturedProduct> activeProducts = new ArrayList<>();
                for (int i = 0; i < sessions.length(); i++) {
                    JSONObject session = sessions.optJSONObject(i);
                    if (session == null || !ACTIVE_STATUS.equalsIgnoreCase(session.optString("status"))) {
                        continue;
                    }
                    activeProducts.add(toFeaturedProduct(session));
                    if (activeProducts.size() >= 8) {
                        break;
                    }
                }

                if (!activeProducts.isEmpty()) {
                    Platform.runLater(() -> setFeaturedProducts(activeProducts));
                }
            } catch (Exception e) {
                logger.warn("Failed to load active product carousel", e);
            }
        }).start();
    }

    private FeaturedProduct toFeaturedProduct(JSONObject session) {
        String name = session.optString("productName", "Live auction item");
        String type = session.optString("productType", "Active auction");
        String imageUrl = buildImageUrl(session.optString("imagePath", ""));
        String endTimeText = formatEndTime(session.optString("endTime", ""));

        return new FeaturedProduct(name, type, imageUrl, endTimeText);
    }

    private void setFeaturedProducts(List<FeaturedProduct> products) {
        featuredProducts.clear();
        featuredProducts.addAll(products);
        featuredProductIndex = 0;

        if (imageSliderHBox != null) {
            imageSliderHBox.getChildren().clear();
            for (FeaturedProduct prod : products) {
                StackPane imageHolder = new StackPane();
                imageHolder.setPrefSize(400.0, 230.0);
                imageHolder.setMaxSize(400.0, 230.0);
                imageHolder.setMinSize(400.0, 230.0);

                ImageView imgView = new ImageView();
                imgView.setFitWidth(360.0);
                imgView.setFitHeight(210.0);
                imgView.setPreserveRatio(true);
                imgView.setSmooth(true);

                String imageUrl = prod.imageUrl().isBlank() ? FALLBACK_PRODUCT_IMAGE : prod.imageUrl();
                imgView.setImage(new Image(imageUrl, true));

                imageHolder.getChildren().add(imgView);
                imageSliderHBox.getChildren().add(imageHolder);
            }
        }

        showFeaturedProduct(featuredProducts.get(featuredProductIndex));
        startProductCarousel();
    }

    private void startProductCarousel() {
        if (productCarouselTimeline != null) {
            productCarouselTimeline.stop();
        }
        if (featuredProducts.size() <= 1) {
            return;
        }

        productCarouselTimeline = new Timeline(new KeyFrame(Duration.seconds(3.5), event -> showNextProduct()));
        productCarouselTimeline.setCycleCount(Animation.INDEFINITE);
        productCarouselTimeline.play();
    }

    private void showNextProduct() {
        if (featuredProducts.isEmpty()) return;
        featuredProductIndex = (featuredProductIndex + 1) % featuredProducts.size();
        showFeaturedProduct(featuredProducts.get(featuredProductIndex));
    }

    private void showFallbackProduct() {
        showFeaturedProduct(new FeaturedProduct(
                "Discover live auctions",
                "Featured marketplace",
                FALLBACK_PRODUCT_IMAGE,
                "Kết thúc: đang cập nhật"
        ));
    }

    private void showFeaturedProduct(FeaturedProduct product) {
        if (product == null) return;

        if (isFirstProductShow) {
            String imageUrl = product.imageUrl().isBlank() ? FALLBACK_PRODUCT_IMAGE : product.imageUrl();
            if (activeProductImage != null) {
                activeProductImage.setImage(new Image(imageUrl, true));
            }
            activeProductType.setText(product.type());
            activeProductName.setText(product.name());
            activeProductPrice.setText(product.priceText());
            isFirstProductShow = false;
            return;
        }

        // Slide the image HBox container horizontally
        if (imageSliderHBox != null && featuredProducts.contains(product)) {
            int index = featuredProducts.indexOf(product);
            double targetX = -index * 400.0;

            TranslateTransition slide = new TranslateTransition(Duration.millis(600), imageSliderHBox);
            slide.setToX(targetX);
            slide.play();
        }

        // Cross-fade text details smoothly
        if (activeProductDetailsContainer != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), activeProductDetailsContainer);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                activeProductType.setText(product.type());
                activeProductName.setText(product.name());
                activeProductPrice.setText(product.priceText());

                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), activeProductDetailsContainer);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        } else {
            activeProductType.setText(product.type());
            activeProductName.setText(product.name());
            activeProductPrice.setText(product.priceText());
        }
    }

    private String buildImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }

        String path = imagePath.trim().replace("\\", "/");
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (path.startsWith("server/upload/images/")) {
            path = path.substring("server/upload/images/".length());
        }
        if (path.startsWith("upload/images/")) {
            path = path.substring("upload/images/".length());
        }
        if (path.startsWith("images/")) {
            path = path.substring("images/".length());
        }

        return path.isBlank() ? "" : Config.API_URL + "/api/files/images/" + path;
    }

    private String formatEndTime(String value) {
        if (value == null || value.isBlank()) {
            return "Kết thúc: đang cập nhật";
        }

        try {
            LocalDateTime endTime = LocalDateTime.parse(value.trim(), SERVER_DATE_TIME);
            return "Kết thúc: " + DISPLAY_DATE_TIME.format(endTime);
        } catch (DateTimeParseException e) {
            return "Kết thúc: " + value.trim();
        }
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
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1280, 800);
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
        if (isTestEnvironment()) {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            return;
        }

        AlertUtil.show(alertType, title, message);
    }

    private record FeaturedProduct(String name, String type, String imageUrl, String priceText) {
    }
}
