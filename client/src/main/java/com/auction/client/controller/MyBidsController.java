package com.auction.client.controller;

import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import com.auction.client.model.User;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.math.BigDecimal;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyBidsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MyBidsController.class);

    private HttpClient client = HttpClient.newHttpClient();

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private FlowPane productContainer;
    @FXML
    private Button btnTabActive;
    @FXML
    private Button btnTabWinning;
    @FXML
    private Button btnTabOutbid;
    @FXML
    private Button btnTabEnded;

    @FXML
    private SidebarController sidebarController;
    @FXML
    private TopbarController topbarController;

    private TextField txtSearch;

    private enum Tab {
        ACTIVE,
        WINNING,
        OUTBID,
        ENDED
    }

    private Tab currentTab = Tab.ACTIVE;

    private final List<JSONObject> allProducts = new ArrayList<>();
    private final Map<String, Image> imageCache = new ConcurrentHashMap<>();
    private ScheduledExecutorService pollingScheduler;
    private final List<String> currentRenderedStates = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (topbarController != null && sidebarController != null) {
            topbarController.setSidebarController(sidebarController);
        }

        if (topbarController != null) {
            this.txtSearch = topbarController.getTxtSearch();
        }

        if (txtSearch != null) {
            txtSearch.setPromptText("Search your bids...");
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterAndRenderProducts());
        }

        // Stable responsive spacing. Keep layout fixed across focus/click refreshes.
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            updateGridLayout();
        });
        updateGridLayout();
        Platform.runLater(this::updateGridLayout);

        // Initialize sidebar styling
        Platform.runLater(() -> {
            if (sidebarController != null) {
                sidebarController.setActiveMyBids();
            }
        });

        // Setup sidebar listener for navigation resets
        if (sidebarController != null) {
            sidebarController.setSidebarListener(new SidebarController.SidebarListener() {
                @Override
                public void onFilterWatchlist(ActionEvent event) {
                    try {
                        MainController.initialShowWatchlist = true;
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (IOException e) {
                        logger.error("Navigation error:", e);
                    }
                }

                @Override
                public void onResetFilter(ActionEvent event) {
                    try {
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (IOException e) {
                        logger.error("Navigation error:", e);
                    }
                }
            });
        }

        updateTabStyles();
        if (System.getProperty("surefire.test.class.path") == null) {
            startPolling();
        }
    }

    private void updateGridLayout() {
        if (scrollPane == null || productContainer == null)
            return;

        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        if (viewportWidth <= 0 && scrollPane.getWidth() > 0) {
            viewportWidth = scrollPane.getWidth();
        }
        if (viewportWidth <= 0)
            return;

        final double cardWidth = 250.0;
        final double hgap = 24.0;
        final int maxColumns = 8;

        int columns = Math.max(1, Math.min(maxColumns, (int) Math.floor((viewportWidth + hgap) / (cardWidth + hgap))));
        double gridWidth = columns * cardWidth + Math.max(0, columns - 1) * hgap;

        productContainer.setAlignment(Pos.TOP_LEFT);
        productContainer.setPrefWrapLength(gridWidth);
        productContainer.setMinWidth(gridWidth);
        productContainer.setPrefWidth(gridWidth);
        productContainer.setMaxWidth(gridWidth);
        productContainer.setHgap(hgap);
        productContainer.setVgap(28.0);
        productContainer.setPadding(new Insets(10.0, 0.0, 18.0, 0.0));
    }

    private void startPolling() {
        pollingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        pollingScheduler.scheduleAtFixedRate(this::fetchProductsData, 0, 5, TimeUnit.SECONDS);
    }

    private void fetchProductsData() {
        try {
            Integer userId = User.getId();
            if (userId == null) {
                logger.warn("[MyBids-DEBUG] userId is NULL, skipping fetch");
                return;
            }
            logger.info("[MyBids-DEBUG] Fetching my-bids for userId={}", userId);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/bidder/my-bids?bidderId=" + userId))
                    .GET();
            if (User.getSessionToken() != null) {
                builder.header("X-Auth-Token", User.getSessionToken());
            }
            HttpRequest request = builder.build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("[MyBids-DEBUG] HTTP status={}, body length={}", response.statusCode(),
                    response.body().length());
            logger.info("[MyBids-DEBUG] Response body (first 500 chars): {}",
                    response.body().substring(0, Math.min(500, response.body().length())));

            if (response.statusCode() == 200) {
                JSONObject responseJson = new JSONObject(response.body());
                int apiStatus = responseJson.getInt("status");
                logger.info("[MyBids-DEBUG] API status={}", apiStatus);

                if (apiStatus == 200) {
                    Object dataObj = responseJson.get("data");
                    logger.info("[MyBids-DEBUG] data type={}", dataObj.getClass().getSimpleName());
                    JSONArray jsonArray = new JSONArray();

                    if (dataObj instanceof JSONObject) {
                        jsonArray = ((JSONObject) dataObj).getJSONArray("content");
                    } else if (dataObj instanceof JSONArray) {
                        jsonArray = (JSONArray) dataObj;
                    }

                    logger.info("[MyBids-DEBUG] jsonArray length={}", jsonArray.length());
                    if (jsonArray.length() > 0) {
                        logger.info("[MyBids-DEBUG] First item: {}", jsonArray.getJSONObject(0).toString().substring(0,
                                Math.min(300, jsonArray.getJSONObject(0).toString().length())));
                    }

                    List<JSONObject> newProducts = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        newProducts.add(jsonArray.getJSONObject(i));
                    }

                    allProducts.clear();
                    allProducts.addAll(newProducts);
                    logger.info("[MyBids-DEBUG] allProducts size={}, currentTab={}", allProducts.size(), currentTab);

                    Platform.runLater(this::filterAndRenderProducts);
                } else {
                    logger.warn("[MyBids-DEBUG] API returned non-200 status: {}", apiStatus);
                }
            } else {
                logger.warn("[MyBids-DEBUG] HTTP returned non-200 status: {}", response.statusCode());
            }
        } catch (Exception e) {
            logger.error("Error loading products: {}", e.getMessage(), e);
        }
    }

    private void filterAndRenderProducts() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        Integer currentUserId = User.getId();

        if (currentUserId == null) {
            productContainer.getChildren().clear();
            currentRenderedStates.clear();
            return;
        }

        Platform.runLater(() -> {
            List<String> newStatesToRender = new ArrayList<>();

            for (JSONObject sessionObj : allProducts) {
                JSONObject itemObj = getItemObject(sessionObj);
                String name = itemObj.optString("name", "");
                String status = normalizeSessionStatus(sessionObj);
                int highestBidderId = sessionObj.optInt("highestBidderId", -1);
                BigDecimal currentPrice = getMoney(sessionObj, "currentPrice",
                        getMoney(sessionObj, "startingPrice", BigDecimal.ZERO));

                boolean matchKeyword = keyword.isEmpty() || name.toLowerCase().contains(keyword);
                boolean matchTab = false;
                switch (currentTab) {
                    case ACTIVE:
                        matchTab = isActiveSession(sessionObj);
                        break;
                    case WINNING:
                        matchTab = isWinningSession(sessionObj, currentUserId);
                        break;
                    case OUTBID:
                        matchTab = isOutbidSession(sessionObj, currentUserId);
                        break;
                    case ENDED:
                        matchTab = isEndedSession(sessionObj);
                        break;
                }
                if (matchKeyword && matchTab) {
                    newStatesToRender.add(getRenderedStateKey(sessionObj, currentPrice, highestBidderId));
                }
            }

            if (!currentRenderedStates.equals(newStatesToRender)) {
                productContainer.getChildren().clear();
                currentRenderedStates.clear();

                for (JSONObject sessionObj : allProducts) {
                    JSONObject itemObj = getItemObject(sessionObj);
                    String name = itemObj.optString("name", "");
                    String status = normalizeSessionStatus(sessionObj);
                    int highestBidderId = sessionObj.optInt("highestBidderId", -1);
                    BigDecimal currentPrice = getMoney(sessionObj, "currentPrice",
                            getMoney(sessionObj, "startingPrice", BigDecimal.ZERO));

                    boolean matchKeyword = keyword.isEmpty() || name.toLowerCase().contains(keyword);
                    boolean matchTab = false;
                    switch (currentTab) {
                        case ACTIVE:
                            matchTab = isActiveSession(sessionObj);
                            break;
                        case WINNING:
                            matchTab = isWinningSession(sessionObj, currentUserId);
                            break;
                        case OUTBID:
                            matchTab = isOutbidSession(sessionObj, currentUserId);
                            break;
                        case ENDED:
                            matchTab = isEndedSession(sessionObj);
                            break;
                    }
                    if (matchKeyword && matchTab) {
                        VBox card = createProductCard(sessionObj, itemObj);
                        productContainer.getChildren().add(card);
                        currentRenderedStates.add(getRenderedStateKey(sessionObj, currentPrice, highestBidderId));
                    }
                }
                updateGridLayout();
            }
        });
    }

    private String normalizeSessionStatus(JSONObject sessionObj) {
        if (sessionObj == null) {
            return "ACTIVE";
        }
        String status = sessionObj.optString("status", "ACTIVE");
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        return status.trim().toUpperCase();
    }

    private boolean isActiveSession(JSONObject sessionObj) {
        return "ACTIVE".equals(normalizeSessionStatus(sessionObj));
    }

    private boolean isEndedSession(JSONObject sessionObj) {
        String status = normalizeSessionStatus(sessionObj);
        if ("ACTIVE".equals(status)
                || "COMING".equals(status)
                || "UPCOMING".equals(status)
                || "PENDING".equals(status)
                || "DRAFT".equals(status)
                || "APPROVED".equals(status)
                || "WAITING".equals(status)) {
            return false;
        }
        return "ENDED".equals(status)
                || "CLOSED".equals(status)
                || "COMPLETED".equals(status)
                || "FINISHED".equals(status)
                || "EXPIRED".equals(status)
                || "CANCELED".equals(status)
                || "CANCELLED".equals(status)
                || !status.isBlank();
    }

    private boolean isWinningSession(JSONObject sessionObj, int currentUserId) {
        return isActiveSession(sessionObj) && sessionObj.optInt("highestBidderId", -1) == currentUserId;
    }

    private boolean isOutbidSession(JSONObject sessionObj, int currentUserId) {
        return isActiveSession(sessionObj) && sessionObj.optInt("highestBidderId", -1) != currentUserId;
    }

    private String getRenderedStateKey(JSONObject sessionObj, BigDecimal currentPrice, int highestBidderId) {
        return sessionObj.optInt("id") + "_" + currentPrice + "_" + highestBidderId + "_"
                + normalizeSessionStatus(sessionObj);
    }

    private boolean isActiveStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim();
        return "ACTIVE".equalsIgnoreCase(normalized)
                || "ONGOING".equalsIgnoreCase(normalized)
                || "LIVE".equalsIgnoreCase(normalized)
                || "OPEN".equalsIgnoreCase(normalized);
    }

    private boolean isEndedStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim();
        return "ENDED".equalsIgnoreCase(normalized)
                || "CLOSED".equalsIgnoreCase(normalized)
                || "COMPLETED".equalsIgnoreCase(normalized)
                || "FINISHED".equalsIgnoreCase(normalized)
                || "CANCELLED".equalsIgnoreCase(normalized)
                || "CANCELED".equalsIgnoreCase(normalized);
    }

    private VBox createProductCard(JSONObject sessionObj, JSONObject itemObj) {
        int id = sessionObj.getInt("id");

        String type = itemObj.optString("type", "");
        String name = itemObj.optString("name", "");
        BigDecimal currentPrice = getMoney(sessionObj, "currentPrice",
                getMoney(sessionObj, "startingPrice", BigDecimal.ZERO));
        String status = normalizeSessionStatus(sessionObj);
        String imagePath = itemObj.optString("imagePath", "default.png");
        int highestBidderId = sessionObj.optInt("highestBidderId", -1);
        int currentUserId = User.getId() != null ? User.getId() : -1;
        boolean activeSession = isActiveSession(sessionObj);
        boolean endedSession = isEndedSession(sessionObj);
        boolean winningSession = isWinningSession(sessionObj, currentUserId);
        boolean outbidSession = isOutbidSession(sessionObj, currentUserId);

        VBox vbox = new VBox();
        vbox.setSpacing(14.0);
        vbox.setPrefWidth(250.0);
        vbox.setMinWidth(250.0);
        vbox.setMaxWidth(250.0);
        vbox.setPrefHeight(400.0);
        vbox.setMinHeight(400.0);
        vbox.setMaxHeight(400.0);

        // Premium modern style
        vbox.setStyle(
                "-fx-border-color: #ffe8e8; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 14px; -fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.05), 10, 0, 0, 2);");

        // Interactive hover scaling and drop shadow micro-animation
        vbox.setOnMouseEntered(e -> {
            vbox.setStyle(
                    "-fx-border-color: #ffd6ee; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 14px; -fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.15), 15, 0, 0, 4); -fx-scale-x: 1.02; -fx-scale-y: 1.02; -fx-cursor: hand;");
        });
        vbox.setOnMouseExited(e -> {
            vbox.setStyle(
                    "-fx-border-color: #ffe8e8; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 14px; -fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.05), 10, 0, 0, 2); -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
        });

        // Image container with clipping and shadow
        StackPane imageWrapper = new StackPane();
        imageWrapper.setPrefHeight(150.0);
        imageWrapper.setMinHeight(150.0);
        imageWrapper.setMaxHeight(150.0);
        imageWrapper.setPrefWidth(222.0);
        imageWrapper.setMinWidth(222.0);
        imageWrapper.setMaxWidth(222.0);
        imageWrapper.setStyle("-fx-background-radius: 14px; -fx-border-radius: 14px; -fx-background-color: #fcf6fc;");

        ImageView imageView = new ImageView();
        imageView.setFitHeight(150.0);
        imageView.setFitWidth(222.0);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Label imageStatusLabel = new Label("No Image");
        imageStatusLabel.setAlignment(Pos.CENTER);
        imageStatusLabel.setStyle("-fx-text-fill: #adb5bd;");

        String imageUrl = buildImageUrl(imagePath);
        imageWrapper.getChildren().add(imageStatusLabel);
        if (!imageUrl.isBlank()) {
            Image cached = imageCache.get(imageUrl);
            if (cached == null || cached.isError()) {
                cached = new Image(imageUrl, true);
                imageCache.put(imageUrl, cached);
            }
            imageView.setImage(cached);
            imageWrapper.getChildren().add(imageView);
            cached.errorProperty().addListener((obs, oldValue, isError) -> {
                if (isError) {
                    imageWrapper.getChildren().remove(imageView);
                    if (!imageWrapper.getChildren().contains(imageStatusLabel)) {
                        imageWrapper.getChildren().add(0, imageStatusLabel);
                    }
                }
            });
        }

        // Clip the image wrapper to keep rounded corners
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(222, 150);
        clip.setArcWidth(28.0);
        clip.setArcHeight(28.0);
        imageWrapper.setClip(clip);

        // Status Badge (Top-Left)
        HBox statusBadge = new HBox(4.0);
        statusBadge.setAlignment(Pos.CENTER);
        statusBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(statusBadge, Pos.TOP_LEFT);
        StackPane.setMargin(statusBadge, new Insets(10, 0, 0, 10));

        Region dot = new Region();
        dot.setPrefSize(8, 8);
        dot.setMinSize(8, 8);
        dot.setMaxSize(8, 8);

        Label badgeLabel = new Label();
        badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        if (endedSession) {
            if (highestBidderId == currentUserId) {
                statusBadge.setStyle(
                        "-fx-background-color: rgba(16, 185, 129, 0.15); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 12px;");
                badgeLabel.setText("Won");
                badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #10b981;");
                dot.setStyle("-fx-background-color: #10b981; -fx-background-radius: 4px;");
                statusBadge.getChildren().setAll(dot, badgeLabel);
            } else {
                statusBadge.setStyle(
                        "-fx-background-color: rgba(108, 117, 125, 0.15); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: rgba(108, 117, 125, 0.3); -fx-border-radius: 12px;");
                badgeLabel.setText("Ended");
                badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #6c757d;");
                statusBadge.getChildren().setAll(badgeLabel);
            }
        } else if (winningSession) {
            statusBadge.setStyle(
                    "-fx-background-color: rgba(16, 185, 129, 0.15); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 12px;");
            badgeLabel.setText("Winning");
            badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #10b981;");
            dot.setStyle("-fx-background-color: #10b981; -fx-background-radius: 4px;");
            statusBadge.getChildren().setAll(dot, badgeLabel);
        } else if (outbidSession) {
            statusBadge.setStyle(
                    "-fx-background-color: rgba(239, 68, 68, 0.15); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: rgba(239, 68, 68, 0.3); -fx-border-radius: 12px;");
            badgeLabel.setText("Outbid");
            badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #ef4444;");
            Label warningIcon = new Label("\uE002");
            warningIcon.setStyle(
                    "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 14px; -fx-text-fill: #ef4444;");
            statusBadge.getChildren().setAll(warningIcon, badgeLabel);
        } else {
            statusBadge.setStyle(
                    "-fx-background-color: rgba(224, 64, 160, 0.12); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: rgba(224, 64, 160, 0.25); -fx-border-radius: 12px;");
            badgeLabel.setText(status);
            badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #e040a0;");
            statusBadge.getChildren().setAll(badgeLabel);
        }
        imageWrapper.getChildren().remove(statusBadge);
        imageWrapper.getChildren().add(statusBadge);

        if (activeSession) {
            HBox timeBadge = new HBox(4.0);
            timeBadge.setAlignment(Pos.CENTER);
            timeBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            StackPane.setAlignment(timeBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(timeBadge, new Insets(10, 10, 0, 0));
            timeBadge.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: #ffe8e8; -fx-border-radius: 12px;");

            Label timerIcon = new Label("\uE425");
            timerIcon.setStyle(
                    "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 14px; -fx-text-fill: #e040a0;");

            Label timeLabel = new Label("Active");
            timeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #e040a0;");
            timeBadge.getChildren().addAll(timerIcon, timeLabel);
            imageWrapper.getChildren().add(timeBadge);
        }

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2e1a28;");
        nameLabel.setWrapText(true);
        nameLabel.setPrefHeight(44.0);
        nameLabel.setMaxHeight(44.0);

        Label categoryLabel = new Label(type);
        categoryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7c52aa; -fx-font-weight: bold;");

        VBox bidDetailsBox = new VBox(6.0);
        if (outbidSession) {
            bidDetailsBox.setStyle(
                    "-fx-background-color: rgba(239, 68, 68, 0.05); -fx-background-radius: 12px; -fx-padding: 10px; -fx-border-color: rgba(239, 68, 68, 0.1); -fx-border-width: 1px; -fx-border-radius: 12px;");
        } else {
            bidDetailsBox.setStyle("-fx-background-color: #f8eef8; -fx-background-radius: 12px; -fx-padding: 10px;");
        }

        HBox currentBidRow = new HBox();
        currentBidRow.setAlignment(Pos.CENTER_LEFT);
        Label lblCurrentBid = new Label("CURRENT BID");
        lblCurrentBid.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #604868;");
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Label priceLabel = new Label("₫ " + formatPrice(currentPrice));
        priceLabel.setId("priceLabel_" + id);
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2e1a28;");
        currentBidRow.getChildren().addAll(lblCurrentBid, spacer1, priceLabel);

        HBox userBidRow = new HBox();
        userBidRow.setAlignment(Pos.CENTER_LEFT);
        Label lblYourBid = new Label(outbidSession ? "YOUR MAX BID" : (endedSession ? "FINAL BID" : "YOUR BID"));
        lblYourBid.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #604868;");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        BigDecimal userMaxBid = getMoney(sessionObj, "userMaxBid", BigDecimal.ZERO);
        Label userPriceLabel = new Label();
        if (winningSession || (endedSession && highestBidderId == currentUserId)) {
            userPriceLabel.setText("₫ " + formatPrice(currentPrice));
            userPriceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10b981;");
        } else if (outbidSession) {
            userPriceLabel.setText("₫ " + formatPrice(userMaxBid));
            userPriceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #ef4444;");
        } else {
            userPriceLabel.setText("₫ " + formatPrice(userMaxBid));
            userPriceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #6c757d;");
        }
        userBidRow.getChildren().addAll(lblYourBid, spacer2, userPriceLabel);
        bidDetailsBox.getChildren().addAll(currentBidRow, userBidRow);

        Button btnAction = new Button();
        btnAction.setMaxWidth(Double.MAX_VALUE);
        btnAction.setPrefHeight(36.0);

        if (outbidSession) {
            btnAction.setText("Increase Bid");
            btnAction.setStyle(
                    "-fx-background-color: #e040a0; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.25), 6, 0, 0, 1);");
            Label arrowIcon = new Label("\uE5D8");
            arrowIcon.setStyle(
                    "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 15px; -fx-text-fill: white; -fx-padding: 0 4px 0 0;");
            btnAction.setGraphic(arrowIcon);

            btnAction.setOnMouseEntered(e -> {
                btnAction.setStyle(
                        "-fx-background-color: #d03090; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.35), 8, 0, 0, 2);");
            });
            btnAction.setOnMouseExited(e -> {
                btnAction.setStyle(
                        "-fx-background-color: #e040a0; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.25), 6, 0, 0, 1);");
            });
        } else {
            btnAction.setText("View Details");
            btnAction.setStyle(
                    "-fx-background-color: #f2e8f2; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px;");
            Label eyeIcon = new Label("\uE8f4");
            eyeIcon.setStyle(
                    "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 15px; -fx-text-fill: #604868; -fx-padding: 0 4px 0 0;");
            btnAction.setGraphic(eyeIcon);

            btnAction.setOnMouseEntered(e -> {
                btnAction.setStyle(
                        "-fx-background-color: #ffd6ee; -fx-text-fill: #e040a0; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px;");
            });
            btnAction.setOnMouseExited(e -> {
                btnAction.setStyle(
                        "-fx-background-color: #f2e8f2; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px;");
            });
        }

        btnAction.setOnAction(event -> {
            try {
                FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 1280, 800);
                AuctionPageController controller = loader.getController();
                controller.setItem(sessionObj, itemObj);
            } catch (IOException e) {
                logger.error("Error switching to auction room", e);
            }
        });

        vbox.getChildren().addAll(imageWrapper, nameLabel, categoryLabel, bidDetailsBox, btnAction);

        return vbox;
    }

    @FXML
    public void handleShowActive(ActionEvent event) {
        currentTab = Tab.ACTIVE;
        updateTabStyles();
        filterAndRenderProducts();
    }

    @FXML
    public void handleShowWinning(ActionEvent event) {
        currentTab = Tab.WINNING;
        updateTabStyles();
        filterAndRenderProducts();
    }

    @FXML
    public void handleShowOutbid(ActionEvent event) {
        currentTab = Tab.OUTBID;
        updateTabStyles();
        filterAndRenderProducts();
    }

    @FXML
    public void handleShowEnded(ActionEvent event) {
        currentTab = Tab.ENDED;
        updateTabStyles();
        filterAndRenderProducts();
    }

    private void updateTabStyles() {
        // Active
        if (btnTabActive != null) {
            if (currentTab == Tab.ACTIVE) {
                btnTabActive.setStyle(
                        "-fx-background-color: #e040a0; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-size: 14px;");
                setLabelStyleInButton(btnTabActive,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: #ffffff; -fx-padding: 0 4px 0 0;");
            } else {
                btnTabActive.setStyle(
                        "-fx-background-color: #f8eef8; -fx-text-fill: #2e1a28; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-size: 14px;");
                setLabelStyleInButton(btnTabActive,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: #e040a0; -fx-padding: 0 4px 0 0;");
            }
        }

        // Winning
        if (btnTabWinning != null) {
            if (currentTab == Tab.WINNING) {
                btnTabWinning.setStyle(
                        "-fx-background-color: #e040a0; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-size: 14px;");
                setLabelStyleInButton(btnTabWinning,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: #ffffff; -fx-padding: 0 4px 0 0;");
            } else {
                btnTabWinning.setStyle(
                        "-fx-background-color: #f8eef8; -fx-text-fill: #2e1a28; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-size: 14px;");
                setLabelStyleInButton(btnTabWinning,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: #10b981; -fx-padding: 0 4px 0 0;");
            }
        }

        // Outbid
        if (btnTabOutbid != null) {
            if (currentTab == Tab.OUTBID) {
                btnTabOutbid.setStyle(
                        "-fx-background-color: #e040a0; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-size: 14px;");
                setLabelStyleInButton(btnTabOutbid,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: #ffffff; -fx-padding: 0 4px 0 0;");
            } else {
                btnTabOutbid.setStyle(
                        "-fx-background-color: #f8eef8; -fx-text-fill: #2e1a28; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-size: 14px;");
                setLabelStyleInButton(btnTabOutbid,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: #ef4444; -fx-padding: 0 4px 0 0;");
            }
        }

        // Ended
        if (btnTabEnded != null) {
            if (currentTab == Tab.ENDED) {
                btnTabEnded.setStyle(
                        "-fx-background-color: #e040a0; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-size: 14px;");
                setLabelStyleInButton(btnTabEnded,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: #ffffff; -fx-padding: 0 4px 0 0;");
            } else {
                btnTabEnded.setStyle(
                        "-fx-background-color: #f8eef8; -fx-text-fill: #2e1a28; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-size: 14px;");
                setLabelStyleInButton(btnTabEnded,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: #7c52aa; -fx-padding: 0 4px 0 0;");
            }
        }
    }

    private void setLabelStyleInButton(Button btn, String style) {
        if (btn != null && btn.getGraphic() instanceof Label) {
            ((Label) btn.getGraphic()).setStyle(style);
        }
    }

    @FXML
    public void handleToggleSidebar(ActionEvent event) {
        if (sidebarController != null) {
            sidebarController.toggleSidebar();
            Platform.runLater(this::updateGridLayout);
        }
    }

    @FXML
    public void handleGoToDashboard(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
        } catch (IOException e) {
            logger.error("Error switching scene", e);
        }
    }

    public void handleLogout(ActionEvent event) throws IOException {
        User.clearSession();
        if (pollingScheduler != null) {
            pollingScheduler.shutdown();
        }
        SceneSwitcher.switchScene(event, "Login.fxml", 1100, 700);
    }

    private JSONObject getItemObject(JSONObject sessionObj) {
        JSONObject itemObj = sessionObj.optJSONObject("item");
        if (itemObj != null) {
            return itemObj;
        }
        JSONObject fallback = new JSONObject();
        fallback.put("name", sessionObj.optString("productName", ""));
        fallback.put("type", sessionObj.optString("productType", ""));
        fallback.put("description", sessionObj.optString("description", ""));
        fallback.put("imagePath", sessionObj.optString("imagePath", ""));
        return fallback;
    }

    private BigDecimal getMoney(JSONObject object, String key, BigDecimal defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue == null ? BigDecimal.ZERO : defaultValue;
        }
        try {
            return new BigDecimal(object.get(key).toString());
        } catch (Exception e) {
            return defaultValue == null ? BigDecimal.ZERO : defaultValue;
        }
    }

    private String buildImageUrl(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }
        String path = rawPath.trim().replace("\\", "/");
        if ((path.startsWith("http://") || path.startsWith("https://")) && !path.contains("/api/files/images/")) {
            return path;
        }
        int apiIndex = path.indexOf("/api/files/images/");
        if (apiIndex >= 0) {
            path = path.substring(apiIndex + "/api/files/images/".length());
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
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

    private String formatPrice(BigDecimal price) {
        if (price == null)
            return "0";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("###,###", symbols);
        return df.format(price);
    }

}