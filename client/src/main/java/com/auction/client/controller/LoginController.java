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
import javafx.fxml.FXMLLoader;
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


import javafx.scene.control.CheckBox;
import java.util.prefs.Preferences;
import com.auction.client.util.GoogleOAuthService;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private static final Preferences LOGIN_PREFS = Preferences.userNodeForPackage(LoginController.class).node("login");
    private static final String PREF_REMEMBER_ME = "rememberMe";
    private static final String PREF_LOGIN_FIELD = "loginField";
    private static final String PREF_PASSWORD = "password";

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

    @FXML
    private CheckBox rememberMeCheckBox;

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
        loadRememberedLogin();
        if (btnGoogle != null) {
            btnGoogle.setTooltip(new Tooltip("Sign in using your Google account"));
        }
        if (btnFacebook != null) {
            btnFacebook.setTooltip(new Tooltip("Facebook login will be added in a future update."));
        }
        if (activeProductCarousel != null) {
            activeProductCarousel.setCursor(javafx.scene.Cursor.HAND);
            activeProductCarousel.setPickOnBounds(true);
            activeProductCarousel.setOnMouseClicked(event -> {
                if (featuredProducts.isEmpty()) return;
                FeaturedProduct currentProd = featuredProducts.get(featuredProductIndex);
                if (currentProd != null && currentProd.sessionObj() != null && currentProd.itemObj() != null) {
                    try {
                        User.setSession(null, "guest", "Guest", "guest@bidpop.com", null, null, "guest", null);
                        FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 1280, 800);
                        AuctionPageController controller = loader.getController();
                        controller.setItem(currentProd.sessionObj(), currentProd.itemObj());
                    } catch (IOException e) {
                        logger.error("Failed to open auction page from carousel", e);
                    }
                }
            });
        }
        showFallbackProduct();
        loadActiveProducts();
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
        int id = session.optInt("id", 0);
        JSONObject itemObj = session.optJSONObject("item");
        if (itemObj == null) {
            itemObj = new JSONObject();
            itemObj.put("name", session.optString("productName", ""));
            itemObj.put("type", session.optString("productType", ""));
            itemObj.put("description", session.optString("description", ""));
            itemObj.put("imagePath", session.optString("imagePath", ""));
        }
        String name = session.optString("productName", "Live auction item");
        String type = session.optString("productType", "Active auction");
        String imageUrl = buildImageUrl(session.optString("imagePath", ""));
        String endTimeText = formatEndTime(session.optString("endTime", ""));

        return new FeaturedProduct(id, session, itemObj, name, type, imageUrl, endTimeText);
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
                imgView.setImage(new Image(imageUrl, 360.0, 210.0, true, true, true));

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
                0,
                null,
                null,
                "Discover live auctions",
                "Featured marketplace",
                FALLBACK_PRODUCT_IMAGE,
                "Ends: updating"
        ));
    }

    private void showFeaturedProduct(FeaturedProduct product) {
        if (product == null) return;

        if (isFirstProductShow) {
            String imageUrl = product.imageUrl().isBlank() ? FALLBACK_PRODUCT_IMAGE : product.imageUrl();
            if (activeProductImage != null) {
                setActiveProductImage(imageUrl);
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


    private void setActiveProductImage(String imageUrl) {
        if (activeProductImage == null) {
            return;
        }
        String safeUrl = (imageUrl == null || imageUrl.isBlank()) ? FALLBACK_PRODUCT_IMAGE : imageUrl;
        Image image = new Image(safeUrl, 360.0, 210.0, true, true, true);
        activeProductImage.setImage(image);
    }
    private String buildImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }

        String path = imagePath.trim().replace("\\", "/");
        if ((path.startsWith("http://") || path.startsWith("https://")) && !path.contains("/api/files/images/")) {
            return Config.applyCacheBuster(path);
        }
        int apiIndex = path.indexOf("/api/files/images/");
        if (apiIndex >= 0) {
            path = path.substring(apiIndex + "/api/files/images/".length());
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

        String url = path.isBlank() ? "" : Config.API_URL + "/api/files/images/" + path;
        return Config.applyCacheBuster(url);
    }

    private String formatEndTime(String value) {
        if (value == null || value.isBlank()) {
            return "Ends: updating";
        }

        try {
            LocalDateTime endTime = LocalDateTime.parse(value.trim(), SERVER_DATE_TIME);
            return "Ends: " + DISPLAY_DATE_TIME.format(endTime);
        } catch (DateTimeParseException e) {
            return "Ends: " + value.trim();
        }
    }

    @FXML
    public void handleGoogleLogin(ActionEvent event) {
        if (btnGoogle == null) return;
        btnGoogle.setDisable(true);
        new Thread(() -> {
            try {
                HttpRequest configRequest = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/auth/google/config"))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(configRequest, HttpResponse.BodyHandlers.ofString());
                if (response == null || response.statusCode() != 200) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Google Login Error", "Failed to retrieve configuration from server.");
                        btnGoogle.setDisable(false);
                    });
                    return;
                }

                JSONObject res = new JSONObject(response.body());
                JSONObject config = res.optJSONObject("data");
                if (config == null) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Google Login Error", "Invalid configuration returned by server.");
                        btnGoogle.setDisable(false);
                    });
                    return;
                }

                boolean isMock = config.optBoolean("mock", false);
                String clientIdStr = config.optString("clientId", "");

                if (isMock) {
                    Platform.runLater(() -> {
                        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("test-google@gmail.com");
                        dialog.setTitle("Developer Mock Google Login");
                        dialog.setHeaderText("Google OAuth is set to mock mode.\nEnter any email to simulate Google authentication.");
                        dialog.setContentText("Email:");

                        dialog.showAndWait().ifPresent(email -> {
                            new Thread(() -> submitGoogleLoginPayload(event, email, "Mock Google User", null, null)).start();
                        });
                        btnGoogle.setDisable(false);
                    });
                } else {
                    Platform.runLater(() -> {
                        GoogleOAuthService oauthService = new GoogleOAuthService();
                        oauthService.startAuthorizationFlow(clientIdStr, new GoogleOAuthService.AuthorizationCallback() {
                            @Override
                            public void onSuccess(String code, String redirectUri) {
                                new Thread(() -> submitGoogleLoginPayload(event, null, null, code, redirectUri)).start();
                            }

                            @Override
                            public void onFailure(String error) {
                                Platform.runLater(() -> {
                                    showAlert(Alert.AlertType.ERROR, "Google Login Failed", "Authorization failed: " + error);
                                    btnGoogle.setDisable(false);
                                });
                            }
                        });
                    });
                }
            } catch (Exception e) {
                logger.error("Error starting Google Login", e);
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Network Error", "Cannot connect to server for Google config!");
                    btnGoogle.setDisable(false);
                });
            }
        }).start();
    }

    private void submitGoogleLoginPayload(ActionEvent event, String email, String name, String code, String redirectUri) {
        try {
            JSONObject body = new JSONObject();
            if (email != null) {
                body.put("email", email);
            }
            if (name != null) {
                body.put("name", name);
            }
            if (code != null) {
                body.put("code", code);
            }
            if (redirectUri != null) {
                body.put("redirectUri", redirectUri);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/auth/google"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response == null || response.statusCode() != 200) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Google Login Failed", "Server rejected Google authentication.");
                    btnGoogle.setDisable(false);
                });
                return;
            }

            JSONObject responseJson = new JSONObject(response.body());
            if (responseJson.getInt("status") != 200) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Google Login Failed", responseJson.optString("message", "Login failed"));
                    btnGoogle.setDisable(false);
                });
                return;
            }

            JSONObject data = responseJson.getJSONObject("data");
            String role = saveUserSession(data, email != null ? email : "google_user");

            logger.info("Google Login successful, user role: {}", role);

            Platform.runLater(() -> {
                try {
                    btnGoogle.setDisable(false);
                    if (!isTestEnvironment()) {
                        switchSceneByRole(event, role);
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Welcome back!");
                    }
                } catch (IOException e) {
                    logger.error("Navigation error after Google Login", e);
                }
            });

        } catch (Exception e) {
            logger.error("Google Login exchange error", e);
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Network Error", "Cannot connect to server to exchange Google credentials!");
                btnGoogle.setDisable(false);
            });
        }
    }

    @FXML
    public void handleFacebookLogin(ActionEvent event) {
        handleComingSoonButton(btnFacebook);
    }

    private void handleComingSoonButton(Button button) {
        if (button == null) return;
        String originalText = button.getText();
        button.setDisable(true);
        button.setText("Not supported");

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
            showAlert(Alert.AlertType.WARNING, "Error", "Please enter Username/Email and Password!");
            return;
        }

        try {
            HttpRequest request = buildLoginRequest(loginField, password);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                showAlert(Alert.AlertType.ERROR, "Login Error", "Invalid account or password!");
                logger.error("Error connecting to server");
                return;
            }

            JSONObject responseJson = new JSONObject(response.body());

            if (responseJson.getInt("status") != 200) {
                showAlert(Alert.AlertType.ERROR, "Login Error", "Login failed!");
                logger.error("Login failed - status code: {}", responseJson.getInt("status"));
                return;
            }

            JSONObject data = responseJson.getJSONObject("data");
            String role = saveUserSession(data, loginField);
            saveRememberedLoginChoice(loginField, password);

            logger.info("Login successful");

            if (!isTestEnvironment()) {
                switchSceneByRole(event, role);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Welcome back!");
            }

        } catch (Exception e) {
            logger.error("Cannot connect to server: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Network Error", "Cannot connect to the server!");
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
        String avatarUrl = data.optString("avatarUrl", data.optString("avatar_url", null));
        if ("null".equals(avatarUrl)) avatarUrl = null;

        User.setSessionToken(data.optString("sessionToken", null));
        User.setSession(id, username, fullname, email, dob, placeOfBirth, role, avatarUrl);
        User.setPasswordSet(data.optBoolean("passwordSet", true));

        try {
            com.auction.client.service.NotificationSocketService.getInstance().start(id);
        } catch (Exception e) {
            logger.error("Failed to start global NotificationSocketService: {}", e.getMessage());
        }

        return role;
    }

    private void switchSceneByRole(ActionEvent event, String role) throws IOException {
        String normalizedRole = normalizeRole(role);

        if (SELLER_ROLE.equals(normalizedRole)) {
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1200, 800);
        } else if (ADMIN_ROLE.equals(normalizedRole)) {
            SceneSwitcher.switchScene(event, "AdminDashboard.fxml", 1200, 800);
        } else {
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1200, 800);
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

    private record FeaturedProduct(int id, JSONObject sessionObj, JSONObject itemObj, String name, String type, String imageUrl, String priceText) {
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

    private void loadRememberedLogin() {
        if (rememberMeCheckBox == null) {
            return;
        }

        boolean remember = LOGIN_PREFS.getBoolean(PREF_REMEMBER_ME, false);
        rememberMeCheckBox.setSelected(remember);

        if (remember) {
            String savedLogin = LOGIN_PREFS.get(PREF_LOGIN_FIELD, "");
            String savedPassword = LOGIN_PREFS.get(PREF_PASSWORD, "");

            if (txtUsername != null) {
                txtUsername.setText(savedLogin);
            }
            if (txtPassword != null) {
                txtPassword.setText(savedPassword);
            }
        }

        rememberMeCheckBox.selectedProperty().addListener((observable, oldValue, selected) -> {
            if (!selected) {
                clearRememberedLogin();
            }
        });
    }

    private void saveRememberedLoginChoice(String loginField, String password) {
        if (rememberMeCheckBox != null && rememberMeCheckBox.isSelected()) {
            LOGIN_PREFS.putBoolean(PREF_REMEMBER_ME, true);
            LOGIN_PREFS.put(PREF_LOGIN_FIELD, loginField == null ? "" : loginField);
            LOGIN_PREFS.put(PREF_PASSWORD, password == null ? "" : password);
        } else {
            clearRememberedLogin();
        }
    }

    private void clearRememberedLogin() {
        LOGIN_PREFS.putBoolean(PREF_REMEMBER_ME, false);
        LOGIN_PREFS.remove(PREF_LOGIN_FIELD);
        LOGIN_PREFS.remove(PREF_PASSWORD);
    }
}
