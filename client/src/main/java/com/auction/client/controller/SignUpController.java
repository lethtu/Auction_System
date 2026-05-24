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
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import org.json.JSONObject;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.Animation;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.util.Duration;
import org.json.JSONArray;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.auction.client.util.AlertUtil;
import com.auction.client.util.TermsDialog;

public class SignUpController {
    private HttpClient client = HttpClientSingleton.getInstance().getHttpClient();
    private static final Logger logger = LoggerFactory.getLogger(SignUpController.class);

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String FALLBACK_PRODUCT_IMAGE = "https://lh3.googleusercontent.com/aida-public/AB6AXuDnjLyjfdx0TIrJ3KClDzo-UUmfFXR3GWEYTCSO6X9ti9b-0RO9z1W7Vx89MBn4k0mRqDddEzvljllw6_p3bb7EAg6b2Yuv8IMQsuaDPQpAPVp_8dc7hJ_3nzCa6Kngylg6UGYDmyhMycZpS5obRFBi1trtMdmnIV1ZHX9cyJ2N3Tlc_hhyxT8t9CQXTk4rQ84n8826ku4yedFwL93b-vWmrtGRNb6yhI0poCfKOiRxzEusfvKiZFPcuMeaaXQMS1em6ZNmS-2K-X6W";
    private static final DateTimeFormatter SERVER_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private record FeaturedProduct(int id, JSONObject sessionObj, JSONObject itemObj, String name, String type, String imageUrl, String priceText) {}

    private final List<FeaturedProduct> featuredProducts = new ArrayList<>();
    private int featuredProductIndex = 0;
    private Timeline productCarouselTimeline;
    private boolean isFirstProductShow = true;

    @FXML private StackPane activeProductCarousel;
    @FXML private ImageView activeProductImage;
    @FXML private Label activeProductType;
    @FXML private Label activeProductName;
    @FXML private Label activeProductPrice;
    @FXML private javafx.scene.layout.HBox imageSliderHBox;
    @FXML private javafx.scene.layout.VBox activeProductDetailsContainer;

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
        if (chkTerms != null) {
            chkTerms.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                if (!chkTerms.isSelected()) {
                    event.consume();
                    showTermsAndCheck(chkTerms, true);
                }
            });
            chkTerms.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.SPACE && !chkTerms.isSelected()) {
                    event.consume();
                    showTermsAndCheck(chkTerms, true);
                }
            });
        }
        if (activeProductCarousel != null) {
            activeProductCarousel.setCursor(javafx.scene.Cursor.HAND);
            activeProductCarousel.setPickOnBounds(true);
            activeProductCarousel.setOnMouseClicked(event -> {
                if (featuredProducts.isEmpty()) return;
                FeaturedProduct currentProd = featuredProducts.get(featuredProductIndex);
                if (currentProd != null && currentProd.sessionObj() != null && currentProd.itemObj() != null) {
                    try {
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

    private void updatePasswordStrength(String password) {
        if (strength1 == null) return;
        strength1.getStyleClass().removeAll("strength-empty", "strength-weak", "strength-medium", "strength-strong");
        strength2.getStyleClass().removeAll("strength-empty", "strength-weak", "strength-medium", "strength-strong");
        strength3.getStyleClass().removeAll("strength-empty", "strength-weak", "strength-medium", "strength-strong");
        
        if (password == null || password.length() < 6) {
            strength1.getStyleClass().add("strength-weak");
            strength2.getStyleClass().add("strength-empty");
            strength3.getStyleClass().add("strength-empty");
            lblStrength.setText("Password too weak (at least 6 characters required)");
            lblStrength.setStyle("-fx-text-fill: #e53e3e; -fx-font-family: 'DM Sans'; -fx-font-size: 11px;");
        } else if (password.length() < 10 || !password.matches(".*\\d.*") || !password.matches(".*[a-zA-Z].*")) {
            strength1.getStyleClass().add("strength-medium");
            strength2.getStyleClass().add("strength-medium");
            strength3.getStyleClass().add("strength-empty");
            lblStrength.setText("Medium password (add numbers & symbols)");
            lblStrength.setStyle("-fx-text-fill: #eab308; -fx-font-family: 'DM Sans'; -fx-font-size: 11px;");
        } else {
            strength1.getStyleClass().add("strength-strong");
            strength2.getStyleClass().add("strength-strong");
            strength3.getStyleClass().add("strength-strong");
            lblStrength.setText("Strong password");
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
            showAlert(Alert.AlertType.WARNING, "Notice", "Please fill in all fields!");
            return;
        }

        if (chkTerms != null && !chkTerms.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "You must agree to the terms!");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Error", "Password must be at least 6 characters!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Password Error", "Confirm password does not match!");
            return;
        }

        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password);
        json.put("email", email);
        json.put("fullname", fullname);
        String jsonBody = json.toString();
        logger.info(jsonBody);
        //Run on separate thread to avoid freezing UI
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
                        if (message.contains("success") || message.contains("success")) {
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Account registered successfully!");
                            logger.info("Account registration successful");
                            try {
                                goToLogin(event);
                            }
                            catch (IOException e) {
                                logger.error("Error switching view: {}", e.getMessage(), e);
                            }

                        }
                        else {
                            showAlert(Alert.AlertType.ERROR, "Failed", message);
                            logger.info("Registration failed");
                        }
                    });
                }
                else {
                    Platform.runLater(
                            () -> showAlert(Alert.AlertType.ERROR, "Server Error", "Error code: " + response.statusCode()));
                    logger.info("Registration failed - status: {}", response.statusCode());
                }

            }
            catch (Exception e) {
                Platform.runLater(
                        () -> showAlert(Alert.AlertType.ERROR, "Connection Error", "Cannot connect to the server!"));
                logger.error("Error connecting to server: {}", e.getMessage(), e);
            }
        }).start();
    }

    public void setHttpClient(HttpClient httpClient) {
        this.client = httpClient;
    }

    @FXML
    public void goToLogin(ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "Login.fxml", 1100, 700);
    }    private void showAlert(Alert.AlertType alertType, String title, String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showAlert(alertType, title, message));
            return;
        }
        AlertUtil.show(alertType, title, message);
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
                activeProductImage.setImage(new Image(imageUrl, true));
            }
            activeProductType.setText(product.type());
            activeProductName.setText(product.name());
            activeProductPrice.setText(product.priceText());
            isFirstProductShow = false;
            return;
        }

        if (imageSliderHBox != null && featuredProducts.contains(product)) {
            int index = featuredProducts.indexOf(product);
            double targetX = -index * 400.0;

            TranslateTransition slide = new TranslateTransition(Duration.millis(600), imageSliderHBox);
            slide.setToX(targetX);
            slide.play();
        }

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
            logger.warn("Failed to parse end time: {}", value);
            return "Ends: " + value;
        }
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

    private void showTermsAndCheck(javafx.scene.control.CheckBox checkBox, boolean isSignUp) {
        String title, subtitle, termsText;
        if (isSignUp) {
            title = "Terms & Conditions";
            subtitle = "Please read and scroll to the bottom to accept the BidPop terms.";
            termsText = "BidPop Marketplace Terms & Conditions\n\n"
                    + "Welcome to BidPop! By creating an account, you agree to comply with and be bound by the following terms of service. Please review them carefully.\n\n"
                    + "1. Acceptance of Terms\n"
                    + "By accessing or using BidPop, you agree to be bound by these Terms. If you do not agree, please do not use the application.\n\n"
                    + "2. Account Registration\n"
                    + "You must provide accurate and complete information during registration. You are responsible for keeping your credentials secure and confidential.\n\n"
                    + "3. User Conduct\n"
                    + "You agree not to engage in any prohibited activities, including bid shilling, fraud, or violating laws and rights of others.\n\n"
                    + "4. Termination\n"
                    + "We reserve the right to suspend or terminate accounts that violate our policies or engage in disruptive behavior.\n\n"
                    + "5. Liability & Disclaimers\n"
                    + "BidPop is provided 'as is' without warranties. We are not liable for transaction disputes between buyers and sellers.\n\n"
                    + "Please scroll to the bottom to confirm you have read and accepted these terms.";
        } else {
            title = "Seller Policy Agreement";
            subtitle = "Read the merchant terms before selling on BidPop.";
            termsText = "BidPop Seller Terms & Policies\n\n"
                    + "Welcome to the BidPop Seller Program! To upgrade your account and start selling, you must agree to these Seller Policies.\n\n"
                    + "1. Product Listings\n"
                    + "Sellers must describe their items accurately. Listing illegal, counterfeit, or prohibited items is strictly forbidden.\n\n"
                    + "2. Auction Rules\n"
                    + "Sellers agree to fulfill orders at the final winning bid. Shill bidding or manipulation is subject to immediate ban.\n\n"
                    + "3. Fees & Commissions\n"
                    + "BidPop charges fees on completed auctions. Detailed fee schedules are published under the Help Center.\n\n"
                    + "4. Dispute Resolution\n"
                    + "Sellers must cooperate with our support team to resolve buyer complaints. Failure to do so will affect your seller rating.\n\n"
                    + "Please scroll to the bottom to confirm you have read and accepted the seller policies.";
        }

        boolean accepted = TermsDialog.show(checkBox.getScene().getWindow(), title, subtitle, termsText);
        if (accepted) {
            checkBox.setSelected(true);
        } else {
            checkBox.setSelected(false);
        }
    }
}