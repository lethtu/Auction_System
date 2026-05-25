package com.auction.client.controller;

import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import com.auction.client.util.CacheManager;
import com.auction.client.util.ShippingInfoDialog;
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
import com.auction.client.service.SettingsService;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyBidsController implements Initializable, SceneLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(MyBidsController.class);

    private static final int POLLING_INTERVAL_SECONDS = 8;
    private static final Duration SEARCH_DEBOUNCE_DELAY = Duration.millis(180);

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

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
    private final PauseTransition searchDebounce = new PauseTransition(SEARCH_DEBOUNCE_DELAY);
    private volatile String lastServerPayloadSignature = "";
    private volatile boolean fetchInProgress;

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
            searchDebounce.setOnFinished(event -> filterAndRenderProducts());
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> searchDebounce.playFromStart());
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
        final int maxColumns = 3;

        int columns = Math.max(1, Math.min(maxColumns, (int) Math.floor((viewportWidth + hgap) / (cardWidth + hgap))));
        double gridWidth = columns * cardWidth + Math.max(0, columns - 1) * hgap;

        productContainer.setAlignment(Pos.TOP_LEFT);
        productContainer.setPrefWrapLength(gridWidth);
        productContainer.setMinWidth(gridWidth);
        productContainer.setPrefWidth(gridWidth);
        productContainer.setMaxWidth(gridWidth);
        productContainer.setHgap(hgap);
        productContainer.setVgap(28.0);
        productContainer.setPadding(new Insets(10.0, 14.0, 18.0, 14.0));
    }

    private void startPolling() {
        if (pollingScheduler != null && !pollingScheduler.isShutdown()) {
            return;
        }
        pollingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        pollingScheduler.scheduleAtFixedRate(this::fetchProductsData, 0, POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void fetchProductsData() {
        if (fetchInProgress) {
            return;
        }
        fetchInProgress = true;

        try {
            Integer userId = User.getId();
            if (userId == null) {
                logger.debug("[MyBids] userId is null, skip fetch");
                return;
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/bidder/my-bids?bidderId=" + userId))
                    .GET();
            if (User.getSessionToken() != null) {
                builder.header("X-Auth-Token", User.getSessionToken());
            }

            HttpResponse<String> response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body();

            if (response.statusCode() != 200) {
                logger.debug("[MyBids] HTTP status={}", response.statusCode());
                return;
            }

            JSONObject responseJson = new JSONObject(body);
            int apiStatus = responseJson.optInt("status", response.statusCode());
            if (apiStatus != 200) {
                logger.debug("[MyBids] API status={}", apiStatus);
                return;
            }

            String payloadSignature = response.statusCode() + ":" + body.hashCode();
            if (payloadSignature.equals(lastServerPayloadSignature)) {
                return;
            }
            lastServerPayloadSignature = payloadSignature;

            Object dataObj = responseJson.get("data");
            JSONArray jsonArray = new JSONArray();
            if (dataObj instanceof JSONObject) {
                jsonArray = ((JSONObject) dataObj).optJSONArray("content");
                if (jsonArray == null) {
                    jsonArray = new JSONArray();
                }
            } else if (dataObj instanceof JSONArray) {
                jsonArray = (JSONArray) dataObj;
            }

            List<JSONObject> newProducts = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                newProducts.add(jsonArray.getJSONObject(i));
            }

            Platform.runLater(() -> {
                allProducts.clear();
                allProducts.addAll(newProducts);
                filterAndRenderProductsNow();
            });
        } catch (Exception e) {
            logger.warn("Error loading products: {}", e.getMessage());
        } finally {
            fetchInProgress = false;
        }
    }

    private void filterAndRenderProducts() {
        if (Platform.isFxApplicationThread()) {
            filterAndRenderProductsNow();
        } else {
            Platform.runLater(this::filterAndRenderProductsNow);
        }
    }

    private void filterAndRenderProductsNow() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        Integer currentUserId = User.getId();

        if (currentUserId == null) {
            productContainer.getChildren().clear();
            currentRenderedStates.clear();
            updateTabLabels();
            return;
        }

        updateTabLabels();

        List<String> newStatesToRender = new ArrayList<>();
        List<JSONObject> displayProducts = currentTab == Tab.ENDED
                ? prioritizeWonProducts(allProducts, currentUserId)
                : new ArrayList<>(allProducts);

        for (JSONObject sessionObj : displayProducts) {
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

        if (newStatesToRender.isEmpty()) {
            newStatesToRender.add("EMPTY_" + currentTab + "_" + keyword);
        }

        if (!currentRenderedStates.equals(newStatesToRender)) {
            productContainer.getChildren().clear();
            currentRenderedStates.clear();

            if (newStatesToRender.size() == 1 && newStatesToRender.get(0).startsWith("EMPTY_")) {
                productContainer.getChildren().add(createEmptyStateCard(keyword));
                currentRenderedStates.add(newStatesToRender.get(0));
                updateGridLayout();
                return;
            }

            for (JSONObject sessionObj : displayProducts) {
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
    }

    private VBox createEmptyStateCard(String keyword) {
        VBox card = new VBox(12.0);
        card.setAlignment(Pos.CENTER);
        card.setMinHeight(260.0);
        card.setPrefHeight(260.0);
        double width = productContainer != null && productContainer.getPrefWidth() > 320.0
                ? productContainer.getPrefWidth()
                : 780.0;
        card.setPrefWidth(width);
        card.setMaxWidth(width);

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String title;
        String subtitle;
        switch (currentTab) {
            case WINNING:
                title = hasKeyword ? "No winning bids found" : "You are not winning any auction yet";
                subtitle = hasKeyword ? "Try another keyword or switch to Active Bids." : "Place a stronger bid to see winning auctions here.";
                break;
            case OUTBID:
                title = hasKeyword ? "No outbid auctions found" : "No outbid alerts right now";
                subtitle = hasKeyword ? "Try another keyword or check Active Bids." : "Nice. You have not been outbid on matching active auctions.";
                break;
            case ENDED:
                title = hasKeyword ? "No ended bids found" : "No ended auctions yet";
                subtitle = hasKeyword ? "Try clearing search or switch tabs." : "Completed auction history will appear here.";
                break;
            case ACTIVE:
            default:
                title = hasKeyword ? "No active bids found" : "You have not joined any active auction yet";
                subtitle = hasKeyword ? "Try another keyword or clear the search box." : "Join a live auction and your bidding cards will appear here.";
                break;
        }

        Label icon = new Label("\uE8B6");
        icon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 42px; -fx-text-fill: -fx-accent;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + primaryTextHex() + ";");
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(Pos.CENTER);

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + mutedTextHex() + ";");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMaxWidth(520.0);

        if (isDarkThemeActive()) {
            card.setStyle("-fx-background-color: #241a2f; -fx-background-radius: 24px; "
                    + "-fx-border-color: rgba(255,255,255,0.14); -fx-border-width: 1.5px; -fx-border-radius: 24px; "
                    + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.28), 16, 0, 0, 5); -fx-padding: 28px;");
        } else {
            card.setStyle("-fx-background-color: -app-card; -fx-background-radius: 24px; "
                    + "-fx-border-color: -app-border; -fx-border-width: 1.5px; -fx-border-radius: 24px; "
                    + "-fx-effect: dropshadow(three-pass-box, rgba(224,64,160,0.10), 16, 0, 0, 5); -fx-padding: 28px;");
        }

        card.getChildren().addAll(icon, titleLabel, subtitleLabel);
        return card;
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

    private boolean isWonSession(JSONObject sessionObj, int currentUserId) {
        String status = normalizeSessionStatus(sessionObj);
        boolean completed = "ENDED".equals(status)
                || "CLOSED".equals(status)
                || "COMPLETED".equals(status)
                || "FINISHED".equals(status);
        return completed && sessionObj.optInt("highestBidderId", -1) == currentUserId;
    }

    private List<JSONObject> prioritizeWonProducts(List<JSONObject> products, int currentUserId) {
        List<JSONObject> orderedProducts = new ArrayList<>(products);
        orderedProducts.sort(Comparator.comparing(sessionObj -> !isWonSession(sessionObj, currentUserId)));
        return orderedProducts;
    }

    private String getRenderedStateKey(JSONObject sessionObj, BigDecimal currentPrice, int highestBidderId) {
        return sessionObj.optInt("id") + "_" + currentPrice + "_" + highestBidderId + "_"
                + normalizeSessionStatus(sessionObj) + "_delivery_"
                + ShippingInfoDialog.hasDeliveryInfo(sessionObj.optInt("id"), sessionObj);
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
        String imagePath = itemObj.optString("imagePath", itemObj.optString("imageUrl", ""));
        int highestBidderId = sessionObj.optInt("highestBidderId", -1);
        int currentUserId = User.getId() != null ? User.getId() : -1;
        boolean activeSession = isActiveSession(sessionObj);
        boolean endedSession = isEndedSession(sessionObj);
        boolean wonSession = isWonSession(sessionObj, currentUserId);
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
        vbox.setStyle(myBidCardStyle(false));

        // Interactive hover scaling and drop shadow micro-animation
        vbox.setOnMouseEntered(e -> {
            vbox.setStyle(myBidCardStyle(true));
        });
        vbox.setOnMouseExited(e -> {
            vbox.setStyle(myBidCardStyle(false));
        });

        // Image container with clipping and shadow
        StackPane imageWrapper = new StackPane();
        imageWrapper.setPrefHeight(150.0);
        imageWrapper.setMinHeight(150.0);
        imageWrapper.setMaxHeight(150.0);
        imageWrapper.setPrefWidth(222.0);
        imageWrapper.setMinWidth(222.0);
        imageWrapper.setMaxWidth(222.0);
        imageWrapper.setStyle(myBidImageWrapperStyle());

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
            Image cached = CacheManager.getCachedImage(imageUrl, updatedImage -> {
                if (updatedImage != null && !updatedImage.isError()) {
                    imageView.setImage(updatedImage);
                    imageWrapper.getChildren().setAll(imageView);
                }
            });
            if (cached != null && !cached.isError()) {
                imageView.setImage(cached);
                imageWrapper.getChildren().setAll(imageView);
            }
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
            badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -fx-accent;");
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
                    "-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: -app-border; -fx-border-radius: 12px;");

            Label timerIcon = new Label("\uE425");
            timerIcon.setStyle(
                    "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 14px; -fx-text-fill: -fx-accent;");

            Label timeLabel = new Label("Active");
            timeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -fx-accent;");
            timeBadge.getChildren().addAll(timerIcon, timeLabel);
            imageWrapper.getChildren().add(timeBadge);
        }

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: " + primaryTextHex() + ";");
        nameLabel.setWrapText(true);
        nameLabel.setPrefHeight(44.0);
        nameLabel.setMaxHeight(44.0);

        Label categoryLabel = new Label(type);
        categoryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + accentHex() + "; -fx-font-weight: bold;");

        VBox bidDetailsBox = new VBox(6.0);
        if (outbidSession) {
            bidDetailsBox.setStyle(
                    "-fx-background-color: rgba(239, 68, 68, 0.05); -fx-background-radius: 12px; -fx-padding: 10px; -fx-border-color: rgba(239, 68, 68, 0.1); -fx-border-width: 1px; -fx-border-radius: 12px;");
        } else {
            bidDetailsBox.setStyle(myBidDetailsStyle());
        }

        HBox currentBidRow = new HBox();
        currentBidRow.setAlignment(Pos.CENTER_LEFT);
        Label lblCurrentBid = new Label("CURRENT BID");
        lblCurrentBid.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + mutedTextHex() + ";");
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Label priceLabel = new Label("₫ " + formatPrice(currentPrice));
        priceLabel.setId("priceLabel_" + id);
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: " + primaryTextHex() + ";");
        currentBidRow.getChildren().addAll(lblCurrentBid, spacer1, priceLabel);

        HBox userBidRow = new HBox();
        userBidRow.setAlignment(Pos.CENTER_LEFT);
        Label lblYourBid = new Label(outbidSession ? "YOUR MAX BID" : (endedSession ? "FINAL BID" : "YOUR BID"));
        lblYourBid.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + mutedTextHex() + ";");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        BigDecimal userMaxBid = getMoney(sessionObj, "userMaxBid", BigDecimal.ZERO);
        Label userPriceLabel = new Label();
        if (winningSession || wonSession) {
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

        if (wonSession) {
            boolean hasDeliveryInfo = ShippingInfoDialog.hasDeliveryInfo(id, sessionObj);
            btnAction.setText(hasDeliveryInfo ? "Edit delivery" : "Delivery details");
            btnAction.setStyle(
                    "-fx-background-color: -fx-accent; -fx-text-fill: white; -fx-font-weight: bold; "
                            + "-fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px; "
                            + "-fx-effect: dropshadow(three-pass-box, -app-accent-opacity-25, 6, 0, 0, 1);");
            Label shippingIcon = new Label(hasDeliveryInfo ? "\uE3C9" : "\uE558");
            shippingIcon.setStyle(
                    "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 15px; -fx-text-fill: white; -fx-padding: 0 4px 0 0;");
            btnAction.setGraphic(shippingIcon);
            btnAction.setOnAction(event -> ShippingInfoDialog.show(id, name, sessionObj, this::filterAndRenderProducts));
        } else if (outbidSession) {
            btnAction.setText("Increase Bid");
            btnAction.setStyle(
                    "-fx-background-color: -fx-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.25), 6, 0, 0, 1);");
            Label arrowIcon = new Label("\uE5D8");
            arrowIcon.setStyle(
                    "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 15px; -fx-text-fill: white; -fx-padding: 0 4px 0 0;");
            btnAction.setGraphic(arrowIcon);

            btnAction.setOnMouseEntered(e -> {
                btnAction.setStyle(
                        "-fx-background-color: derive(-fx-accent, -10%); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.35), 8, 0, 0, 2);");
            });
            btnAction.setOnMouseExited(e -> {
                btnAction.setStyle(
                        "-fx-background-color: -fx-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.25), 6, 0, 0, 1);");
            });
        } else {
            btnAction.setText("View Details");
            btnAction.setStyle(
                    "-fx-background-color: -app-surface-2; -fx-text-fill: -app-text-muted; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px;");
            Label eyeIcon = new Label("\uE8f4");
            eyeIcon.setStyle(
                    "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 15px; -fx-text-fill: -app-text-muted; -fx-padding: 0 4px 0 0;");
            btnAction.setGraphic(eyeIcon);

            btnAction.setOnMouseEntered(e -> {
                btnAction.setStyle(
                        "-fx-background-color: -app-surface-2; -fx-text-fill: -fx-accent; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px;");
            });
            btnAction.setOnMouseExited(e -> {
                btnAction.setStyle(
                        "-fx-background-color: -app-surface-2; -fx-text-fill: -app-text-muted; -fx-font-weight: bold; -fx-background-radius: 18px; -fx-cursor: hand; -fx-font-size: 13px;");
            });
        }

        if (!wonSession) {
            btnAction.setOnAction(event -> {
                try {
                    FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 1280, 800);
                    AuctionPageController controller = loader.getController();
                    controller.setItem(sessionObj, itemObj);
                } catch (IOException e) {
                    logger.error("Error switching to auction room", e);
                }
            });
        }

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

    private boolean isDarkThemeActive() {
        return SettingsService.getInstance().getTheme().toLowerCase(java.util.Locale.ROOT).contains("dark");
    }

    private String accentHex() {
        String color = SettingsService.getInstance().getPrimaryColor();
        if (color == null) return "#e040a0";
        String normalized = color.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("purple")) return "#8b5cf6";
        if (normalized.contains("emerald") || normalized.contains("green")) return "#10b981";
        if (normalized.contains("blue")) return "#3b82f6";
        if (normalized.contains("orange")) return "#f97316";
        return "#e040a0";
    }

    private String primaryTextHex() {
        return isDarkThemeActive() ? "#f0e6f8" : "#2e1a28";
    }

    private String mutedTextHex() {
        return isDarkThemeActive() ? "#b8a8c8" : "#604868";
    }

    private String myBidCardStyle(boolean hover) {
        String scale = hover ? " -fx-scale-x: 1.02; -fx-scale-y: 1.02; -fx-cursor: hand;" : " -fx-scale-x: 1.0; -fx-scale-y: 1.0;";
        if (isDarkThemeActive()) {
            return "-fx-border-color: rgba(255,255,255,0.14); -fx-border-width: 1.5px; -fx-border-radius: 20px; "
                    + "-fx-background-radius: 20px; -fx-padding: 14px; -fx-background-color: #241a2f; "
                    + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.30), 14, 0, 0, 4);" + scale;
        }
        return "-fx-border-color: " + (hover ? "-app-surface-2" : "-app-border") + "; -fx-border-width: 2px; -fx-border-radius: 20px; "
                + "-fx-background-radius: 20px; -fx-padding: 14px; -fx-background-color: white; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, " + (hover ? "0.15" : "0.05") + "), 15, 0, 0, 4);" + scale;
    }

    private String myBidImageWrapperStyle() {
        if (isDarkThemeActive()) {
            return "-fx-background-radius: 14px; -fx-border-radius: 14px; -fx-background-color: #1a1223; "
                    + "-fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1px;";
        }
        return "-fx-background-radius: 14px; -fx-border-radius: 14px; -fx-background-color: #fcf6fc;";
    }

    private String myBidDetailsStyle() {
        if (isDarkThemeActive()) {
            return "-fx-background-color: #2a2035; -fx-background-radius: 12px; -fx-padding: 10px; "
                    + "-fx-border-color: rgba(255,255,255,0.10); -fx-border-width: 1px; -fx-border-radius: 12px;";
        }
        return "-fx-background-color: -app-surface-2; -fx-background-radius: 12px; -fx-padding: 10px;";
    }

    private String activeTabStyle() {
        return "-fx-background-color: " + accentHex() + "; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-background-radius: 20px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-size: 14px;";
    }

    private String inactiveTabStyle() {
        if (isDarkThemeActive()) {
            return "-fx-background-color: #2a2035; -fx-text-fill: #f0e6f8; -fx-font-weight: bold; "
                    + "-fx-background-radius: 20px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-size: 14px; "
                    + "-fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 20px;";
        }
        return "-fx-background-color: -app-surface-2; -fx-text-fill: -app-text; -fx-font-weight: bold; "
                + "-fx-background-radius: 20px; -fx-padding: 10px 20px; -fx-cursor: hand; -fx-font-size: 14px;";
    }

    private void updateTabLabels() {
        if (btnTabActive == null || btnTabWinning == null || btnTabOutbid == null || btnTabEnded == null) {
            return;
        }

        Integer currentUserId = User.getId();
        int activeCount = 0;
        int winningCount = 0;
        int outbidCount = 0;
        int endedCount = 0;

        if (currentUserId != null) {
            for (JSONObject sessionObj : allProducts) {
                if (isActiveSession(sessionObj)) {
                    activeCount++;
                }
                if (isWinningSession(sessionObj, currentUserId)) {
                    winningCount++;
                }
                if (isOutbidSession(sessionObj, currentUserId)) {
                    outbidCount++;
                }
                if (isEndedSession(sessionObj)) {
                    endedCount++;
                }
            }
        }

        btnTabActive.setText("Active Bids (" + activeCount + ")");
        btnTabWinning.setText("Winning (" + winningCount + ")");
        btnTabOutbid.setText("Outbid (" + outbidCount + ")");
        btnTabEnded.setText("Ended (" + endedCount + ")");
    }

    private void updateTabStyles() {
        updateTabLabels();
        // Active
        if (btnTabActive != null) {
            if (currentTab == Tab.ACTIVE) {
                btnTabActive.setStyle(activeTabStyle());
                setLabelStyleInButton(btnTabActive,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: white; -fx-padding: 0 4px 0 0;");
            } else {
                btnTabActive.setStyle(inactiveTabStyle());
                setLabelStyleInButton(btnTabActive,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: -fx-accent; -fx-padding: 0 4px 0 0;");
            }
        }

        // Winning
        if (btnTabWinning != null) {
            if (currentTab == Tab.WINNING) {
                btnTabWinning.setStyle(activeTabStyle());
                setLabelStyleInButton(btnTabWinning,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: white; -fx-padding: 0 4px 0 0;");
            } else {
                btnTabWinning.setStyle(inactiveTabStyle());
                setLabelStyleInButton(btnTabWinning,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: #10b981; -fx-padding: 0 4px 0 0;");
            }
        }

        // Outbid
        if (btnTabOutbid != null) {
            if (currentTab == Tab.OUTBID) {
                btnTabOutbid.setStyle(activeTabStyle());
                setLabelStyleInButton(btnTabOutbid,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: white; -fx-padding: 0 4px 0 0;");
            } else {
                btnTabOutbid.setStyle(inactiveTabStyle());
                setLabelStyleInButton(btnTabOutbid,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: #ef4444; -fx-padding: 0 4px 0 0;");
            }
        }

        // Ended
        if (btnTabEnded != null) {
            if (currentTab == Tab.ENDED) {
                btnTabEnded.setStyle(activeTabStyle());
                setLabelStyleInButton(btnTabEnded,
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: white; -fx-padding: 0 4px 0 0;");
            } else {
                btnTabEnded.setStyle(inactiveTabStyle());
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
        return path.isBlank() ? "" : Config.applyCacheBuster(Config.API_URL + "/api/files/images/" + path);
    }

    private String formatPrice(BigDecimal price) {
        if (price == null)
            return "0";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("###,###", symbols);
        return df.format(price);
    }

    @Override
    public void onSceneHidden() {
        stopMyBidsBackgroundWork();
    }

    private void stopMyBidsBackgroundWork() {
        searchDebounce.stop();
        fetchInProgress = false;
        if (pollingScheduler != null) {
            pollingScheduler.shutdownNow();
            pollingScheduler = null;
        }
    }
}