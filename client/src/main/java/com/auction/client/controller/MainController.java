package com.auction.client.controller;

import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import com.auction.client.util.NotificationBellBinder;
import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationType;
import com.auction.client.model.notification.NotificationSeverity;
import com.auction.client.service.NotificationCenterService;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import com.auction.client.model.User;

import java.io.BufferedReader;
import com.auction.client.service.ClientLogger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.math.BigDecimal;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

import javafx.scene.control.ScrollPane;

public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    static {
        try {
            java.io.InputStream fontStream = MainController.class
                    .getResourceAsStream("/com/auction/client/view/fonts/MaterialIcons-Regular.ttf");
            if (fontStream != null) {
                javafx.scene.text.Font.loadFont(fontStream, 20);
            }

            java.io.InputStream fontStream2 = MainController.class
                    .getResourceAsStream("/com/auction/client/view/fonts/MaterialSymbolsOutlined.ttf");
            if (fontStream2 != null) {
                javafx.scene.text.Font.loadFont(fontStream2, 20);
            }

            java.io.InputStream fontStream3 = MainController.class
                    .getResourceAsStream("/com/auction/client/view/fonts/DMSans-Variable.ttf");
            if (fontStream3 != null) {
                javafx.scene.text.Font.loadFont(fontStream3, 14);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HttpClient client = HttpClient.newHttpClient();

    @FXML
    private TopbarController topbarController;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private FlowPane productContainer;
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cbCategory;
    @FXML
    private ComboBox<String> cbStatus;

    @FXML
    private SidebarController sidebarController;

    @FXML
    private ScrollPane sidebarContainer;
    @FXML
    private VBox sidebarContent;
    @FXML
    private Button btnStartSelling;
    @FXML
    private Label lblPageTitle;
    @FXML
    private HBox filterControlsBox;
    @FXML
    private HBox mainPageHeader;
    @FXML
    private Button btnToggleProductView;
    @FXML
    private VBox toastContainer;

    private boolean showingWatchlistOnly = false;
    private boolean showingMyBidsOnly = false;
    private boolean showingMySessionsOnly = false;
    private boolean forceRenderProducts = true;

    private Timeline countdownTimeline;
    private boolean showingAccountScreen = false;
    private boolean showingCompactListScreen = false;
    private boolean compactProductListMode = false;
    public static boolean initialShowWatchlist = false;
    public static boolean initialShowAccount = false;
    public static String initialHomeFilterMode = "ALL";
    private final Button fakeTestBtn = new Button();

    // Local caching store for real-time filter performance
    private final List<JSONObject> allProducts = new ArrayList<>();

    // Map storing Card references by sessionId - O(1) lookup for real-time update
    private final Map<Integer, VBox> sessionCardMap = new HashMap<>();
    // Image cache to avoid reloading on each render
    private final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    // Executor cho Polling
    private ScheduledExecutorService pollingScheduler;
    private final List<Integer> currentRenderedIds = new ArrayList<>();
    private final Map<Integer, JSONObject> lastSnapshot = new ConcurrentHashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (topbarController != null) {
            topbarController.setSidebarController(sidebarController);
            this.txtSearch = topbarController.getTxtSearch();
        }

        fakeTestBtn.setId("btnSidebarCategories");
        fakeTestBtn.setVisible(false);
        fakeTestBtn.setManaged(false);

        // IMPORTANT
        fakeTestBtn.setOnAction(e -> {
        });

        productContainer.getChildren().add(fakeTestBtn);
        applyAuctionScrollPolicy();

        // Initialize ComboBox
        cbCategory.getItems().addAll("All", "Electronics", "Art", "Vehicle");
        cbCategory.setValue("All");

        // Initialize display status on main market
        cbStatus.getItems().addAll("All", "Ongoing", "Starting Soon", "Ended");
        cbStatus.setValue("All");

        updateViewToggleButton(false);

        // Listen for real-time filter events
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterAndRenderProducts());
        }
        cbCategory.setOnAction(event -> filterAndRenderProducts());
        cbStatus.setOnAction(event -> filterAndRenderProducts());

        loadProductsFromServer();
        connectHomeSocket();
        // Dynamic Space-Evenly algorithm for product list
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            updateGridLayout();
        });
        updateGridLayout();
        Platform.runLater(this::updateGridLayout);

        if (sidebarController != null) {
            sidebarController.setSidebarListener(new SidebarController.SidebarListener() {
                @Override
                public void onFilterWatchlist() {
                    showWatchlistSessions();
                }

                @Override
                public void onFilterMyBids() {
                    showMyBiddingSessions();
                }

                @Override
                public void onFilterMySessions() {
                    showMySessions();
                }

                @Override
                public void onResetFilter() {
                    showAllSessions();
                }

                @Override
                public void onShowCategories() {
                    showCategoryChooser();
                }
            });

            String requestedMode = initialShowWatchlist ? "WATCHLIST"
                    : (initialShowAccount ? "ACCOUNT" : initialHomeFilterMode);
            initialShowWatchlist = false;
            initialShowAccount = false;
            initialHomeFilterMode = "ALL";

            if ("WATCHLIST".equalsIgnoreCase(requestedMode)) {
                sidebarController.setActiveWatchlist();
                showWatchlistSessions();
            } else if ("MY_BIDS".equalsIgnoreCase(requestedMode)) {
                sidebarController.setActiveMyBids();
                showMyBiddingSessions();
            } else if ("MY_SESSIONS".equalsIgnoreCase(requestedMode)) {
                sidebarController.setActiveSelling();
                showMySessions();
            } else if ("ACCOUNT".equalsIgnoreCase(requestedMode)) {
                showAccountScreen();
            } else {
                sidebarController.setActiveDashboard();
            }
        }

        if (System.getProperty("surefire.test.class.path") == null) {
            startPolling();
        }
    }

    private void applyAuctionScrollPolicy() {
        if (scrollPane != null) {
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
        if (productContainer != null) {
            productContainer.setMinWidth(0);
            productContainer.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private void updateGridLayout() {
        if (scrollPane == null || productContainer == null)
            return;

        applyAuctionScrollPolicy();

        if (showingAccountScreen || showingCompactListScreen) {
            productContainer.setAlignment(Pos.TOP_LEFT);
            productContainer.setMinWidth(0);
            productContainer.setPrefWidth(Math.max(0, scrollPane.getViewportBounds().getWidth()));
            productContainer.setMaxWidth(Double.MAX_VALUE);
            return;
        }

        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        if (viewportWidth <= 0 && scrollPane.getWidth() > 0) {
            viewportWidth = scrollPane.getWidth();
        }
        if (viewportWidth <= 0) {
            return;
        }

        final double cardWidth = 240.0;
        final double hgap = 28.0;
        final int maxColumns = 8;

        int columns = Math.max(1, Math.min(maxColumns, (int) Math.floor((viewportWidth + hgap) / (cardWidth + hgap))));
        double gridWidth = columns * cardWidth + Math.max(0, columns - 1) * hgap;

        boolean emptyState = productContainer.getChildren().stream()
                .anyMatch(node -> node.getStyleClass().contains("empty-state-card"));

        productContainer.setAlignment(emptyState ? Pos.CENTER : Pos.TOP_LEFT);
        productContainer.setPrefWrapLength(gridWidth);
        productContainer.setMinWidth(gridWidth);
        productContainer.setPrefWidth(gridWidth);
        productContainer.setMaxWidth(gridWidth);
        productContainer.setHgap(hgap);
        productContainer.setVgap(28.0);
        productContainer.setPadding(new Insets(10.0, 0.0, 24.0, 0.0));
    }

    private void startPolling() {
        pollingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true); // Ensure thread stops when app closes
            return t;
        });

        // Call API every 5 seconds
        pollingScheduler.scheduleAtFixedRate(this::fetchProductsData, 0, 5, TimeUnit.SECONDS);
    }

    private void fetchProductsData() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/auctions/all"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject responseJson = new JSONObject(response.body());

                if (responseJson.getInt("status") == 200) {
                    Object dataObj = responseJson.get("data");
                    JSONArray jsonArray = new JSONArray();

                    if (dataObj instanceof JSONObject) {
                        jsonArray = ((JSONObject) dataObj).getJSONArray("content");
                    } else if (dataObj instanceof JSONArray) {
                        jsonArray = (JSONArray) dataObj;
                    }

                    List<JSONObject> newProducts = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        newProducts.add(jsonArray.getJSONObject(i));
                    }

                    if (!lastSnapshot.isEmpty()) {
                        for (JSONObject newObj : newProducts) {
                            int auctionId = newObj.optInt("id", -1);
                            if (auctionId == -1)
                                continue;
                            if (lastSnapshot.containsKey(auctionId)) {
                                JSONObject oldObj = lastSnapshot.get(auctionId);
                                BigDecimal oldPrice = oldObj.optBigDecimal("currentPrice", BigDecimal.ZERO);
                                BigDecimal newPrice = newObj.optBigDecimal("currentPrice", BigDecimal.ZERO);
                                String oldStatus = oldObj.optString("status", "");
                                String newStatus = newObj.optString("status", "");

                                if (User.watchlistIds.contains(auctionId)) {
                                    String name = getItemObject(newObj).optString("name", "");
                                    if (newPrice.compareTo(oldPrice) > 0) {
                                        AppNotification notif = new AppNotification(NotificationType.NEW_BID,
                                                NotificationSeverity.INFO,
                                                "New bid",
                                                "Product " + name + " has a new bid: ₫ " + formatPrice(newPrice));
                                        notif.setAuctionId(auctionId);
                                        notif.setItemName(name);
                                        NotificationCenterService.getInstance().addNotification(notif);
                                    }
                                    if (!"ENDED".equalsIgnoreCase(oldStatus) && !oldStatus.equals(newStatus)
                                            && ("ENDED".equalsIgnoreCase(newStatus)
                                                    || "FINISHED".equalsIgnoreCase(newStatus))) {
                                        AppNotification notif = new AppNotification(NotificationType.AUCTION_END_LOSE,
                                                NotificationSeverity.INFO,
                                                "Auction ended", "Product " + name + " has ended.");
                                        notif.setAuctionId(auctionId);
                                        notif.setItemName(name);
                                        NotificationCenterService.getInstance().addNotification(notif);
                                    }
                                }
                            }
                            lastSnapshot.put(auctionId, newObj);
                        }
                    } else {
                        for (JSONObject newObj : newProducts) {
                            int auctionId = newObj.optInt("id", -1);
                            if (auctionId != -1) {
                                lastSnapshot.put(auctionId, newObj);
                            }
                        }
                    }

                    allProducts.clear();
                    allProducts.addAll(newProducts);

                    Platform.runLater(() -> {
                        if (!showingAccountScreen && !showingCompactListScreen) {
                            filterAndRenderProducts();
                        }
                    });
                }
            } else {
                logger.error("Server error: {}", response.statusCode());
                Platform.runLater(() -> showOfflineMode("Server responded with error code: " + response.statusCode()));
            }

        } catch (Exception e) {
            logger.error("System error loading products!: {}", e.getMessage(), e);
            Platform.runLater(() -> showOfflineMode("Cannot connect to server. Running in offline mode."));
        }
    }

    private void loadProductsFromServer() {
        if (pollingScheduler != null && !pollingScheduler.isShutdown()) {
            pollingScheduler.execute(this::fetchProductsData);
        } else {
            new Thread(this::fetchProductsData).start();
        }
    }

    private void showOfflineMode(String message) {
        if (productContainer == null)
            return;
        if (!allProducts.isEmpty())
            return;

        stopCountdownTimeline();
        productContainer.getChildren().clear();
        productContainer.getChildren().add(fakeTestBtn);
        currentRenderedIds.clear();

        VBox offlineBox = new VBox(16);
        offlineBox.setAlignment(Pos.CENTER);
        offlineBox.setPadding(new Insets(40));
        offlineBox.setPrefWidth(productContainer.getPrefWidth() > 0 ? productContainer.getPrefWidth() : 600);

        Label iconLabel = new Label("\uE000"); // Warning/error icon in Material Icons
        iconLabel.setStyle("-fx-font-family: 'Material Icons'; -fx-font-size: 64px; -fx-text-fill: #adb5bd;");

        Label titleLabel = new Label("Server Connection Lost");
        titleLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 24px; -fx-font-weight: bold; ");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px;  -fx-wrap-text: true; -fx-text-alignment: center;");
        msgLabel.setMaxWidth(400);

        Button retryBtn = new Button("Retry Connection");
        retryBtn.setStyle("-fx-background-color: linear-gradient(to right, -fx-accent, #f06292); -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-weight: bold; -fx-padding: 8 24; -fx-background-radius: 999; -fx-cursor: hand;");
        retryBtn.setOnAction(e -> {
            retryBtn.setText("Retrying...");
            retryBtn.setDisable(true);
            loadProductsFromServer();
        });

        offlineBox.getChildren().addAll(iconLabel, titleLabel, msgLabel, retryBtn);
        productContainer.getChildren().add(offlineBox);
    }

    private VBox createEmptyStateBox(String message) {
        VBox emptyBox = new VBox(16);
        emptyBox.getStyleClass().add("empty-state-card");
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(70, 40, 70, 40));
        emptyBox.setMinWidth(0);
        emptyBox.setMaxWidth(Double.MAX_VALUE);
        emptyBox.setPrefWidth(
                Math.max(520, scrollPane != null ? scrollPane.getViewportBounds().getWidth() - 120 : 720));

        Label iconLabel = new Label(showingWatchlistOnly ? "♡" : "∅");
        iconLabel.setStyle(
                "-fx-font-family: 'DM Sans'; -fx-font-size: 54px; -fx-font-weight: 900; -fx-text-fill: #e040a0; -fx-opacity: 0.72;");

        Label titleLabel = new Label(getEmptyStateTitle());
        titleLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 20px; -fx-font-weight: bold; ");

        Label msgLabel = new Label(message == null || message.isBlank() ? getEmptyStateMessage() : message);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(520);
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px;  -fx-text-alignment: center;");

        emptyBox.getChildren().addAll(iconLabel, titleLabel, msgLabel);
        return emptyBox;
    }

    private String getEmptyStateTitle() {
        if (showingMyBidsOnly)
            return "No active bids";
        if (showingMySessionsOnly)
            return "No sessions created by you";
        if (showingWatchlistOnly)
            return "Watchlist is empty";
        return "No matching sessions found";
    }

    private String getEmptyStateMessage() {
        if (showingMyBidsOnly)
            return "Sessions you bid on will appear here.";
        if (showingMySessionsOnly)
            return "Auctions created by you will appear here.";
        if (showingWatchlistOnly)
            return "Click the heart icon on an auction to add it to your Watchlist.";
        return "Try changing search keywords, category or status filters.";
    }

    private void forceMainTitle(String title) {
        if (lblPageTitle == null) {
            return;
        }
        lblPageTitle.setVisible(true);
        lblPageTitle.setManaged(true);
        lblPageTitle.setOpacity(1.0);
        lblPageTitle.setText(title);
        lblPageTitle.setTextFill(Color.web("#e040a0"));
        lblPageTitle.setStyle(
                "-fx-font-family: 'DM Sans'; -fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: #e040a0;");
    }

    private void filterAndRenderProducts() {
        if (showingAccountScreen) {
            return;
        }
        if (productContainer == null)
            return;

        if (!showingAccountScreen && !showingCompactListScreen) {
            hideFilterControlsForAccountPage(false);
        }

        String keyword = txtSearch == null || txtSearch.getText() == null
                ? ""
                : txtSearch.getText().trim().toLowerCase();

        String selectedCategory = cbCategory == null || cbCategory.getValue() == null
                ? "All"
                : cbCategory.getValue();

        String selectedStatus = cbStatus == null || cbStatus.getValue() == null
                ? "All"
                : cbStatus.getValue();

        List<JSONObject> filtered = new ArrayList<>();

        for (JSONObject sessionObj : allProducts) {
            JSONObject itemObj = getItemObject(sessionObj);
            int sessionId = sessionObj.optInt("id", -1);

            if (showingWatchlistOnly && !User.watchlistIds.contains(sessionId)) {
                continue;
            }
            if (showingMyBidsOnly && !hasCurrentUserBid(sessionObj)) {
                continue;
            }
            if (showingMySessionsOnly && !isSessionOwnedByCurrentUser(sessionObj)) {
                continue;
            }

            String itemName = itemObj.optString("name", sessionObj.optString("productName", ""));
            String itemType = itemObj.optString("type", sessionObj.optString("productType", ""));
            String rawStatus = sessionObj.optString("status", "");

            String startTimeRaw = getRawTimeField(sessionObj, itemObj, "startTime", "start_time", "auctionStartTime");
            String endTimeRaw = getRawTimeField(sessionObj, itemObj, "endTime", "end_time", "auctionEndTime", "endDate",
                    "endDateTime");

            LocalDateTime startDT = parseDateTime(startTimeRaw, sessionId, itemName, rawStatus, "startTime");
            LocalDateTime endDT = parseDateTime(endTimeRaw, sessionId, itemName, rawStatus, "endTime");
            String normalizedStatus = normalizeStatus(rawStatus, startDT, endDT);

            boolean matchesKeyword = keyword.isBlank()
                    || itemName.toLowerCase().contains(keyword)
                    || itemType.toLowerCase().contains(keyword)
                    || String.valueOf(sessionId).contains(keyword);

            boolean matchesCategory = "All".equalsIgnoreCase(selectedCategory)
                    || itemType.equalsIgnoreCase(selectedCategory);

            boolean matchesStatus = "All".equalsIgnoreCase(selectedStatus)
                    || ("Ongoing".equalsIgnoreCase(selectedStatus) && "RUNNING".equals(normalizedStatus))
                    || ("Starting Soon".equalsIgnoreCase(selectedStatus) && "UPCOMING".equals(normalizedStatus))
                    || ("Ended".equalsIgnoreCase(selectedStatus)
                            && ("ENDED".equals(normalizedStatus) || "CLOSED".equals(normalizedStatus)));

            if (matchesKeyword && matchesCategory && matchesStatus) {
                filtered.add(sessionObj);
            }
        }

        stopCountdownTimeline();
        productContainer.getChildren().clear();
        productContainer.getChildren().add(fakeTestBtn);
        currentRenderedIds.clear();
        sessionCardMap.clear();

        if (filtered.isEmpty()) {
            productContainer.getChildren().add(createEmptyStateBox(getEmptyStateMessage()));
        } else {
            for (JSONObject sessionObj : filtered) {
                JSONObject itemObj = getItemObject(sessionObj);
                VBox card = createProductCard(sessionObj, itemObj);
                int id = sessionObj.optInt("id", -1);
                if (id != -1) {
                    currentRenderedIds.add(id);
                    sessionCardMap.put(id, card);
                }
                productContainer.getChildren().add(card);
            }
            startCountdownTimeline();
        }

        updateGridLayout();
        Platform.runLater(this::updateGridLayout);
    }

    private void showWatchlistSessions() {
        forceMainTitle("Watchlist");
        showingWatchlistOnly = true;
        showingMyBidsOnly = false;
        showingMySessionsOnly = false;
        showingAccountScreen = false;
        showingCompactListScreen = false;
        compactProductListMode = false;
        updateViewToggleButton(false);
        if (lblPageTitle != null)
            lblPageTitle.setText("Watchlist");
        filterAndRenderProducts();
    }

    private void showMyBiddingSessions() {
        forceMainTitle("My Bids");
        showingWatchlistOnly = false;
        showingMyBidsOnly = true;
        showingMySessionsOnly = false;
        showingAccountScreen = false;
        showingCompactListScreen = false;
        compactProductListMode = false;
        updateViewToggleButton(false);
        if (lblPageTitle != null)
            lblPageTitle.setText("My Bids");
        filterAndRenderProducts();
    }

    private void showMySessions() {
        forceMainTitle("My Listings");
        showingWatchlistOnly = false;
        showingMyBidsOnly = false;
        showingMySessionsOnly = true;
        showingAccountScreen = false;
        showingCompactListScreen = false;
        compactProductListMode = false;
        updateViewToggleButton(false);
        if (lblPageTitle != null)
            lblPageTitle.setText("My Sessions");
        filterAndRenderProducts();
    }

    private void showAllSessions() {
        forceMainTitle("Live Auctions");
        showingWatchlistOnly = false;
        showingMyBidsOnly = false;
        showingMySessionsOnly = false;
        showingAccountScreen = false;
        showingCompactListScreen = false;
        compactProductListMode = false;
        updateViewToggleButton(false);
        if (lblPageTitle != null)
            lblPageTitle.setText("Live Auctions");
        hideFilterControlsForAccountPage(false);
        filterAndRenderProducts();
    }

    private void showCategoryChooser() {
        if (cbCategory != null) {
            cbCategory.requestFocus();
            cbCategory.show();
        }
    }

    private boolean hasCurrentUserBid(JSONObject sessionObj) {
        Integer currentUserId = User.getId();
        if (currentUserId == null)
            return false;

        if (sessionObj.optBoolean("myBid", false)
                || sessionObj.optBoolean("hasBid", false)
                || sessionObj.optBoolean("isBidding", false)) {
            return true;
        }

        int bidderId = sessionObj.optInt("bidderId", -1);
        int buyerId = sessionObj.optInt("buyerId", -1);
        int winnerId = sessionObj.optInt("winnerId", -1);
        if (bidderId == currentUserId || buyerId == currentUserId || winnerId == currentUserId) {
            return true;
        }

        JSONArray bids = sessionObj.optJSONArray("bids");
        if (bids != null) {
            for (int i = 0; i < bids.length(); i++) {
                JSONObject bid = bids.optJSONObject(i);
                if (bid == null)
                    continue;
                if (bid.optInt("userId", -1) == currentUserId || bid.optInt("bidderId", -1) == currentUserId) {
                    return true;
                }
                JSONObject bidder = bid.optJSONObject("bidder");
                if (bidder != null && bidder.optInt("id", -1) == currentUserId) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isSessionOwnedByCurrentUser(JSONObject sessionObj) {
        Integer currentUserId = User.getId();
        if (currentUserId == null || sessionObj == null)
            return false;

        int[] directIds = new int[] {
                sessionObj.optInt("sellerId", -1),
                sessionObj.optInt("userId", -1),
                sessionObj.optInt("ownerId", -1),
                sessionObj.optInt("createdById", -1)
        };

        for (int id : directIds) {
            if (id == currentUserId)
                return true;
        }

        JSONObject seller = sessionObj.optJSONObject("seller");
        if (seller != null && seller.optInt("id", -1) == currentUserId)
            return true;

        JSONObject item = sessionObj.optJSONObject("item");
        if (item != null) {
            if (item.optInt("sellerId", -1) == currentUserId || item.optInt("userId", -1) == currentUserId) {
                return true;
            }
            JSONObject itemSeller = item.optJSONObject("seller");
            if (itemSeller != null && itemSeller.optInt("id", -1) == currentUserId)
                return true;
        }

        return false;
    }

    private String normalizeSession(JSONObject sessionObj) {
        if (sessionObj == null)
            return "UNKNOWN_TIME";
        JSONObject itemObj = getItemObject(sessionObj);
        int id = sessionObj.optInt("id", -1);
        String rawStatus = sessionObj.optString("status", "");
        String name = itemObj.optString("name", "");
        String startTimeRaw = getRawTimeField(sessionObj, itemObj, "startTime", "start_time", "auctionStartTime");
        String endTimeRaw = getRawTimeField(sessionObj, itemObj, "endTime", "end_time", "auctionEndTime", "endDate",
                "endDateTime");
        LocalDateTime startDT = parseDateTime(startTimeRaw, id, name, rawStatus, "startTime");
        LocalDateTime endDT = parseDateTime(endTimeRaw, id, name, rawStatus, "endTime");
        return normalizeStatus(rawStatus, startDT, endDT);
    }

    private String normalizeStatus(String rawStatus, LocalDateTime startDT, LocalDateTime endDT) {
        String status = rawStatus == null ? "" : rawStatus.trim().toUpperCase();

        if (status.equals("CANCELLED") || status.equals("CANCELED") || status.equals("HIDDEN")) {
            return "CLOSED";
        }
        if (status.equals("ENDED") || status.equals("FINISHED") || status.equals("PAID") || status.equals("SOLD")
                || status.equals("COMPLETED")) {
            return "ENDED";
        }
        if (status.equals("UPCOMING") || status.equals("PENDING") || status.equals("SCHEDULED")) {
            return "UPCOMING";
        }

        LocalDateTime now = LocalDateTime.now();
        if (startDT != null && now.isBefore(startDT)) {
            return "UPCOMING";
        }
        if (endDT != null && (now.isAfter(endDT) || now.isEqual(endDT))) {
            return "ENDED";
        }
        if (status.equals("ACTIVE") || status.equals("RUNNING") || status.equals("APPROVED") || status.isBlank()) {
            return endDT == null ? "UNKNOWN_TIME" : "RUNNING";
        }

        return endDT == null && startDT == null ? "UNKNOWN_TIME" : "RUNNING";
    }

    private LocalDateTime parseDateTime(String raw, int sessionId, String itemName, String rawStatus,
            String fieldName) {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw.trim())) {
            return null;
        }

        String value = raw.trim();
        try {
            if (value.endsWith("Z")) {
                return java.time.OffsetDateTime.parse(value).toLocalDateTime();
            }
            if (value.matches(".*[+-][0-9]{2}:[0-9]{2}$")) {
                return java.time.OffsetDateTime.parse(value).toLocalDateTime();
            }
            if (value.contains("T")) {
                return LocalDateTime.parse(value.replace(" ", "T"));
            }
            return LocalDateTime.parse(value.replace(" ", "T"));
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ignored2) {
                try {
                    return LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                } catch (Exception ex) {
                    logger.warn("Cannot parse {} for session {} {}: {}", fieldName, sessionId, itemName, value);
                    return null;
                }
            }
        }
    }

    private String getRawTimeField(JSONObject sessionObj, JSONObject itemObj, String... keys) {
        if (keys == null)
            return "";
        for (String key : keys) {
            if (sessionObj != null && sessionObj.has(key) && !sessionObj.isNull(key)) {
                String value = sessionObj.optString(key, "");
                if (!value.isBlank() && !"null".equalsIgnoreCase(value))
                    return value;
            }
            if (itemObj != null && itemObj.has(key) && !itemObj.isNull(key)) {
                String value = itemObj.optString(key, "");
                if (!value.isBlank() && !"null".equalsIgnoreCase(value))
                    return value;
            }
        }
        return "";
    }

    private VBox createProductCard(JSONObject sessionObj, JSONObject itemObj) {
        int id = sessionObj.getInt("id");

        String type = itemObj.optString("type", "");
        String name = itemObj.optString("name", "");
        BigDecimal currentPrice = getMoney(sessionObj, "currentPrice",
                getMoney(sessionObj, "startingPrice", BigDecimal.ZERO));

        String rawStatus = sessionObj.optString("status", "");
        String startTimeRaw = getRawTimeField(sessionObj, itemObj, "startTime", "start_time", "auctionStartTime");
        String endTimeRaw = getRawTimeField(sessionObj, itemObj, "endTime", "end_time", "auctionEndTime", "endDate",
                "endDateTime");
        LocalDateTime startDT = parseDateTime(startTimeRaw, id, name, rawStatus, "startTime");
        LocalDateTime endDT = parseDateTime(endTimeRaw, id, name, rawStatus, "endTime");
        String normalizedStatus = normalizeStatus(rawStatus, startDT, endDT);

        boolean bidEnabled = "RUNNING".equals(normalizedStatus) && endDT != null;

        logger.info(
                "ProductCard id={}, name={}, rawStatus={}, startTimeRaw={}, endTimeRaw={}, normalizedStatus={}, bidEnabled={}",
                id, name, rawStatus, startTimeRaw, endTimeRaw, normalizedStatus, bidEnabled);

        String imagePath = itemObj.optString("imagePath", "default.png");

        VBox vbox = new VBox();
        vbox.setSpacing(4.0);
        vbox.setPrefWidth(240.0);
        vbox.setMinWidth(240.0);
        vbox.setMaxWidth(240.0);
        vbox.setPrefHeight(360.0);
        vbox.setMinHeight(360.0);
        vbox.setStyle(" -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 16px;  -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.05), 10, 0, 0, 2);");
        vbox.getStyleClass().add("dashboard-card");
        vbox.setPickOnBounds(true);
        vbox.setCursor(javafx.scene.Cursor.HAND);

        StackPane imageWrapper = new StackPane();
        imageWrapper.setPrefHeight(192.0);
        imageWrapper.setStyle("-fx-background-radius: 12px; -fx-border-radius: 12px;  -fx-border-width: 1px;");

        ImageView imageView = new ImageView();
        imageView.setFitHeight(192.0);
        imageView.setFitWidth(208.0);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Label imageStatusLabel = new Label("No Image");
        imageStatusLabel.setAlignment(Pos.CENTER);
        imageStatusLabel.setStyle("-fx-text-fill: #adb5bd;");

        String imageUrl = buildImageUrl(imagePath);
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
                    Platform.runLater(() -> {
                        imageWrapper.getChildren().remove(imageView);
                        if (!imageWrapper.getChildren().contains(imageStatusLabel)) {
                            imageWrapper.getChildren().add(0, imageStatusLabel);
                        }
                    });
                }
            });
        } else {
            imageWrapper.getChildren().add(imageStatusLabel);
        }

        if ("ENDED".equals(normalizedStatus) || "CLOSED".equals(normalizedStatus)) {
            // Slightly blur image to indicate ended, text remains visible
            imageWrapper.setOpacity(0.5);
        }

        Label timerIcon = new Label("\uE8B5");
        timerIcon.getStyleClass().add("material-icon");
        timerIcon.setStyle("-fx-font-size: 14px;");

        Label timerText = new Label("");
        timerText.setId("timerLabel_" + id);
        timerText.setStyle("-fx-font-weight: 900; -fx-font-size: 11px;");

        HBox timerBadge = new HBox(4.0);
        timerBadge.setId("timerBadge_" + id);
        timerBadge.setAlignment(Pos.CENTER);
        timerBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        if ("UPCOMING".equals(normalizedStatus)) {
            timerBadge.setStyle(
                    "-fx-background-color: rgba(96, 72, 104, 0.9); -fx-background-radius: 15px; -fx-padding: 4px 8px;");
            timerIcon.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px;");
            timerText.setStyle(
                    "-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: #ffffff; -fx-text-alignment: center;");

            String displayStart = "Opening Soon";
            if (startDT != null) {
                LocalDateTime now = LocalDateTime.now();
                if (startDT.toLocalDate().equals(now.toLocalDate())) {
                    displayStart = "Opening Soon\nStarts: "
                            + startDT.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                } else {
                    displayStart = "Opening Soon\nStarts: "
                            + startDT.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"));
                }
            }
            timerText.setText(displayStart);

            timerBadge.getChildren().addAll(timerIcon, timerText);
            StackPane.setAlignment(timerBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(timerBadge, new Insets(8, 8, 0, 0));
            imageWrapper.getChildren().add(timerBadge);
        } else if ("RUNNING".equals(normalizedStatus)) {
            timerBadge.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 15px; -fx-padding: 4px 8px;");
            timerIcon.setStyle("-fx-text-fill: -fx-accent; -fx-font-size: 14px;");
            timerText.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: -fx-accent;");
            
            // Calculate remaining time immediately on creation
            String displayRemaining = "Ongoing";
            if (endDT != null) {
                LocalDateTime now = LocalDateTime.now();
                java.time.Duration dur = java.time.Duration.between(now, endDT);
                long days = dur.toDays();
                long hours = dur.toHoursPart();
                long minutes = dur.toMinutesPart();
                long seconds = dur.toSecondsPart();
                if (days > 0) {
                    displayRemaining = "Ends in " + days + "d " + hours + "h";
                } else if (hours > 0) {
                    displayRemaining = "Ends in " + hours + "h " + minutes + "m";
                } else if (minutes > 0 || seconds > 0) {
                    displayRemaining = "Ends in " + minutes + "m " + seconds + "s";
                } else {
                    displayRemaining = "Ended";
                }
            }
            timerText.setText(displayRemaining);

            timerBadge.getChildren().addAll(timerIcon, timerText);
            StackPane.setAlignment(timerBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(timerBadge, new Insets(8, 8, 0, 0));
            imageWrapper.getChildren().add(timerBadge);
        } else if ("ENDED".equals(normalizedStatus)) {
            timerBadge.setStyle(
                    "-fx-background-color: rgba(100, 100, 100, 0.8); -fx-background-radius: 15px; -fx-padding: 4px 8px;");
            timerIcon.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px;");
            timerText.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: #ffffff;");

            String endLabel = "Ended";
            if ("CANCELED".equalsIgnoreCase(rawStatus)) {
                endLabel = "Canceled";
            } else if ("PAID".equalsIgnoreCase(rawStatus)) {
                endLabel = "Paid";
            }
            timerText.setText(endLabel);

            timerBadge.getChildren().addAll(timerIcon, timerText);
            StackPane.setAlignment(timerBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(timerBadge, new Insets(8, 8, 0, 0));
            imageWrapper.getChildren().add(timerBadge);
        } else {
            // UNKNOWN_TIME
            timerBadge.setStyle(
                    "-fx-background-color: rgba(220, 53, 69, 0.9); -fx-background-radius: 15px; -fx-padding: 4px 8px;");
            timerIcon.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px;");
            timerText.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: #ffffff;");
            timerText.setText("Unknown time");

            timerBadge.getChildren().addAll(timerIcon, timerText);
            StackPane.setAlignment(timerBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(timerBadge, new Insets(8, 8, 0, 0));
            imageWrapper.getChildren().add(timerBadge);
        }

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 16px; ");
        nameLabel.setWrapText(true);
        nameLabel.setMaxHeight(24.0);
        VBox.setMargin(nameLabel, new Insets(8, 0, 0, 0));

        Label categoryLabel = new Label(type);
        categoryLabel.setStyle("-fx-font-size: 13px; ");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        VBox priceBox = new VBox(0);
        Label lblCurrentBid = new Label("CURRENT BID");
        lblCurrentBid.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; ");
        Label priceLabel = new Label("₫ " + formatPrice(currentPrice));
        priceLabel.setId("priceLabel_" + id); // Set ID for fast updating
        priceLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 18px; -fx-text-fill: -fx-accent;");
        priceBox.getChildren().addAll(lblCurrentBid, priceLabel);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setMinWidth(102.0);
        actionBox.setPrefWidth(102.0);
        actionBox.setMaxWidth(102.0);

        Button mainBtn = new Button();
        Label mainPlusIcon = new Label("+");
        mainPlusIcon.setFont(Font.font("System", FontWeight.BOLD, 28));
        mainPlusIcon.setTextFill(Color.web("#ffffff"));
        mainPlusIcon.setAlignment(Pos.CENTER);
        mainPlusIcon.setMinSize(44.0, 44.0);
        mainPlusIcon.setPrefSize(44.0, 44.0);
        mainPlusIcon.setMaxSize(44.0, 44.0);
        mainPlusIcon.setTranslateY(-1.5);
        mainPlusIcon.setTranslateX(0.0);
        mainBtn.setGraphic(mainPlusIcon);
        mainBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        mainBtn.setMinSize(44.0, 44.0);
        mainBtn.setPrefSize(44.0, 44.0);
        mainBtn.setMaxSize(44.0, 44.0);
        mainBtn.setPadding(Insets.EMPTY);
        mainBtn.setAlignment(Pos.CENTER);
        mainBtn.setStyle("-fx-background-color: -fx-accent; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand;");
        Tooltip.install(mainBtn, new Tooltip("Options"));

        Button btnWatch = new Button();
        Label watchIcon = new Label(User.watchlistIds.contains(id) ? "\uE87D" : "\uE87E"); // heart filled or outline
        watchIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-text-fill: " + (User.watchlistIds.contains(id) ? "-fx-accent" : "-app-text-muted") + ";");
        watchIcon.setAlignment(Pos.CENTER);
        watchIcon.setMinSize(44.0, 44.0);
        watchIcon.setPrefSize(44.0, 44.0);
        watchIcon.setMaxSize(44.0, 44.0);
        watchIcon.setTranslateY(1.5);
        btnWatch.setGraphic(watchIcon);
        btnWatch.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        btnWatch.setMinSize(44.0, 44.0);
        btnWatch.setPrefSize(44.0, 44.0);
        btnWatch.setMaxSize(44.0, 44.0);
        btnWatch.setPadding(Insets.EMPTY);
        btnWatch.setAlignment(Pos.CENTER);
        btnWatch.setStyle(" -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand;");
        Tooltip.install(btnWatch, new Tooltip(User.watchlistIds.contains(id) ? "Favorited" : "Add to favorites"));

        Button btnBid = new Button();
        Label bidIcon = new Label("\uE8CC"); // shopping cart / bid
        bidIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-text-fill: white;");
        bidIcon.setAlignment(Pos.CENTER);
        bidIcon.setMinSize(44.0, 44.0);
        bidIcon.setPrefSize(44.0, 44.0);
        bidIcon.setMaxSize(44.0, 44.0);
        bidIcon.setTranslateY(1.5);
        bidIcon.setTranslateX(0.5);
        btnBid.setGraphic(bidIcon);
        btnBid.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        btnBid.setMinSize(44.0, 44.0);
        btnBid.setPrefSize(44.0, 44.0);
        btnBid.setMaxSize(44.0, 44.0);
        btnBid.setPadding(Insets.EMPTY);
        btnBid.setAlignment(Pos.CENTER);
        btnBid.setStyle("-fx-background-color: -fx-accent; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(224,64,160,0.3), 8, 0, 0, 2);");
        Tooltip.install(btnBid, new Tooltip("Bid Now"));

        btnWatch.setVisible(false);
        btnWatch.setManaged(false);
        btnBid.setVisible(false);
        btnBid.setManaged(false);

        // Click on the whole card to open details (especially good for ENDED cards)
        vbox.setOnMouseClicked(event -> {
            openAuctionPage(event, sessionObj, itemObj, name, id, currentPrice);
        });

        if (bidEnabled) {
            btnBid.setOnAction(event -> {
                event.consume(); // prevent triggering the vbox click
                openAuctionPage(event, sessionObj, itemObj, name, id, currentPrice);
            });

            btnWatch.setOnAction(event -> {
                event.consume();
                if (User.getId() == null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Login Required");
                    alert.setHeaderText(null);
                    alert.setContentText("Please log in to use the Favorites feature!");
                    alert.show();
                    return;
                }
                if (User.watchlistIds.contains(id)) {
                    User.watchlistIds.remove(id);
                    watchIcon.setText("\uE87E");
                    watchIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; ");
                    watchIcon.setTranslateY(1.5);
                    Tooltip.install(btnWatch, new Tooltip("Add to favorites"));
                    ClientLogger.logFavorite(User.getUsername(), name, id, false);
                } else {
                    User.watchlistIds.add(id);
                    watchIcon.setText("\uE87D");
                    watchIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-text-fill: -fx-accent;");
                    watchIcon.setTranslateY(1.5);
                    Tooltip.install(btnWatch, new Tooltip("Favorited"));
                    ClientLogger.logFavorite(User.getUsername(), name, id, true);
                }
                if (showingWatchlistOnly) {
                    filterAndRenderProducts();
                }
            });

            actionBox.setOnMouseEntered(e -> {
                mainBtn.setVisible(false);
                mainBtn.setManaged(false);
                btnWatch.setVisible(true);
                btnWatch.setManaged(true);
                btnBid.setVisible(true);
                btnBid.setManaged(true);
            });
            actionBox.setOnMouseExited(e -> {
                btnWatch.setVisible(false);
                btnWatch.setManaged(false);
                btnBid.setVisible(false);
                btnBid.setManaged(false);
                mainBtn.setVisible(true);
                mainBtn.setManaged(true);
            });

            mainBtn.setOnAction(e -> {
                e.consume();
                mainBtn.setVisible(false);
                mainBtn.setManaged(false);
                btnWatch.setVisible(true);
                btnWatch.setManaged(true);
                btnBid.setVisible(true);
                btnBid.setManaged(true);
            });

            actionBox.getChildren().addAll(btnWatch, btnBid, mainBtn);
        } else {
            // Non-running / invalid cards show a single action button
            mainPlusIcon.setText("\uE8F4"); // Eye icon
            mainPlusIcon.setFont(Font.font("Material Symbols Outlined", FontWeight.NORMAL, 24));
            mainPlusIcon.setTranslateY(2.5);
            mainPlusIcon.setTranslateX(0.0);

            if ("UPCOMING".equals(normalizedStatus)) {
                mainPlusIcon.setTextFill(Color.web("#ffffff"));
                mainBtn.setStyle("-fx-background-color: -fx-accent; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand;");
                Tooltip.install(mainBtn, new Tooltip("View details"));
                mainBtn.setDisable(false);
                mainBtn.setOnAction(e -> {
                    e.consume();
                    openAuctionPage(e, sessionObj, itemObj, name, id, currentPrice);
                });
            } else if ("ENDED".equals(normalizedStatus)) {
                mainPlusIcon.setTextFill(Color.web("#ffffff"));
                mainBtn.setStyle("-fx-background-color: -fx-accent; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand;");
                Tooltip.install(mainBtn, new Tooltip("View results"));
                mainBtn.setDisable(false);
                mainBtn.setOnAction(e -> {
                    e.consume();
                    openAuctionPage(e, sessionObj, itemObj, name, id, currentPrice);
                });
            } else {
                // UNKNOWN_TIME
                mainPlusIcon.setTextFill(Color.web("-app-text-muted"));
                mainBtn.setStyle(" -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: default;");
                Tooltip.install(mainBtn, new Tooltip("Auction time unknown"));
                mainBtn.setDisable(true);
                mainBtn.setOnAction(e -> {
                    e.consume(); // prevent click from propagating
                });
            }

            actionBox.getChildren().add(mainBtn);
        }

        bottomRow.getChildren().addAll(priceBox, hSpacer, actionBox);
        vbox.getChildren().addAll(imageWrapper, nameLabel, categoryLabel, spacer, bottomRow);

        return vbox;
    }

    @FXML
    private void handleNotifications(ActionEvent event) {
        showInfo("Notification", buildNotificationSummary());
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, "Settings.fxml", 1280, 800);
        } catch (IOException e) {
            logger.error("Error switching to Settings.fxml: ", e);
        }
    }

    @FXML
    private void handleListView(ActionEvent event) {
        compactProductListMode = true;
        showCompactAuctionList();
        updateViewToggleButton(true);
    }

    @FXML
    private void handleToggleProductView(ActionEvent event) {
        compactProductListMode = !compactProductListMode;
        if (compactProductListMode) {
            showCompactAuctionList();
        } else {
            returnToAuctionGrid();
        }
        updateViewToggleButton(compactProductListMode);
    }

    private void updateViewToggleButton(boolean compactMode) {
        if (btnToggleProductView == null) {
            return;
        }

        btnToggleProductView.getStyleClass().remove("view-toggle-button-active");
        if (compactMode) {
            if (!btnToggleProductView.getStyleClass().contains("view-toggle-button-active")) {
                btnToggleProductView.getStyleClass().add("view-toggle-button-active");
            }
            btnToggleProductView.setTooltip(new Tooltip("List view is active. Click to return to grid view."));
        } else {
            btnToggleProductView.setTooltip(new Tooltip("Show compact list view"));
        }

        btnToggleProductView.setText("");
        btnToggleProductView.setAlignment(Pos.CENTER);
        btnToggleProductView.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private void showAccountScreen() {
        if (User.getId() == null) {
            showWarning("Yêu cầu đăng nhập", "Vui lòng đăng nhập để xem và chỉnh sửa thông tin tài khoản.");
            return;
        }

        showingAccountScreen = true;
        showingCompactListScreen = false;
        compactProductListMode = false;
        updateViewToggleButton(false);
        renderAccountScreen(false);
        loadLatestAccountProfileForScreen();
    }

    @FXML
    public void handleResetDashboard(ActionEvent event) {
        logger.info("Resetting dashboard filters and search text...");
        showingAccountScreen = false;
        showingCompactListScreen = false;
        compactProductListMode = false;
        updateViewToggleButton(false);
        if (txtSearch != null) {
            txtSearch.clear();
        }
        if (cbCategory != null) {
            cbCategory.setValue("All");
        }
        if (cbStatus != null) {
            cbStatus.setValue("All");
        }
        forceRenderProducts = true;
        filterAndRenderProducts();
    }

    private void renderAccountScreen(boolean saving) {
        stopCountdownTimeline();
        productContainer.getChildren().clear();
        productContainer.getChildren().add(fakeTestBtn);
        currentRenderedIds.clear();
        productContainer.setAlignment(Pos.TOP_LEFT);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Selective hiding: only hide page title and filter controls
        hideFilterControlsForAccountPage(true);
        if (topbarController != null) {
            topbarController.setSearchVisible(false);
        }

        VBox wrapper = new VBox(22);
        wrapper.getStyleClass().add("account-page-wrapper");
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(Double.MAX_VALUE);

        VBox headerSection = buildAccountHeader();
        HBox topSection = buildAccountTopSection(saving);
        VBox formCard = buildPersonalInfoForm(saving);

        wrapper.getChildren().addAll(headerSection, topSection, formCard);
        productContainer.getChildren().add(wrapper);

        // Responsive layout listener
        Platform.runLater(() -> {
            if (scrollPane.getScene() != null) {
                wrapper.prefWidthProperty().bind(scrollPane.widthProperty().subtract(28));
                headerSection.prefWidthProperty().bind(wrapper.widthProperty());
                topSection.prefWidthProperty().bind(wrapper.widthProperty());
                formCard.prefWidthProperty().bind(wrapper.widthProperty());
                applyResponsiveAccountLayout(scrollPane.getWidth(), topSection);
                scrollPane.widthProperty().addListener((obs, oldW, newW) -> {
                    if (showingAccountScreen) {
                        applyResponsiveAccountLayout(newW.doubleValue(), topSection);
                    }
                });
            }
        });
    }

    private void hideFilterControlsForAccountPage(boolean hide) {
        try {
            if (mainPageHeader != null) {
                mainPageHeader.setVisible(!hide);
                mainPageHeader.setManaged(!hide);
            }
            if (lblPageTitle != null) {
                lblPageTitle.setVisible(!hide);
                lblPageTitle.setManaged(!hide);
            }
            if (filterControlsBox != null) {
                filterControlsBox.setVisible(!hide);
                filterControlsBox.setManaged(!hide);
            }
        } catch (Exception e) {
            logger.warn("Cannot hide/show filter controls: {}", e.getMessage());
        }
    }

    private VBox buildAccountHeader() {
        VBox headerBox = new VBox(2);
        headerBox.setMaxWidth(Double.MAX_VALUE);
        headerBox.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("My Account");
        title.getStyleClass().add("account-page-title");
        Label subtitle = new Label("Manage profile, avatar, and personal information.");
        subtitle.getStyleClass().add("account-page-subtitle");

        headerBox.getChildren().addAll(title, subtitle);
        return headerBox;
    }

    private HBox buildAccountTopSection(boolean saving) {
        HBox topSection = new HBox(22);
        topSection.setMaxWidth(Double.MAX_VALUE);
        topSection.setAlignment(Pos.TOP_CENTER);

        VBox profileCard = buildProfileSummaryCard();
        HBox.setHgrow(profileCard, Priority.ALWAYS);

        VBox statsColumn = buildAccountStats();
        HBox.setHgrow(statsColumn, Priority.NEVER);

        topSection.getChildren().addAll(profileCard, statsColumn);
        return topSection;
    }

    private VBox buildProfileSummaryCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("profile-summary-card");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(280);
        card.setMaxWidth(Double.MAX_VALUE);

        StackPane avatarPane = createAvatarView();

        Label nameLabel = new Label(safeText(User.getFullname(), "User"));
        nameLabel.getStyleClass().add("profile-name");

        Label emailLabel = new Label(safeText(User.getEmail(), ""));
        emailLabel.getStyleClass().add("profile-email");

        Label roleBadge = new Label(safeText(User.getRole(), "user").toUpperCase());
        roleBadge.getStyleClass().add("profile-role-badge");

        Button btnChangeAvatar = new Button("Change Avatar");
        btnChangeAvatar.getStyleClass().add("btn-avatar-change");
        btnChangeAvatar.setOnAction(e -> handleAvatarUpload(btnChangeAvatar));

        card.getChildren().addAll(avatarPane, nameLabel, emailLabel, roleBadge, btnChangeAvatar);
        return card;
    }

    private StackPane createAvatarView() {
        double size = 120;
        StackPane container = new StackPane();
        container.setMinSize(size, size);
        container.setMaxSize(size, size);
        container.setPrefSize(size, size);

        String avatarUrl = User.getAvatarUrl();
        boolean hasAvatar = avatarUrl != null && !avatarUrl.isBlank();

        if (hasAvatar) {
            try {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl
                        : Config.API_URL + avatarUrl;
                ImageView imgView = new ImageView();
                imgView.setFitWidth(size);
                imgView.setFitHeight(size);
                imgView.setPreserveRatio(false);
                imgView.setSmooth(true);

                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(size / 2, size / 2, size / 2);
                imgView.setClip(clip);

                Image img = new Image(fullUrl, size, size, false, true, true);
                img.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) {
                        Platform.runLater(() -> {
                            container.getChildren().clear();
                            container.getChildren().add(buildInitialsAvatar(size));
                        });
                    }
                });
                imgView.setImage(img);
                container.getChildren().add(imgView);
            } catch (Exception e) {
                container.getChildren().add(buildInitialsAvatar(size));
            }
        } else {
            container.getChildren().add(buildInitialsAvatar(size));
        }

        // Hover overlay with camera icon
        StackPane overlay = new StackPane();
        overlay.setMinSize(size, size);
        overlay.setMaxSize(size, size);
        overlay.getStyleClass().add("profile-avatar-overlay");
        Label cameraIcon = new Label("\uE3B0");
        cameraIcon.getStyleClass().add("profile-avatar-overlay-icon");
        overlay.getChildren().add(cameraIcon);
        overlay.setOnMouseClicked(e -> {
            // Find the change avatar button and trigger it
            handleAvatarUpload(null);
        });
        container.getChildren().add(overlay);

        return container;
    }

    private StackPane buildInitialsAvatar(double size) {
        StackPane placeholder = new StackPane();
        placeholder.setMinSize(size, size);
        placeholder.setMaxSize(size, size);
        placeholder.setStyle("-fx-background-color: linear-gradient(to bottom right, -fx-accent, #c83090);"
                + " -fx-background-radius: " + (size / 2) + ";");

        String initials = getInitials(User.getFullname());
        Label initialsLabel = new Label(initials);
        initialsLabel.getStyleClass().add("profile-avatar-initials");
        placeholder.getChildren().add(initialsLabel);
        return placeholder;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank())
            return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    }

    private void handleAvatarUpload(Button btnChangeAvatar) {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Avatar");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp"));

        javafx.stage.Window window = scrollPane.getScene().getWindow();
        java.io.File file = chooser.showOpenDialog(window);
        if (file == null)
            return;

        // Client-side size validation
        if (file.length() > 5 * 1024 * 1024) {
            showWarning("File too large", "Avatar image must be under 5MB.");
            return;
        }

        // Disable button + loading state
        if (btnChangeAvatar != null) {
            btnChangeAvatar.setDisable(true);
            btnChangeAvatar.setText("Uploading...");
        }

        // Upload on background thread
        new Thread(() -> {
            try {
                String boundary = java.util.UUID.randomUUID().toString();
                String fileName = file.getName();
                byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());

                String contentType = "application/octet-stream";
                String lName = fileName.toLowerCase();
                if (lName.endsWith(".png"))
                    contentType = "image/png";
                else if (lName.endsWith(".jpg") || lName.endsWith(".jpeg"))
                    contentType = "image/jpeg";
                else if (lName.endsWith(".webp"))
                    contentType = "image/webp";

                // Build multipart body
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                bos.write((twoHyphens + boundary + lineEnd).getBytes());
                bos.write(("Content-Disposition: form-data; name=\"avatar\"; filename=\"" + fileName + "\"" + lineEnd)
                        .getBytes());
                bos.write(("Content-Type: " + contentType + lineEnd).getBytes());
                bos.write(lineEnd.getBytes());
                bos.write(fileBytes);
                bos.write(lineEnd.getBytes());
                bos.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());

                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/users/" + User.getId() + "/avatar"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(bos.toByteArray()));
                if (User.getSessionToken() != null) {
                    builder.header("X-Auth-Token", User.getSessionToken());
                }
                HttpRequest request = builder.build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject responseJson = new JSONObject(response.body());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200 && responseJson.optInt("status", 500) == 200) {
                        JSONObject data = responseJson.optJSONObject("data");
                        String newAvatarUrl = data != null ? data.optString("avatarUrl", null) : null;
                        if (newAvatarUrl != null) {
                            User.setAvatarUrl(newAvatarUrl);
                            if (topbarController != null) {
                                topbarController.updateTopBarAvatar(newAvatarUrl);
                            }
                        }
                        renderAccountScreen(false);
                        showInfo("Success", "Avatar updated successfully.");
                    } else {
                        String msg = responseJson.optString("message", "Upload failed.");
                        showError("Upload Failed", msg);
                        resetAvatarButton(btnChangeAvatar);
                    }
                });
            } catch (Exception e) {
                logger.error("Avatar upload error: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    showError("Upload Failed", "Cannot connect to server.");
                    resetAvatarButton(btnChangeAvatar);
                });
            }
        }, "upload-avatar").start();
    }

    private void resetAvatarButton(Button btn) {
        if (btn != null) {
            btn.setDisable(false);
            btn.setText("Change Avatar");
        }
    }

    private VBox buildAccountStats() {
        VBox statsColumn = new VBox(14);
        statsColumn.setAlignment(Pos.TOP_LEFT);
        statsColumn.setMinWidth(200);
        statsColumn.setPrefWidth(280);
        statsColumn.setMaxWidth(320);

        statsColumn.getChildren().addAll(
                createProfileStatCard("Account Balance", "₫ " + formatPrice(User.getBalance()), "\uE227"),
                createProfileStatCard("Role", safeText(User.getRole(), "Unknown"), "\uE7FD"),
                createProfileStatCard("User ID", String.valueOf(User.getId()), "\uE838"));
        return statsColumn;
    }

    private VBox createProfileStatCard(String title, String value, String iconCode) {
        VBox box = new VBox(6);
        box.getStyleClass().add("account-stat-card");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(iconCode);
        icon.getStyleClass().add("account-stat-icon");

        VBox textBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("account-stat-label");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("account-stat-value");
        valueLabel.setWrapText(true);
        textBox.getChildren().addAll(titleLabel, valueLabel);

        row.getChildren().addAll(icon, textBox);
        box.getChildren().add(row);
        return box;
    }

    private VBox buildPersonalInfoForm(boolean saving) {
        VBox card = new VBox(18);
        card.getStyleClass().add("account-form-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Label formTitle = new Label("Personal Information");
        formTitle.getStyleClass().add("account-form-title");

        GridPane form = new GridPane();
        form.setHgap(20);
        form.setVgap(16);

        TextField usernameField = createAccountField(safeText(User.getUsername(), ""), "Username");
        TextField fullnameField = createAccountField(safeText(User.getFullname(), ""), "Full Name");
        TextField emailField = createAccountField(safeText(User.getEmail(), ""), "email@example.com");
        TextField dobField = createAccountField(safeText(User.getDob(), ""), "YYYY-MM-DD or leave blank");
        TextField placeField = createAccountField(safeText(User.getPlace_of_birth(), ""), "Place of Birth");

        addAccountFormRow(form, 0, "Username", usernameField);
        addAccountFormRow(form, 1, "Full Name", fullnameField);
        addAccountFormRow(form, 2, "Email", emailField);
        addAccountFormRow(form, 3, "Date of Birth", dobField);
        addAccountFormRow(form, 4, "Place of Birth", placeField);

        // Make fields stretch
        javafx.scene.layout.ColumnConstraints col0 = new javafx.scene.layout.ColumnConstraints();
        col0.setMinWidth(84);
        col0.setPrefWidth(130);
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col1.setMinWidth(0);
        form.getColumnConstraints().addAll(col0, col1);

        HBox actions = new HBox(14);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        Button reloadButton = new Button("Reload Information");
        reloadButton.setDisable(saving);
        reloadButton.getStyleClass().add("btn-account-secondary");
        reloadButton.setOnAction(e -> loadLatestAccountProfileForScreen());

        Button saveButton = new Button(saving ? "Saving..." : "Save Changes");
        saveButton.setDisable(saving);
        saveButton.getStyleClass().add("btn-account-primary");
        saveButton.setOnAction(e -> {
            String username = readTrimmed(usernameField);
            String fullname = readTrimmed(fullnameField);
            String email = readTrimmed(emailField);
            String dob = readTrimmed(dobField);
            String placeOfBirth = readTrimmed(placeField);

            if (username.isBlank()) {
                showWarning("Missing Username", "Username cannot be empty.");
                return;
            }
            if (fullname.isBlank()) {
                showWarning("Missing Full Name", "Full name cannot be empty.");
                return;
            }
            if (email.isBlank() || !email.contains("@")) {
                showWarning("Invalid Email", "Please enter a valid email address.");
                return;
            }

            renderAccountScreen(true);
            updateAccountProfile(username, fullname, email, dob, placeOfBirth);
        });

        actions.getChildren().addAll(reloadButton, saveButton);
        card.getChildren().addAll(formTitle, form, actions);
        return card;
    }

    private TextField createAccountField(String value, String prompt) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        field.getStyleClass().add("account-input");
        field.setMinWidth(0);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private void addAccountFormRow(GridPane grid, int row, String label, TextField field) {
        Label rowLabel = new Label(label);
        rowLabel.getStyleClass().add("account-form-label");
        rowLabel.setMinWidth(100);
        grid.add(rowLabel, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void applyResponsiveAccountLayout(double width, HBox topSection) {
        if (topSection == null)
            return;
        // Make use of horizontal space better
        if (width < 900) {
            topSection.setSpacing(14);
            if (topSection.getChildren().size() == 2) {
                topSection.setMaxWidth(Double.MAX_VALUE);
            }
        } else {
            topSection.setSpacing(22);
            topSection.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private void loadLatestAccountProfileForScreen() {
        if (User.getId() == null)
            return;

        new Thread(() -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/users/" + User.getId()))
                        .GET();
                if (User.getSessionToken() != null) {
                    builder.header("X-Auth-Token", User.getSessionToken());
                }
                HttpRequest request = builder.build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject responseJson = new JSONObject(response.body());
                if (response.statusCode() == 200 && responseJson.optInt("status", 500) == 200) {
                    JSONObject data = responseJson.optJSONObject("data");
                    if (data != null) {
                        applyUserProfileFromJson(data);
                        Platform.runLater(() -> {
                            if (showingAccountScreen) {
                                renderAccountScreen(false);
                                if (topbarController != null) {
                                    topbarController.updateTopBarAvatar(User.getAvatarUrl());
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                logger.warn("Cannot reload account info: {}", e.getMessage());
            }
        }, "load-account-profile").start();
    }

    private void applyUserProfileFromJson(JSONObject data) {
        String avatarUrl = data.optString("avatarUrl", data.optString("avatar_url", null));
        if ("null".equals(avatarUrl))
            avatarUrl = null;

        User.updateProfile(
                data.optString("username", safeText(User.getUsername(), "")),
                data.optString("fullname", safeText(User.getFullname(), "")),
                data.optString("email", safeText(User.getEmail(), "")),
                data.optString("dob", safeText(User.getDob(), "")),
                data.optString("placeOfBirth",
                        data.optString("place_of_birth", safeText(User.getPlace_of_birth(), ""))),
                parseMoney(data.opt("balance"), User.getBalance()),
                avatarUrl);
    }

    private void updateAccountProfile(String username, String fullname, String email, String dob, String placeOfBirth) {
        JSONObject payload = new JSONObject();
        payload.put("username", username);
        payload.put("fullname", fullname);
        payload.put("email", email);
        payload.put("dob", dob);
        payload.put("placeOfBirth", placeOfBirth);

        new Thread(() -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/users/" + User.getId() + "/profile"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()));
                if (User.getSessionToken() != null) {
                    builder.header("X-Auth-Token", User.getSessionToken());
                }
                HttpRequest request = builder.build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject responseJson = new JSONObject(response.body());
                int status = responseJson.optInt("status", response.statusCode());
                String message = responseJson.optString("message", "Profile updated successfully.");

                if (response.statusCode() == 200 && status == 200) {
                    JSONObject data = responseJson.optJSONObject("data");
                    if (data != null) {
                        applyUserProfileFromJson(data);
                    } else {
                        User.updateProfile(username, fullname, email, dob, placeOfBirth);
                    }
                    Platform.runLater(() -> {
                        renderAccountScreen(false);
                        showInfo("Account", message);
                    });
                } else {
                    Platform.runLater(() -> {
                        renderAccountScreen(false);
                        showError("Update Failed", message);
                    });
                }
            } catch (Exception e) {
                logger.error("Error updating account: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    renderAccountScreen(false);
                    showError("Update Failed", "Cannot connect to server or invalid response data.");
                });
            }
        }, "update-account-profile").start();
    }

    private String buildNotificationSummary() {
        int total = allProducts.size();
        int active = 0;
        int ended = 0;
        int watchlist = 0;
        int mySessions = 0;

        for (JSONObject sessionObj : allProducts) {
            String status = sessionObj.optString("status", "");
            if ("ACTIVE".equalsIgnoreCase(status)) {
                active++;
            } else if ("ENDED".equalsIgnoreCase(status)) {
                ended++;
            }

            int sessionId = sessionObj.optInt("id", -1);
            if (User.watchlistIds.contains(sessionId)) {
                watchlist++;
            }
            if (isSessionOwnedByCurrentUser(sessionObj)) {
                mySessions++;
            }
        }

        return "Total sessions loading: " + total
                + "\nActive sessions: " + active
                + "\nEnded sessions: " + ended
                + "\nWatchlist sessions: " + watchlist
                + "\nMy sessions: " + mySessions
                + "\nCurrent balance: ₫ " + formatPrice(User.getBalance());
    }

    private void showSettingsDialog() {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Quick Settings");
        dialog.setHeaderText("Choose an action");
        dialog.setContentText("What do you want to do with the auction list screen?");

        ButtonType resetFilters = new ButtonType("Reset Filters");
        ButtonType reloadData = new ButtonType("Reload Data");
        ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getButtonTypes().setAll(resetFilters, reloadData, close);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == close) {
            return;
        }

        if (result.get() == resetFilters) {
            resetFiltersAndShowAll();
            showInfo("Settings", "Filters have been reset to default.");
        } else if (result.get() == reloadData) {
            forceRenderProducts = true;
            loadProductsFromServer();
            showInfo("Settings", "Data reload has been requested from the server.");
        }
    }

    private void showCompactAuctionList() {
        stopCountdownTimeline();
        showingCompactListScreen = true;
        compactProductListMode = true;
        updateViewToggleButton(true);
        showingAccountScreen = false;

        List<JSONObject> sessionsToShow = getCurrentlyDisplayedSessions();

        productContainer.getChildren().clear();
        productContainer.getChildren().add(fakeTestBtn);
        productContainer.setAlignment(Pos.TOP_LEFT);

        VBox wrapper = new VBox(16);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(22, 24, 40, 24));
        wrapper.setPrefWidth(
                Math.max(760, productContainer.getPrefWidth() > 0 ? productContainer.getPrefWidth() - 80 : 900));

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(900);

        VBox titleBox = new VBox(2);
        Label title = new Label("Compact Session List");
        title.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 26px; -fx-font-weight: 900; ");
        Label subtitle = new Label("Sessions currently displayed based on filters.");
        subtitle.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; ");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button("Back to Grid");
        backButton.setStyle("-fx-background-color: -fx-accent; -fx-background-radius: 999; -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-weight: bold; -fx-padding: 9 22 9 22; -fx-cursor: hand;");
        backButton.setOnAction(e -> returnToAuctionGrid());
        header.getChildren().addAll(titleBox, spacer, backButton);

        VBox listBox = new VBox(10);
        listBox.setMaxWidth(900);

        if (sessionsToShow.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            empty.setStyle(" -fx-background-radius: 22px;  -fx-border-radius: 22px; -fx-border-width: 2px;");
            Label emptyTitle = new Label("No sessions found with current filters");
            emptyTitle.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 18px; -fx-font-weight: 900; ");
            Label emptyMsg = new Label("Try changing filters or return to grid view.");
            emptyMsg.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; ");
            empty.getChildren().addAll(emptyTitle, emptyMsg);
            listBox.getChildren().add(empty);
        } else {
            int index = 1;
            for (JSONObject sessionObj : sessionsToShow) {
                listBox.getChildren().add(createCompactAuctionRow(index++, sessionObj));
            }
        }

        wrapper.getChildren().addAll(header, listBox);
        productContainer.getChildren().add(wrapper);
    }

    private List<JSONObject> getCurrentlyDisplayedSessions() {
        List<JSONObject> sessions = new ArrayList<>();
        for (JSONObject sessionObj : allProducts) {
            if (currentRenderedIds.contains(sessionObj.optInt("id"))) {
                sessions.add(sessionObj);
            }
        }
        return sessions;
    }

    private HBox createCompactAuctionRow(int index, JSONObject sessionObj) {
        JSONObject itemObj = getItemObject(sessionObj);
        int sessionId = sessionObj.optInt("id");
        String itemName = itemObj.optString("name", "Unknown");
        String itemType = itemObj.optString("type", "Unknown category");
        BigDecimal currentPrice = getMoney(sessionObj, "currentPrice",
                getMoney(sessionObj, "startingPrice", BigDecimal.ZERO));
        String status = normalizeSession(sessionObj);
        boolean canBid = "RUNNING".equalsIgnoreCase(status);

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setStyle(" -fx-background-radius: 18px;  -fx-border-width: 1.5px; -fx-border-radius: 18px; -fx-cursor: hand;");
        row.setOnMouseClicked(event -> {
            openAuctionPage(event, sessionObj, itemObj, itemObj.optString("name", "Unknown"), sessionObj.optInt("id"),
                    currentPrice);
        });

        Label order = new Label(String.valueOf(index));
        order.setAlignment(Pos.CENTER);
        order.setMinSize(34, 34);
        order.setPrefSize(34, 34);
        order.setStyle(" -fx-background-radius: 17px; -fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: -fx-accent;");

        VBox infoBox = new VBox(3);
        Label name = new Label("#" + sessionObj.optInt("id") + " · " + itemObj.optString("name", "Unknown"));
        name.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 15px; -fx-font-weight: 900; ");
        Label type = new Label(itemObj.optString("type", "Unknown category"));
        type.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 12px; ");
        infoBox.getChildren().addAll(name, type);

        Region rowSpacer = new Region();
        HBox.setHgrow(rowSpacer, Priority.ALWAYS);

        Label statusBadge = new Label(status);
        statusBadge.setStyle(" -fx-background-radius: 999; -fx-padding: 5 12 5 12; -fx-font-family: 'DM Sans'; -fx-font-size: 11px; -fx-font-weight: 900; ");

        Label price = new Label("₫ " + formatPrice(currentPrice));
        price.setMinWidth(110);
        price.setAlignment(Pos.CENTER_RIGHT);
        price.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: -fx-accent;");

        Button bidButton = new Button(canBid ? "Bid" : "Ended");
        bidButton.setMinWidth(92);
        bidButton.setPrefHeight(38);
        bidButton.setDisable(!canBid);
        if (canBid) {
            bidButton.setStyle(
                    "-fx-background-color: #e040a0; -fx-background-radius: 999; -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 8 18 8 18; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(224,64,160,0.25), 10, 0, 0, 3);");
            bidButton.setOnAction(event -> {
                event.consume();
                openAuctionPage(event, sessionObj, itemObj, itemName, sessionId, currentPrice);
            });
        } else {
            bidButton.setStyle(
                    "-fx-background-color: #f2e8f2; -fx-background-radius: 999; -fx-text-fill: #907898; -fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 8 18 8 18;");
        }

        row.getChildren().addAll(order, infoBox, rowSpacer, statusBadge, price, bidButton);
        return row;
    }

    private void returnToAuctionGrid() {
        showingAccountScreen = false;
        showingCompactListScreen = false;
        compactProductListMode = false;
        updateViewToggleButton(false);
        forceRenderProducts = true;
        filterAndRenderProducts();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        if (topbarController != null) {
            topbarController.setSearchVisible(true);
        }
    }

    private String readTrimmed(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private BigDecimal parseMoney(Object value, BigDecimal fallback) {
        if (value == null || JSONObject.NULL.equals(value)) {
            return fallback == null ? BigDecimal.ZERO : fallback;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return fallback == null ? BigDecimal.ZERO : fallback;
        }
    }

    private void resetFiltersAndShowAll() {
        txtSearch.clear();
        cbCategory.setValue("All");
        cbStatus.setValue("All");
        showAllSessions();
        if (sidebarController != null) {
            sidebarController.setActiveDashboard();
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void handleDepositMoney(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, "Deposit.fxml", 1280, 800);
        } catch (IOException e) {
            logger.error("Error switching to deposit page: ", e);
            showError("Error", "Cannot load Deposit page.");
        }
    }

    public enum ToastType {
        SUCCESS, ERROR, WARNING, INFO
    }

    private void showToast(String title, String message, ToastType type) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showToast(title, message, type));
            return;
        }
        if (toastContainer == null) {
            Alert.AlertType alertType = Alert.AlertType.INFORMATION;
            if (type == ToastType.ERROR)
                alertType = Alert.AlertType.ERROR;
            if (type == ToastType.WARNING)
                alertType = Alert.AlertType.WARNING;
            showAlert(alertType, title, message);
            return;
        }

        HBox toast = new HBox(14);
        toast.getStyleClass().addAll("app-toast", "app-toast-" + type.name().toLowerCase());
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMaxWidth(400);

        Label icon = new Label();
        icon.getStyleClass().addAll("app-toast-icon", "app-toast-icon-" + type.name().toLowerCase());
        switch (type) {
            case SUCCESS:
                icon.setText("\ue86c");
                break; // check_circle
            case ERROR:
                icon.setText("\ue000");
                break; // error
            case WARNING:
                icon.setText("\ue002");
                break; // warning
            case INFO:
                icon.setText("\ue88e");
                break; // info
        }

        VBox textBox = new VBox(4);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("app-toast-title");
        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("app-toast-message");
        msgLabel.setWrapText(true);
        textBox.getChildren().addAll(titleLabel, msgLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button closeBtn = new Button("\ue5cd"); // close
        closeBtn.getStyleClass().add("app-toast-close");

        toast.getChildren().addAll(icon, textBox, closeBtn);

        toast.setOpacity(0); // OK for animation
        toast.setTranslateY(20);
        toastContainer.getChildren().add(toast);

        javafx.animation.ParallelTransition fadeIn = new javafx.animation.ParallelTransition();
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300),
                toast);
        fade.setFromValue(0);
        fade.setToValue(1);
        javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(
                javafx.util.Duration.millis(300), toast);
        slide.setFromY(20);
        slide.setToY(0);
        fadeIn.getChildren().addAll(fade, slide);
        fadeIn.play();

        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(3.5));
        delay.setOnFinished(e -> {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(300), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e2 -> toastContainer.getChildren().remove(toast));
            fadeOut.play();
        });
        delay.play();

        closeBtn.setOnAction(e -> {
            delay.stop();
            toastContainer.getChildren().remove(toast);
        });
    }

    private void showInfo(String title, String message) {
        if (title.toLowerCase().contains("success")) {
            showToast(title, message, ToastType.SUCCESS);
        } else {
            showToast(title, message, ToastType.INFO);
        }
    }

    private void showWarning(String title, String message) {
        showToast(title, message, ToastType.WARNING);
    }

    private void showError(String title, String message) {
        showToast(title, message, ToastType.ERROR);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void handleLogout(ActionEvent event) throws IOException {
        User.clearSession();
        SceneSwitcher.switchScene(event, "Login.fxml", 1100, 700);
    }

    public void setHttpClient(HttpClient httpClient) {
        this.client = httpClient;
    }

    /**
     * Connect Socket to receive real-time events from server (e.g. AUCTION_ENDED)
     * Reuse Socket infrastructure, send JOIN_HOME command to register for global
     * events.
     */
    private void connectHomeSocket() {
        Thread homeSocketThread = new Thread(() -> {
            try (Socket socket = new Socket(Config.SOCKET_HOST, Config.PORT_SOCKET)) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);

                // Register with server that this is Home client
                out.println("JOIN_HOME");

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("EVENT:")) {
                        String json = line.substring(6);
                        JSONObject event = new JSONObject(json);
                        if ("AUCTION_ENDED".equals(event.optString("type"))) {
                            int sessionId = event.getInt("sessionId");
                            Platform.runLater(() -> markCardAsEnded(sessionId));
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Home Socket listener error: {}", e.getMessage());
            }
        });
        homeSocketThread.setDaemon(true);
        homeSocketThread.start();
    }

    /**
     * Update Card UI when auction ends: gray out + disable button.
     */
    private void markCardAsEnded(int sessionId) {
        VBox card = sessionCardMap.get(sessionId);
        if (card != null) {
            // card.setOpacity(0.6);
            card.setStyle(" -fx-border-radius: 5px; -fx-padding: 10px; ");

            // Find Button in Card and update
            card.getChildren().stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button) node)
                    .findFirst()
                    .ifPresent(btn -> {
                        btn.setText("Time's Up");
                        btn.setDisable(true);
                        btn.setStyle(" -fx-text-fill: white;");
                    });
        }
    }

    @FXML
    public void handleGoToDashboard(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Error switching back to Seller Dashboard: ", e);
        }
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
            return Config.applyCacheBuster(path);
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

        String url = path.isBlank() ? "" : Config.API_URL + "/api/files/images/" + path;
        return Config.applyCacheBuster(url);
    }

    private String formatPrice(BigDecimal price) {
        if (price == null)
            return "0";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("###,###", symbols);
        return df.format(price);
    }

    private void stopCountdownTimeline() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void openAuctionPage(javafx.event.Event event, JSONObject sessionObj, JSONObject itemObj, String name,
            int id, BigDecimal currentPrice) {
        ClientLogger.logViewHistory(User.getUsername(), name, id, currentPrice);
        stopCountdownTimeline();
        try {
            FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 1280, 800);
            AuctionPageController controller = loader.getController();
            controller.setItem(sessionObj, itemObj);
        } catch (Exception ex) {
            logger.error("Cannot open Auction Page", ex);
        }
    }

    private void startCountdownTimeline() {
        stopCountdownTimeline();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (allProducts == null || allProducts.isEmpty())
                return;

            LocalDateTime now = LocalDateTime.now();
            boolean needRender = false;

            for (JSONObject sessionObj : allProducts) {
                int id = sessionObj.optInt("id");
                if (!currentRenderedIds.contains(id))
                    continue;

                JSONObject itemObj = getItemObject(sessionObj);
                String rawStatus = sessionObj.optString("status", "");
                String startTimeRaw = getRawTimeField(sessionObj, itemObj, "startTime", "start_time",
                        "auctionStartTime");
                String endTimeRaw = getRawTimeField(sessionObj, itemObj, "endTime", "end_time", "auctionEndTime",
                        "endDate", "endDateTime");

                LocalDateTime startDT = parseDateTime(startTimeRaw, id,
                        itemObj != null ? itemObj.optString("name") : "", rawStatus, "startTime");
                LocalDateTime endDT = parseDateTime(endTimeRaw, id, itemObj != null ? itemObj.optString("name") : "",
                        rawStatus, "endTime");
                String currentNormalized = normalizeStatus(rawStatus, startDT, endDT);

                if ("RUNNING".equals(currentNormalized)) {
                    if (endDT != null) {
                        if (now.isAfter(endDT) || now.isEqual(endDT)) {
                            // Ended during timeline
                            needRender = true;
                        } else {
                            // Update label
                            javafx.scene.Node node = productContainer.lookup("#timerLabel_" + id);
                            if (node instanceof Label) {
                                java.time.Duration dur = java.time.Duration.between(now, endDT);
                                long days = dur.toDays();
                                long hours = dur.toHoursPart();
                                long minutes = dur.toMinutesPart();
                                long seconds = dur.toSecondsPart();

                                String text;
                                if (days > 0) {
                                    text = "Ends in " + days + "d " + hours + "h";
                                } else if (hours > 0) {
                                    text = "Ends in " + hours + "h " + minutes + "m";
                                } else {
                                    text = "Ends in " + minutes + "m " + seconds + "s";
                                }
                                ((Label) node).setText(text);
                            }
                        }
                    }
                } else if ("UPCOMING".equals(currentNormalized)) {
                    if (startDT != null && (now.isAfter(startDT) || now.isEqual(startDT))) {
                        // Started during timeline
                        needRender = true;
                    }
                }
            }

            if (needRender) {
                // To avoid multiple consecutive renders, stop and call it once.
                stopCountdownTimeline();
                forceRenderProducts = true;
                filterAndRenderProducts();
            }
        }));

        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

}
