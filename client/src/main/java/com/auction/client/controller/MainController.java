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

public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    static {
        try {
            java.io.InputStream fontStream = MainController.class.getResourceAsStream("/com/auction/client/view/fonts/MaterialIcons-Regular.ttf");
            if (fontStream != null) {
                javafx.scene.text.Font.loadFont(fontStream, 20);
            }

            java.io.InputStream fontStream2 = MainController.class.getResourceAsStream("/com/auction/client/view/fonts/MaterialSymbolsOutlined.ttf");
            if (fontStream2 != null) {
                javafx.scene.text.Font.loadFont(fontStream2, 20);
            }

            java.io.InputStream fontStream3 = MainController.class.getResourceAsStream("/com/auction/client/view/fonts/DMSans-Variable.ttf");
            if (fontStream3 != null) {
                javafx.scene.text.Font.loadFont(fontStream3, 14);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HttpClient client = HttpClient.newHttpClient();

    @FXML private MenuButton userMenuButton;
    @FXML private ScrollPane scrollPane;
    @FXML private FlowPane productContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbStatus;

    @FXML private Button btnNotificationBell;
    @FXML private Label notificationBadge;
    @FXML private SidebarController sidebarController;

    @FXML private ScrollPane sidebarContainer;
    @FXML private VBox sidebarContent;
    @FXML private Button btnHamburger;
    @FXML private Button btnStartSelling;
    @FXML private Label lblPageTitle;
    @FXML private HBox filterControlsBox;
    @FXML private StackPane topBarAvatarPane;
    @FXML private VBox toastContainer;

    private boolean showingWatchlistOnly = false;
    private boolean showingMyBidsOnly = false;
    private boolean showingMySessionsOnly = false;
    private boolean forceRenderProducts = true;
    
    private Timeline countdownTimeline;
    private boolean showingAccountScreen = false;
    private boolean showingCompactListScreen = false;
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
        btnHamburger.setId("btnHamburger");

        fakeTestBtn.setId("btnSidebarCategories");
        fakeTestBtn.setVisible(false);
        fakeTestBtn.setManaged(false);

        // IMPORTANT
        fakeTestBtn.setOnAction(e -> {});

        productContainer.getChildren().add(fakeTestBtn);

        if (btnNotificationBell != null && notificationBadge != null) {
            NotificationBellBinder.bind(btnNotificationBell, notificationBadge);
        }

        // IMPORTANT
        btnHamburger.setOnAction(this::handleToggleSidebar);

        if (User.getFullname() != null) {
            createUserOption("Hello, " + User.getFullname());
        }



        // Initialize ComboBox
        cbCategory.getItems().addAll("All", "Electronics", "Art", "Vehicle");
        cbCategory.setValue("All");

        // Initialize display status on main market
        cbStatus.getItems().addAll("All", "Ongoing", "Starting Soon", "Ended");
        cbStatus.setValue("All");

        // Listen for real-time filter events
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterAndRenderProducts());
        cbCategory.setOnAction(event -> filterAndRenderProducts());
        cbStatus.setOnAction(event -> filterAndRenderProducts());

        loadProductsFromServer();
        connectHomeSocket();
        // Dynamic Space-Evenly algorithm for product list
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            updateGridLayout();
        });
        scheduleStableGridLayout();

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

            String requestedMode = initialShowWatchlist ? "WATCHLIST" : (initialShowAccount ? "ACCOUNT" : initialHomeFilterMode);
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

        // Load avatar into top bar if available from login session
        Platform.runLater(() -> updateTopBarAvatar(User.getAvatarUrl()));

        Platform.runLater(() -> updateTopBarAvatar(User.getAvatarUrl()));

        if (System.getProperty("surefire.test.class.path") == null) {
            startPolling();
        }
    }

    private void updateGridLayout() {
        if (scrollPane == null || productContainer == null) return;

        if (showingAccountScreen || showingCompactListScreen) {
            productContainer.setAlignment(Pos.TOP_CENTER);
            return;
        }

        // Stable layout: do not recalculate dynamic spacing for very small viewport changes.
        // JavaFX sometimes refreshes viewport on background click / app focus change, causing dynamic gap to toggle.
        // So we keep gap fixed and let FlowPane center the product row.
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        if (viewportWidth <= 0) return;

        double stableWidth = Math.max(0, Math.floor(viewportWidth) - 24.0);

        productContainer.setAlignment(Pos.TOP_LEFT);
        productContainer.setPrefWrapLength(stableWidth);
        productContainer.setMinWidth(stableWidth);
        productContainer.setPrefWidth(stableWidth);
        productContainer.setMaxWidth(stableWidth);
        productContainer.setHgap(44.0);
        productContainer.setVgap(28.0);
        productContainer.setPadding(new Insets(10.0, 18.0, 10.0, 18.0));
    }

    private void scheduleStableGridLayout() {
        Platform.runLater(this::updateGridLayout);
        PauseTransition delay = new PauseTransition(Duration.millis(150));
        delay.setOnFinished(event -> updateGridLayout());
        delay.play();
    }

    private void createUserOption(String text) {
        // userMenuButton.setText(text); // Hidden name to show only Avatar

        MenuItem accountItem = new MenuItem("My Account");
        accountItem.setId("menuAccount");
        MenuItem depositMoney = new MenuItem("Deposit");
        depositMoney.setId("menuDeposit");
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setId("menuLogout");

        accountItem.setOnAction(e -> showAccountScreen());
        depositMoney.setOnAction(e -> handleDepositMoney(e));
        logoutItem.setOnAction(e -> System.out.println("Performing logout..."));

        logoutItem.setOnAction(event -> {
            try {
                handleLogout(event); // Call existing logout handler
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error switching to Login screen!");
            }
        });

        userMenuButton.getItems().addAll(accountItem, depositMoney, new SeparatorMenuItem(), logoutItem);

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
                            if (auctionId == -1) continue;
                            if (lastSnapshot.containsKey(auctionId)) {
                                JSONObject oldObj = lastSnapshot.get(auctionId);
                                BigDecimal oldPrice = oldObj.optBigDecimal("currentPrice", BigDecimal.ZERO);
                                BigDecimal newPrice = newObj.optBigDecimal("currentPrice", BigDecimal.ZERO);
                                String oldStatus = oldObj.optString("status", "");
                                String newStatus = newObj.optString("status", "");

                                if (User.watchlistIds.contains(auctionId)) {
                                    String name = getItemObject(newObj).optString("name", "");
                                    if (newPrice.compareTo(oldPrice) > 0) {
                                        AppNotification notif = new AppNotification(NotificationType.NEW_BID, NotificationSeverity.INFO,
                                                "New bid", "Product " + name + " has a new bid: ₫ " + formatPrice(newPrice));
                                        notif.setAuctionId(auctionId);
                                        notif.setItemName(name);
                                        NotificationCenterService.getInstance().addNotification(notif);
                                    }
                                    if (!"ENDED".equalsIgnoreCase(oldStatus) && !oldStatus.equals(newStatus) && ("ENDED".equalsIgnoreCase(newStatus) || "FINISHED".equalsIgnoreCase(newStatus))) {
                                        AppNotification notif = new AppNotification(NotificationType.AUCTION_END_LOSE, NotificationSeverity.INFO,
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

                    Platform.runLater(this::filterAndRenderProducts);
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
        if (productContainer == null) return;
        if (!allProducts.isEmpty()) return; // If old data exists, keep display intact

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
        titleLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2e1a28;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-text-fill: #604868; -fx-wrap-text: true; -fx-text-alignment: center;");
        msgLabel.setMaxWidth(400);

        Button retryBtn = new Button("Retry Connection");
        retryBtn.setStyle("-fx-background-color: linear-gradient(to right, #e040a0, #f06292); -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-weight: bold; -fx-padding: 8 24; -fx-background-radius: 999; -fx-cursor: hand;");
        retryBtn.setOnAction(e -> {
            retryBtn.setText("Retrying...");
            retryBtn.setDisable(true);
            loadProductsFromServer();
        });

        offlineBox.getChildren().addAll(iconLabel, titleLabel, msgLabel, retryBtn);
        productContainer.getChildren().add(offlineBox);
    }

    /**
     * Core Data-Driven UI handler: Filter buffer (RAM) and redraw screen
     */
    private void filterAndRenderProducts() {
        if (showingAccountScreen || showingCompactListScreen) {
            return;
        }

        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        String selectedCategory = cbCategory.getValue();
        String selectedStatus = cbStatus.getValue();

        Platform.runLater(() -> {
            sortProducts(allProducts);

            List<Integer> newIdsToRender = new ArrayList<>();

            // Step 1: Calculate the list of IDs to display after filtering
            for (JSONObject sessionObj : allProducts) {
                JSONObject itemObj = getItemObject(sessionObj);

                String name = itemObj.optString("name", "");
                String type = itemObj.optString("type", "");
                String status = normalizeSession(sessionObj);

                // Only block canceled or paid sessions
                if ("CLOSED".equalsIgnoreCase(status)) {
                    continue;
                }

                // 3-layer filtering logic
                boolean matchKeyword = keyword.isEmpty() || name.toLowerCase().contains(keyword);
                boolean matchCategory = "All".equals(selectedCategory) || type.equalsIgnoreCase(selectedCategory);
                boolean matchWatchlist = !showingWatchlistOnly || User.watchlistIds.contains(sessionObj.optInt("id"));
                boolean matchMySessions = !showingMySessionsOnly || isSessionOwnedByCurrentUser(sessionObj);

                boolean matchStatus = false;
                if ("All".equals(selectedStatus) || selectedStatus == null) {
                    matchStatus = true;
                } else if ("Ongoing".equals(selectedStatus)) {
                    matchStatus = "RUNNING".equalsIgnoreCase(status);
                } else if ("Starting Soon".equals(selectedStatus)) {
                    matchStatus = "UPCOMING".equalsIgnoreCase(status);
                } else if ("Ended".equals(selectedStatus)) {
                    matchStatus = "ENDED".equalsIgnoreCase(status);
                }

                if (matchKeyword && matchCategory && matchStatus && matchWatchlist && matchMySessions) {
                    newIdsToRender.add(sessionObj.optInt("id"));
                }
            }

            // Step 2: Check if the display list has changed (add/remove/change filters)
            if (forceRenderProducts || !currentRenderedIds.equals(newIdsToRender)) {
                forceRenderProducts = false;
                // Changes detected => Redraw everything
                productContainer.getChildren().clear();
                currentRenderedIds.clear();

                productContainer.getChildren().add(fakeTestBtn);

                if (newIdsToRender.isEmpty()) {
                    productContainer.getChildren().add(createEmptyStateBox());
                    updateGridLayout();
                    return;
                }

                sessionCardMap.clear();

                for (JSONObject sessionObj : allProducts) {
                    JSONObject itemObj = getItemObject(sessionObj);

                    String name = itemObj.optString("name", "");
                    String type = itemObj.optString("type", "");
                    String status = normalizeSession(sessionObj);

                    if ("CLOSED".equalsIgnoreCase(status)) {
                        continue;
                    }

                    boolean matchKeyword = keyword.isEmpty() || name.toLowerCase().contains(keyword);
                    boolean matchCategory = "All".equals(selectedCategory) || type.equalsIgnoreCase(selectedCategory);
                    
                    boolean matchStatus = false;
                    if ("All".equals(selectedStatus) || selectedStatus == null) {
                        matchStatus = true;
                    } else if ("Ongoing".equals(selectedStatus)) {
                        matchStatus = "RUNNING".equalsIgnoreCase(status);
                    } else if ("Starting Soon".equals(selectedStatus)) {
                        matchStatus = "UPCOMING".equalsIgnoreCase(status);
                    } else if ("Ended".equals(selectedStatus)) {
                        matchStatus = "ENDED".equalsIgnoreCase(status);
                    }
                    
                    boolean matchWatchlist = !showingWatchlistOnly || User.watchlistIds.contains(sessionObj.optInt("id"));
                    boolean matchMySessions = !showingMySessionsOnly || isSessionOwnedByCurrentUser(sessionObj);

                    if (matchKeyword && matchCategory && matchStatus && matchWatchlist && matchMySessions) {
                        VBox card = createProductCard(sessionObj, itemObj);
                        productContainer.getChildren().add(card);
                        currentRenderedIds.add(sessionObj.optInt("id"));

                        sessionCardMap.put(sessionObj.optInt("id"), card);
                    }
                }
                updateGridLayout();
            } else {
                // Structure unchanged (polling only got new price) => Update Label in place to avoid UI jitter
                for (JSONObject sessionObj : allProducts) {
                    int id = sessionObj.optInt("id");
                    if (currentRenderedIds.contains(id)) {
                        BigDecimal currentPrice = sessionObj.optBigDecimal("currentPrice", BigDecimal.ZERO);
                        javafx.scene.Node priceNode = productContainer.lookup("#priceLabel_" + id);
                        if (priceNode instanceof Label) {
                            ((Label) priceNode).setText("₫ " + formatPrice(currentPrice));
                        }
                    }
                }
            }
            startCountdownTimeline();
        });
    }

    private String getRawTimeField(JSONObject sessionObj, JSONObject itemObj, String... keys) {
        if (sessionObj == null) return null;
        for (String key : keys) {
            if (sessionObj.has(key) && !sessionObj.isNull(key)) {
                return sessionObj.optString(key);
            }
        }
        String[] nestedKeys = {"auctionSession", "session", "auction"};
        for (String nKey : nestedKeys) {
            if (sessionObj.has(nKey) && !sessionObj.isNull(nKey)) {
                JSONObject nestedObj = sessionObj.optJSONObject(nKey);
                if (nestedObj != null) {
                    for (String key : keys) {
                        if (nestedObj.has(key) && !nestedObj.isNull(key)) {
                            return nestedObj.optString(key);
                        }
                    }
                }
            }
        }
        if (itemObj != null) {
            for (String key : keys) {
                if (itemObj.has(key) && !itemObj.isNull(key)) {
                    return itemObj.optString(key);
                }
            }
            for (String nKey : nestedKeys) {
                if (itemObj.has(nKey) && !itemObj.isNull(nKey)) {
                    JSONObject nestedObj = itemObj.optJSONObject(nKey);
                    if (nestedObj != null) {
                        for (String key : keys) {
                            if (nestedObj.has(key) && !nestedObj.isNull(key)) {
                                return nestedObj.optString(key);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(String rawVal, int id, String name, String rawStatus, String fieldName) {
        if (rawVal == null || rawVal.isBlank()) {
            return null;
        }
        rawVal = rawVal.trim();
        try {
            return LocalDateTime.parse(rawVal);
        } catch (Exception e) {
            try {
                if (rawVal.contains(" ") && rawVal.indexOf(" ") == 10) {
                    return LocalDateTime.parse(rawVal.replace(" ", "T"));
                }
            } catch (Exception ex) {}
        }
        
        try {
            long millis = Long.parseLong(rawVal);
            return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), java.time.ZoneId.systemDefault());
        } catch (NumberFormatException e) {
        }
        
        String[] formats = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm"
        };
        for (String format : formats) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                return LocalDateTime.parse(rawVal, formatter);
            } catch (Exception e) {
            }
        }
        
        logger.warn("parseDateTime failed for product id={}, name='{}', rawStatus='{}', field='{}', raw value='{}'",
                id, name, rawStatus, fieldName, rawVal);
        return null;
    }

    private String normalizeSession(JSONObject sessionObj) {
        if (sessionObj == null) return "UNKNOWN_TIME";
        JSONObject itemObj = getItemObject(sessionObj);
        int id = sessionObj.optInt("id");
        String name = itemObj.optString("name");
        String rawStatus = sessionObj.optString("status", "");
        
        String startTimeRaw = getRawTimeField(sessionObj, itemObj, "startTime", "start_time", "auctionStartTime");
        String endTimeRaw = getRawTimeField(sessionObj, itemObj, "endTime", "end_time", "auctionEndTime", "endDate", "endDateTime");
        
        LocalDateTime startDT = parseDateTime(startTimeRaw, id, name, rawStatus, "startTime");
        LocalDateTime endDT = parseDateTime(endTimeRaw, id, name, rawStatus, "endTime");
        
        return normalizeStatus(rawStatus, startDT, endDT);
    }

    private String normalizeStatus(String rawStatus, LocalDateTime startDT, LocalDateTime endDT) {
        if (rawStatus == null) rawStatus = "";
        rawStatus = rawStatus.toUpperCase().trim();

        if (rawStatus.equals("FINISHED") || rawStatus.equals("ENDED") || 
            rawStatus.equals("CANCELED") || rawStatus.equals("PAID") || 
            rawStatus.equals("CLOSED")) {
            return "ENDED";
        }

        if (startDT != null && endDT != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(startDT)) {
                return "UPCOMING";
            } else if (!now.isBefore(startDT) && now.isBefore(endDT)) {
                return "RUNNING";
            } else {
                return "ENDED";
            }
        }

        if ((rawStatus.equals("RUNNING") || rawStatus.equals("ACTIVE")) && endDT == null) {
            return "UNKNOWN_TIME";
        }

        if (rawStatus.equals("OPEN")) {
            LocalDateTime now = LocalDateTime.now();
            if (startDT != null && endDT != null) {
                if (!now.isBefore(startDT) && now.isBefore(endDT)) {
                    return "RUNNING";
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        if (endDT != null && (now.isAfter(endDT) || now.isEqual(endDT))) {
            return "ENDED";
        }
        if (startDT != null && now.isBefore(startDT)) {
            return "UPCOMING";
        }
        if (rawStatus.equals("RUNNING") || rawStatus.equals("ACTIVE") || rawStatus.equals("OPEN")) {
            if (endDT != null) {
                return "RUNNING";
            } else {
                return "UNKNOWN_TIME";
            }
        }

        return "UNKNOWN_TIME";
    }

    private void sortProducts(List<JSONObject> list) {
        list.sort((o1, o2) -> {
            String status1 = normalizeSession(o1);
            String status2 = normalizeSession(o2);

            int p1 = getStatusPriority(status1);
            int p2 = getStatusPriority(status2);
            if (p1 != p2) return Integer.compare(p1, p2);

            JSONObject item1 = getItemObject(o1);
            JSONObject item2 = getItemObject(o2);
            
            String st1Raw = getRawTimeField(o1, item1, "startTime", "start_time", "auctionStartTime");
            String et1Raw = getRawTimeField(o1, item1, "endTime", "end_time", "auctionEndTime", "endDate", "endDateTime");
            String st2Raw = getRawTimeField(o2, item2, "startTime", "start_time", "auctionStartTime");
            String et2Raw = getRawTimeField(o2, item2, "endTime", "end_time", "auctionEndTime", "endDate", "endDateTime");

            LocalDateTime st1 = parseDateTime(st1Raw, o1.optInt("id"), item1.optString("name"), o1.optString("status", ""), "startTime");
            LocalDateTime et1 = parseDateTime(et1Raw, o1.optInt("id"), item1.optString("name"), o1.optString("status", ""), "endTime");
            LocalDateTime st2 = parseDateTime(st2Raw, o2.optInt("id"), item2.optString("name"), o2.optString("status", ""), "startTime");
            LocalDateTime et2 = parseDateTime(et2Raw, o2.optInt("id"), item2.optString("name"), o2.optString("status", ""), "endTime");

            if (p1 == 1) { // RUNNING: sort by endTime asc
                if (et1 == null && et2 == null) return 0;
                if (et1 == null) return 1;
                if (et2 == null) return -1;
                return et1.compareTo(et2);
            } else if (p1 == 2) { // UPCOMING: sort by startTime asc
                if (st1 == null && st2 == null) return 0;
                if (st1 == null) return 1;
                if (st2 == null) return -1;
                return st1.compareTo(st2);
            } else if (p1 == 3) { // ENDED: sort by endTime desc
                if (et1 == null && et2 == null) return 0;
                if (et1 == null) return 1;
                if (et2 == null) return -1;
                return et2.compareTo(et1);
            }
            return 0;
        });
    }

    private int getStatusPriority(String status) {
        switch (status) {
            case "RUNNING": return 1;
            case "UPCOMING": return 2;
            case "ENDED": return 3;
            case "CLOSED": return 4;
            default: return 5;
        }
    }

    private void showCategoryChooser() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                cbCategory.getValue() == null ? "All" : cbCategory.getValue(),
                "All", "Electronics", "Art", "Vehicle"
        );
        dialog.setTitle("Category");
        dialog.setHeaderText("Select category to view");
        dialog.setContentText("Category:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        showAllSessions();
        cbCategory.setValue(result.get());
        filterAndRenderProducts();
    }

    @FXML
    private void handleApplyFilter(ActionEvent event) {
        filterAndRenderProducts();
    }

    @FXML
    private void handleResetFilter(ActionEvent event) {
        resetFiltersAndShowAll();
    }


    private void showAllSessions() {
        showingAccountScreen = false;
        showingCompactListScreen = false;
        showingWatchlistOnly = false;
        showingMyBidsOnly = false;
        showingMySessionsOnly = false;
        hideFilterControlsForAccountPage(false);
        forceRenderProducts = true;
        loadProductsFromServer();
        filterAndRenderProducts();
    }

    private void showWatchlistSessions() {
        showingAccountScreen = false;
        showingCompactListScreen = false;
        showingWatchlistOnly = true;
        showingMyBidsOnly = false;
        showingMySessionsOnly = false;
        hideFilterControlsForAccountPage(false);
        forceRenderProducts = true;
        filterAndRenderProducts();
    }

    private void showMySessions() {
        showingAccountScreen = false;
        showingCompactListScreen = false;
        hideFilterControlsForAccountPage(false);
        if (User.getId() == null) {
            showWarning("Login Required", "Please log in to view your auction sessions.");
            return;
        }
        showingWatchlistOnly = false;
        showingMyBidsOnly = false;
        showingMySessionsOnly = true;
        forceRenderProducts = true;
        loadProductsFromServer();
        filterAndRenderProducts();
    }

    private void showMyBiddingSessions() {
        showingAccountScreen = false;
        showingCompactListScreen = false;
        hideFilterControlsForAccountPage(false);
        if (User.getId() == null) {
            showWarning("Login Required", "Please log in to view your active bids.");
            return;
        }

        showingWatchlistOnly = false;
        showingMyBidsOnly = true;
        showingMySessionsOnly = false;
        forceRenderProducts = true;
        loadMyBiddingSessionsFromServer();
    }

    private void loadMyBiddingSessionsFromServer() {
        Integer bidderId = User.getId();
        if (bidderId == null) return;

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/bidder/my-bidding-sessions?bidderId=" + bidderId))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    Platform.runLater(() -> showError("Cannot Load My Bids", "Server responded with error code: " + response.statusCode()));
                    return;
                }

                JSONObject responseJson = new JSONObject(response.body());
                if (responseJson.optInt("status", 500) != 200) {
                    Platform.runLater(() -> showError("Cannot Load My Bids", responseJson.optString("message", "Unknown error.")));
                    return;
                }

                List<JSONObject> sessions = parseSessionList(responseJson.get("data"));
                Platform.runLater(() -> {
                    allProducts.clear();
                    allProducts.addAll(sessions);
                    forceRenderProducts = true;
                    filterAndRenderProducts();
                });
            } catch (Exception e) {
                logger.error("Error loading bids for user {}: {}", bidderId, e.getMessage(), e);
                Platform.runLater(() -> showError("Cannot Load My Bids", "Cannot connect to server or invalid data returned."));
            }
        }, "load-my-bidding-sessions").start();
    }

    private List<JSONObject> parseSessionList(Object dataObj) {
        JSONArray jsonArray = new JSONArray();
        if (dataObj instanceof JSONObject) {
            JSONObject dataJson = (JSONObject) dataObj;
            if (dataJson.has("content")) {
                jsonArray = dataJson.getJSONArray("content");
            }
        } else if (dataObj instanceof JSONArray) {
            jsonArray = (JSONArray) dataObj;
        }

        List<JSONObject> sessions = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            sessions.add(jsonArray.getJSONObject(i));
        }
        return sessions;
    }

    private boolean isSessionOwnedByCurrentUser(JSONObject sessionObj) {
        Integer currentUserId = User.getId();
        if (currentUserId == null) return false;
        return getSellerId(sessionObj) == currentUserId;
    }

    private int getSellerId(JSONObject sessionObj) {
        if (sessionObj == null) return -1;
        if (sessionObj.has("sellerId") && !sessionObj.isNull("sellerId")) {
            return sessionObj.optInt("sellerId", -1);
        }
        JSONObject sellerObj = sessionObj.optJSONObject("seller");
        if (sellerObj != null) {
            return sellerObj.optInt("id", -1);
        }
        return -1;
    }

    private VBox createEmptyStateBox() {
        VBox emptyBox = new VBox(10);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(50));
        emptyBox.setPrefWidth(600);

        Label iconLabel = new Label("\uE88B");
        iconLabel.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 56px; -fx-text-fill: #c8b6cf;");

        Label titleLabel = new Label(getEmptyStateTitle());
        titleLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2e1a28;");

        Label msgLabel = new Label(getEmptyStateMessage());
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(480);
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-text-fill: #604868; -fx-text-alignment: center;");

        emptyBox.getChildren().addAll(iconLabel, titleLabel, msgLabel);
        return emptyBox;
    }

    private String getEmptyStateTitle() {
        if (showingMyBidsOnly) return "No active bids";
        if (showingMySessionsOnly) return "No sessions created by you";
        if (showingWatchlistOnly) return "Watchlist is empty";
        return "No matching sessions found";
    }

    private String getEmptyStateMessage() {
        if (showingMyBidsOnly) return "Sessions you bid on will appear here.";
        if (showingMySessionsOnly) return "Auctions created by you will appear here.";
        if (showingWatchlistOnly) return "Click the heart icon on an auction to add it to your Watchlist.";
        return "Try changing search keywords, category or status filters.";
    }

    private VBox createProductCard(JSONObject sessionObj, JSONObject itemObj) {
        int id = sessionObj.getInt("id");

        String type = itemObj.optString("type", "");
        String name = itemObj.optString("name", "");
        BigDecimal currentPrice = getMoney(sessionObj, "currentPrice", getMoney(sessionObj, "startingPrice", BigDecimal.ZERO));

        String rawStatus = sessionObj.optString("status", "");
        String startTimeRaw = getRawTimeField(sessionObj, itemObj, "startTime", "start_time", "auctionStartTime");
        String endTimeRaw = getRawTimeField(sessionObj, itemObj, "endTime", "end_time", "auctionEndTime", "endDate", "endDateTime");
        LocalDateTime startDT = parseDateTime(startTimeRaw, id, name, rawStatus, "startTime");
        LocalDateTime endDT = parseDateTime(endTimeRaw, id, name, rawStatus, "endTime");
        String normalizedStatus = normalizeStatus(rawStatus, startDT, endDT);

        boolean bidEnabled = "RUNNING".equals(normalizedStatus) && endDT != null;

        logger.info("ProductCard id={}, name={}, rawStatus={}, startTimeRaw={}, endTimeRaw={}, normalizedStatus={}, bidEnabled={}",
                id, name, rawStatus, startTimeRaw, endTimeRaw, normalizedStatus, bidEnabled);

        String imagePath = itemObj.optString("imagePath", "default.png");

        VBox vbox = new VBox();
        vbox.setSpacing(4.0);
        vbox.setPrefWidth(240.0);
        vbox.setMinWidth(240.0);
        vbox.setMaxWidth(240.0);
        vbox.setPrefHeight(360.0);
        vbox.setMinHeight(360.0);
        vbox.setStyle("-fx-border-color: #ffe8e8; -fx-border-width: 2px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 16px; -fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.05), 10, 0, 0, 2);");

        StackPane imageWrapper = new StackPane();
        imageWrapper.setPrefHeight(192.0);
        imageWrapper.setStyle("-fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: #f2e8f2; -fx-border-width: 1px;");

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
            timerBadge.setStyle("-fx-background-color: rgba(96, 72, 104, 0.9); -fx-background-radius: 15px; -fx-padding: 4px 8px;");
            timerIcon.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px;");
            timerText.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: #ffffff; -fx-text-alignment: center;");
            
            String displayStart = "Opening Soon";
            if (startDT != null) {
                LocalDateTime now = LocalDateTime.now();
                if (startDT.toLocalDate().equals(now.toLocalDate())) {
                    displayStart = "Opening Soon\nStarts: " + startDT.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                } else {
                    displayStart = "Opening Soon\nStarts: " + startDT.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"));
                }
            }
            timerText.setText(displayStart);

            timerBadge.getChildren().addAll(timerIcon, timerText);
            StackPane.setAlignment(timerBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(timerBadge, new Insets(8, 8, 0, 0));
            imageWrapper.getChildren().add(timerBadge);
        } else if ("RUNNING".equals(normalizedStatus)) {
            timerBadge.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 15px; -fx-padding: 4px 8px;");
            timerIcon.setStyle("-fx-text-fill: #e040a0; -fx-font-size: 14px;");
            timerText.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: #e040a0;");
            
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
            timerBadge.setStyle("-fx-background-color: rgba(100, 100, 100, 0.8); -fx-background-radius: 15px; -fx-padding: 4px 8px;");
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
            timerBadge.setStyle("-fx-background-color: rgba(220, 53, 69, 0.9); -fx-background-radius: 15px; -fx-padding: 4px 8px;");
            timerIcon.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px;");
            timerText.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: #ffffff;");
            timerText.setText("Unknown time");
            
            timerBadge.getChildren().addAll(timerIcon, timerText);
            StackPane.setAlignment(timerBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(timerBadge, new Insets(8, 8, 0, 0));
            imageWrapper.getChildren().add(timerBadge);
        }

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 16px; -fx-text-fill: #2e1a28;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxHeight(24.0);
        VBox.setMargin(nameLabel, new Insets(8, 0, 0, 0));

        Label categoryLabel = new Label(type);
        categoryLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #604868;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        VBox priceBox = new VBox(0);
        Label lblCurrentBid = new Label("CURRENT BID");
        lblCurrentBid.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #907898;");
        Label priceLabel = new Label("₫ " + formatPrice(currentPrice));
        priceLabel.setId("priceLabel_" + id); // Set ID for fast updating
        priceLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 18px; -fx-text-fill: #e040a0;");
        priceBox.getChildren().addAll(lblCurrentBid, priceLabel);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
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
        mainBtn.setGraphic(mainPlusIcon);
        mainBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        mainBtn.setMinSize(44.0, 44.0);
        mainBtn.setPrefSize(44.0, 44.0);
        mainBtn.setMaxSize(44.0, 44.0);
        mainBtn.setPadding(Insets.EMPTY);
        mainBtn.setAlignment(Pos.CENTER);
        mainBtn.setStyle("-fx-background-color: #e040a0; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand;");
        Tooltip.install(mainBtn, new Tooltip("Options"));

        Button btnWatch = new Button();
        Label watchIcon = new Label(User.watchlistIds.contains(id) ? "\uE87D" : "\uE87E"); // heart filled or outline
        watchIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-text-fill: " + (User.watchlistIds.contains(id) ? "#e040a0" : "#604868") + ";");
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
        btnWatch.setStyle("-fx-background-color: #f2e8f2; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand;");
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
        btnBid.setStyle("-fx-background-color: #e040a0; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(224,64,160,0.3), 8, 0, 0, 2);");
        Tooltip.install(btnBid, new Tooltip("Bid Now"));

        btnWatch.setVisible(false); btnWatch.setManaged(false);
        btnBid.setVisible(false); btnBid.setManaged(false);

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
                    watchIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-text-fill: #604868;");
                    watchIcon.setTranslateY(1.5);
                    Tooltip.install(btnWatch, new Tooltip("Add to favorites"));
                    ClientLogger.logFavorite(User.getUsername(), name, id, false);
                } else {
                    User.watchlistIds.add(id);
                    watchIcon.setText("\uE87D");
                    watchIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-text-fill: #e040a0;");
                    watchIcon.setTranslateY(1.5);
                    Tooltip.install(btnWatch, new Tooltip("Favorited"));
                    ClientLogger.logFavorite(User.getUsername(), name, id, true);
                }
                if (showingWatchlistOnly) {
                    filterAndRenderProducts();
                }
            });

            actionBox.setOnMouseEntered(e -> {
                mainBtn.setVisible(false); mainBtn.setManaged(false);
                btnWatch.setVisible(true); btnWatch.setManaged(true);
                btnBid.setVisible(true); btnBid.setManaged(true);
            });
            actionBox.setOnMouseExited(e -> {
                btnWatch.setVisible(false); btnWatch.setManaged(false);
                btnBid.setVisible(false); btnBid.setManaged(false);
                mainBtn.setVisible(true); mainBtn.setManaged(true);
            });

            mainBtn.setOnAction(e -> {
                e.consume();
                mainBtn.setVisible(false); mainBtn.setManaged(false);
                btnWatch.setVisible(true); btnWatch.setManaged(true);
                btnBid.setVisible(true); btnBid.setManaged(true);
            });

            actionBox.getChildren().addAll(btnWatch, btnBid, mainBtn);
        } else {
            // Non-running / invalid cards show a single action button
            mainPlusIcon.setText("\uE8F4"); // Eye icon
            mainPlusIcon.setFont(Font.font("Material Symbols Outlined", FontWeight.NORMAL, 24));
            
            if ("UPCOMING".equals(normalizedStatus)) {
                mainPlusIcon.setTextFill(Color.web("#ffffff"));
                mainBtn.setStyle("-fx-background-color: #e040a0; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand;");
                Tooltip.install(mainBtn, new Tooltip("View details"));
                mainBtn.setDisable(false);
                mainBtn.setOnAction(e -> {
                    e.consume();
                    openAuctionPage(e, sessionObj, itemObj, name, id, currentPrice);
                });
            } else if ("ENDED".equals(normalizedStatus)) {
                mainPlusIcon.setTextFill(Color.web("#ffffff"));
                mainBtn.setStyle("-fx-background-color: #e040a0; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand;");
                Tooltip.install(mainBtn, new Tooltip("View results"));
                mainBtn.setDisable(false);
                mainBtn.setOnAction(e -> {
                    e.consume();
                    openAuctionPage(e, sessionObj, itemObj, name, id, currentPrice);
                });
            } else {
                // UNKNOWN_TIME
                mainPlusIcon.setTextFill(Color.web("#888888"));
                mainBtn.setStyle("-fx-background-color: #cccccc; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: default;");
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
        showCompactAuctionList();
    }

    private void showAccountScreen() {
        if (User.getId() == null) {
            showWarning("Login Required", "Please log in to view and edit account information.");
            return;
        }

        showingAccountScreen = true;
        showingCompactListScreen = false;
        renderAccountScreen(false);
        loadLatestAccountProfileForScreen();
    }

    @FXML
    public void handleResetDashboard(ActionEvent event) {
        logger.info("Resetting dashboard filters and search text...");
        if (txtSearch != null) {
            txtSearch.clear();
        }
        if (cbCategory != null) {
            cbCategory.setValue("All");
        }
        if (cbStatus != null) {
            cbStatus.setValue("All");
        }
        filterAndRenderProducts();
    }

    @FXML
    public void handleToggleSidebar(ActionEvent event) {
        if (sidebarController != null) {
            sidebarController.toggleSidebar();
            Platform.runLater(this::updateGridLayout);
        }
    }

    private void renderAccountScreen(boolean saving) {
        stopCountdownTimeline();
        productContainer.getChildren().clear();
        productContainer.getChildren().add(fakeTestBtn);
        currentRenderedIds.clear();
        productContainer.setAlignment(Pos.TOP_CENTER);

        // Selective hiding: only hide page title and filter controls
        hideFilterControlsForAccountPage(true);

        VBox wrapper = new VBox(22);
        wrapper.getStyleClass().add("account-page-wrapper");
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(1250);

        VBox headerSection = buildAccountHeader();
        HBox topSection = buildAccountTopSection(saving);
        VBox formCard = buildPersonalInfoForm(saving);

        wrapper.getChildren().addAll(headerSection, topSection, formCard);
        productContainer.getChildren().add(wrapper);

        // Responsive layout listener
        Platform.runLater(() -> {
            if (scrollPane.getScene() != null) {
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
        VBox headerBox = new VBox(4);
        headerBox.setMaxWidth(1250);
        headerBox.setAlignment(Pos.TOP_LEFT);

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);

        Button backButton = new Button("← Back");
        backButton.getStyleClass().add("account-back-btn");
        backButton.setOnAction(e -> returnToAuctionGrid());

        VBox titleBox = new VBox(2);
        Label title = new Label("My Account");
        title.getStyleClass().add("account-page-title");
        Label subtitle = new Label("Manage profile, avatar, and personal information.");
        subtitle.getStyleClass().add("account-page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        row.getChildren().addAll(backButton, titleBox);
        headerBox.getChildren().add(row);
        return headerBox;
    }

    private HBox buildAccountTopSection(boolean saving) {
        HBox topSection = new HBox(22);
        topSection.setMaxWidth(1250);
        topSection.setAlignment(Pos.TOP_CENTER);

        VBox profileCard = buildProfileSummaryCard();
        HBox.setHgrow(profileCard, Priority.ALWAYS);

        VBox statsColumn = buildAccountStats();
        HBox.setHgrow(statsColumn, Priority.ALWAYS);

        topSection.getChildren().addAll(profileCard, statsColumn);
        return topSection;
    }

    private VBox buildProfileSummaryCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("profile-summary-card");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(280);

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
        placeholder.setStyle("-fx-background-color: linear-gradient(to bottom right, #e040a0, #c83090);"
                + " -fx-background-radius: " + (size / 2) + ";");

        String initials = getInitials(User.getFullname());
        Label initialsLabel = new Label(initials);
        initialsLabel.getStyleClass().add("profile-avatar-initials");
        placeholder.getChildren().add(initialsLabel);
        return placeholder;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    }

    private void handleAvatarUpload(Button btnChangeAvatar) {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Avatar Image");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.webp"));

        javafx.stage.Window window = scrollPane.getScene().getWindow();
        java.io.File file = chooser.showOpenDialog(window);
        if (file == null) return;

        // Client-side size validation
        if (file.length() > 5 * 1024 * 1024) {
            showWarning("File Too Large", "Avatar image must not exceed 5MB.");
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
                if (lName.endsWith(".png")) contentType = "image/png";
                else if (lName.endsWith(".jpg") || lName.endsWith(".jpeg")) contentType = "image/jpeg";
                else if (lName.endsWith(".webp")) contentType = "image/webp";

                // Build multipart body
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                bos.write((twoHyphens + boundary + lineEnd).getBytes());
                bos.write(("Content-Disposition: form-data; name=\"avatar\"; filename=\"" + fileName + "\"" + lineEnd).getBytes());
                bos.write(("Content-Type: " + contentType + lineEnd).getBytes());
                bos.write(lineEnd.getBytes());
                bos.write(fileBytes);
                bos.write(lineEnd.getBytes());
                bos.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/users/" + User.getId() + "/avatar"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(bos.toByteArray()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject responseJson = new JSONObject(response.body());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200 && responseJson.optInt("status", 500) == 200) {
                        JSONObject data = responseJson.optJSONObject("data");
                        String newAvatarUrl = data != null ? data.optString("avatarUrl", null) : null;
                        if (newAvatarUrl != null) {
                            User.setAvatarUrl(newAvatarUrl);
                            updateTopBarAvatar(newAvatarUrl);
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

    private void updateTopBarAvatar(String avatarUrl) {
        if (topBarAvatarPane == null) return;
        try {
            topBarAvatarPane.getChildren().clear();
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl
                        : Config.API_URL + avatarUrl;
                ImageView imgView = new ImageView(new Image(fullUrl, 36, 36, false, true, true));
                imgView.setFitWidth(36);
                imgView.setFitHeight(36);
                imgView.setSmooth(true);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                imgView.setClip(clip);
                topBarAvatarPane.getChildren().add(imgView);
            } else {
                Label icon = new Label("\uE7FD");
                icon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: white;");
                topBarAvatarPane.getChildren().add(icon);
            }
        } catch (Exception e) {
            logger.warn("Cannot update avatar on top bar: {}", e.getMessage());
        }
    }

    private VBox buildAccountStats() {
        VBox statsColumn = new VBox(14);
        statsColumn.setAlignment(Pos.TOP_LEFT);
        statsColumn.setMinWidth(200);

        statsColumn.getChildren().addAll(
                createProfileStatCard("Account Balance", "₫ " + formatPrice(User.getBalance()), "\uE227"),
                createProfileStatCard("Role", safeText(User.getRole(), "Unknown"), "\uE7FD"),
                createProfileStatCard("User ID", String.valueOf(User.getId()), "\uE838")
        );
        return statsColumn;
    }

    private VBox createProfileStatCard(String title, String value, String iconCode) {
        VBox box = new VBox(6);
        box.getStyleClass().add("account-stat-card");
        box.setAlignment(Pos.CENTER_LEFT);

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
        card.setMaxWidth(1250);

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
        col0.setMinWidth(100);
        col0.setPrefWidth(130);
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col1.setMinWidth(200);
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
                showWarning("Username Required", "Username cannot be empty.");
                return;
            }
            if (fullname.isBlank()) {
                showWarning("Full Name Required", "Full Name cannot be empty.");
                return;
            }
            if (email.isBlank() || !email.contains("@")) {
                showWarning("Invalid Email", "Please enter a valid email.");
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
        if (topSection == null) return;
        // Make use of horizontal space better
        if (width < 900) {
            topSection.setSpacing(14);
            if (topSection.getChildren().size() == 2) {
                topSection.setPrefWidth(Math.min(width - 40, 850));
            }
        } else {
            topSection.setSpacing(22);
            topSection.setPrefWidth(1250);
            topSection.setMaxWidth(1250);
        }
    }

    private void loadLatestAccountProfileForScreen() {
        if (User.getId() == null) return;

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/users/" + User.getId()))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject responseJson = new JSONObject(response.body());
                if (response.statusCode() == 200 && responseJson.optInt("status", 500) == 200) {
                    JSONObject data = responseJson.optJSONObject("data");
                    if (data != null) {
                        applyUserProfileFromJson(data);
                        Platform.runLater(() -> {
                            if (showingAccountScreen) {
                                renderAccountScreen(false);
                                updateTopBarAvatar(User.getAvatarUrl());
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
        if ("null".equals(avatarUrl)) avatarUrl = null;

        User.updateProfile(
                data.optString("username", safeText(User.getUsername(), "")),
                data.optString("fullname", safeText(User.getFullname(), "")),
                data.optString("email", safeText(User.getEmail(), "")),
                data.optString("dob", safeText(User.getDob(), "")),
                data.optString("placeOfBirth", data.optString("place_of_birth", safeText(User.getPlace_of_birth(), ""))),
                parseMoney(data.opt("balance"), User.getBalance()),
                avatarUrl
        );
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
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/users/" + User.getId() + "/profile"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

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
                    showError("Update Failed", "Cannot connect to server or invalid data returned.");
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

    private void showCompactAuctionList() {
        stopCountdownTimeline();
        showingCompactListScreen = true;
        showingAccountScreen = false;

        List<JSONObject> sessionsToShow = getCurrentlyDisplayedSessions();

        productContainer.getChildren().clear();
        productContainer.getChildren().add(fakeTestBtn);
        productContainer.setAlignment(Pos.TOP_CENTER);

        VBox wrapper = new VBox(16);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(22, 24, 40, 24));
        wrapper.setPrefWidth(Math.max(760, productContainer.getPrefWidth() > 0 ? productContainer.getPrefWidth() - 80 : 900));

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(900);

        VBox titleBox = new VBox(2);
        Label title = new Label("Compact Session List");
        title.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;");
        Label subtitle = new Label("Sessions currently displayed based on filters.");
        subtitle.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-text-fill: #907898;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button("Back to Grid");
        backButton.setStyle("-fx-background-color: #e040a0; -fx-background-radius: 999; -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-weight: bold; -fx-padding: 9 22 9 22; -fx-cursor: hand;");
        backButton.setOnAction(e -> returnToAuctionGrid());
        header.getChildren().addAll(titleBox, spacer, backButton);

        VBox listBox = new VBox(10);
        listBox.setMaxWidth(900);

        if (sessionsToShow.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            empty.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 22px; -fx-border-color: #ffe8e8; -fx-border-radius: 22px; -fx-border-width: 2px;");
            Label emptyTitle = new Label("No sessions found with current filters");
            emptyTitle.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;");
            Label emptyMsg = new Label("Try changing filters or return to grid view.");
            emptyMsg.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-text-fill: #907898;");
            empty.getChildren().addAll(emptyTitle, emptyMsg);
            listBox.getChildren().add(empty);
        } else {
            int index = 1;
            for (JSONObject sessionObj : sessionsToShow) {
                listBox.getChildren().add(createCompactAuctionRow(index++, sessionObj));
            }
        }

        ScrollPane listScroll = new ScrollPane(listBox);
        listScroll.setFitToWidth(true);
        listScroll.setMaxWidth(920);
        listScroll.setPrefHeight(460);
        listScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        wrapper.getChildren().addAll(header, listScroll);
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
        BigDecimal currentPrice = getMoney(sessionObj, "currentPrice", getMoney(sessionObj, "startingPrice", BigDecimal.ZERO));
        String status = sessionObj.optString("status", "UNKNOWN");

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 18px; -fx-border-color: #ffe8e8; -fx-border-width: 1.5px; -fx-border-radius: 18px;");

        Label order = new Label(String.valueOf(index));
        order.setAlignment(Pos.CENTER);
        order.setMinSize(34, 34);
        order.setPrefSize(34, 34);
        order.setStyle("-fx-background-color: #ffd6ee; -fx-background-radius: 17px; -fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: #e040a0;");

        VBox infoBox = new VBox(3);
        Label name = new Label("#" + sessionObj.optInt("id") + " · " + itemObj.optString("name", "Unknown"));
        name.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;");
        Label type = new Label(itemObj.optString("type", "Unknown category"));
        type.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 12px; -fx-text-fill: #907898;");
        infoBox.getChildren().addAll(name, type);

        Region rowSpacer = new Region();
        HBox.setHgrow(rowSpacer, Priority.ALWAYS);

        Label statusBadge = new Label(status);
        statusBadge.setStyle("-fx-background-color: #f2e8f2; -fx-background-radius: 999; -fx-padding: 5 12 5 12; -fx-font-family: 'DM Sans'; -fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #604868;");

        Label price = new Label("₫ " + formatPrice(currentPrice));
        price.setMinWidth(110);
        price.setAlignment(Pos.CENTER_RIGHT);
        price.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #e040a0;");

        row.getChildren().addAll(order, infoBox, rowSpacer, statusBadge, price);
        return row;
    }

    private void returnToAuctionGrid() {
        showingAccountScreen = false;
        showingCompactListScreen = false;
        forceRenderProducts = true;
        filterAndRenderProducts();
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
            if (type == ToastType.ERROR) alertType = Alert.AlertType.ERROR;
            if (type == ToastType.WARNING) alertType = Alert.AlertType.WARNING;
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
            case SUCCESS: icon.setText("\ue86c"); break; // check_circle
            case ERROR: icon.setText("\ue000"); break; // error
            case WARNING: icon.setText("\ue002"); break; // warning
            case INFO: icon.setText("\ue88e"); break; // info
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

        toast.setOpacity(0);
        toast.setTranslateY(20);
        toastContainer.getChildren().add(toast);

        javafx.animation.ParallelTransition fadeIn = new javafx.animation.ParallelTransition();
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toast);
        fade.setFromValue(0);
        fade.setToValue(1);
        javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(300), toast);
        slide.setFromY(20);
        slide.setToY(0);
        fadeIn.getChildren().addAll(fade, slide);
        fadeIn.play();

        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3.5));
        delay.setOnFinished(e -> {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toast);
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
        SceneSwitcher.switchScene(event, "Login.fxml", 400, 500);
    }

    public void setHttpClient(HttpClient httpClient) {
        this.client = httpClient;
    }

    /**
     * Connect Socket to receive real-time events from server (e.g. AUCTION_ENDED)
     * Reuse Socket infrastructure, send JOIN_HOME command to register for global events.
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
            card.setOpacity(0.6);
            card.setStyle("-fx-border-color: #dee2e6; -fx-border-radius: 5px; -fx-padding: 10px; -fx-background-color: #f4f4f4;");

            // Find Button in Card and update
            card.getChildren().stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button) node)
                    .findFirst()
                    .ifPresent(btn -> {
                        btn.setText("Time's Up");
                        btn.setDisable(true);
                        btn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white;");
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
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
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
        if (price == null) return "0";
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
    
    private void openAuctionPage(javafx.event.Event event, JSONObject sessionObj, JSONObject itemObj, String name, int id, BigDecimal currentPrice) {
        ClientLogger.logViewHistory(User.getUsername(), name, id, currentPrice);
        stopCountdownTimeline();
        try {
            FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 1280, 800);
            AuctionPageController controller = loader.getController();
            controller.setItem(sessionObj, itemObj);
        } catch (IOException ex) {
            logger.error("Cannot open Auction Page", ex);
        }
    }

    private void startCountdownTimeline() {
        stopCountdownTimeline();
        
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (allProducts == null || allProducts.isEmpty()) return;
            
            LocalDateTime now = LocalDateTime.now();
            boolean needRender = false;
            
            for (JSONObject sessionObj : allProducts) {
                int id = sessionObj.optInt("id");
                if (!currentRenderedIds.contains(id)) continue;
                
                JSONObject itemObj = getItemObject(sessionObj);
                String rawStatus = sessionObj.optString("status", "");
                String startTimeRaw = getRawTimeField(sessionObj, itemObj, "startTime", "start_time", "auctionStartTime");
                String endTimeRaw = getRawTimeField(sessionObj, itemObj, "endTime", "end_time", "auctionEndTime", "endDate", "endDateTime");
                
                LocalDateTime startDT = parseDateTime(startTimeRaw, id, itemObj != null ? itemObj.optString("name") : "", rawStatus, "startTime");
                LocalDateTime endDT = parseDateTime(endTimeRaw, id, itemObj != null ? itemObj.optString("name") : "", rawStatus, "endTime");
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
