package com.auction.client.controller;



import javafx.scene.control.*;

import javafx.scene.Cursor;

import javafx.fxml.FXMLLoader;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import com.auction.client.Config;

import com.auction.client.HttpClientSingleton;

import javafx.application.Platform;

import javafx.animation.PauseTransition;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;

import javafx.fxml.Initializable;

import com.auction.client.util.NotificationBellBinder;

import com.auction.client.model.notification.AppNotification;

import com.auction.client.model.notification.NotificationType;

import com.auction.client.model.notification.NotificationSeverity;

import com.auction.client.service.NotificationCenterService;

import javafx.geometry.Pos;

import javafx.geometry.Bounds;

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
import javafx.scene.control.Button;



public class MainController implements Initializable {
    @FXML
    private Button btnToggleProductView;

    private boolean compactProductListMode = false;


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



    private boolean isSidebarCollapsed = false;

    private boolean showingWatchlistOnly = false;

    private boolean showingMyBidsOnly = false;

    private boolean showingMySessionsOnly = false;

    private boolean showingAccountScreen = false;

    private boolean showingCompactListScreen = false;

    private boolean forceRenderProducts = false;

    public static boolean initialShowWatchlist = false;

    public static boolean initialShowAccount = false;

    public static String initialHomeFilterMode = "ALL";

    private final Button fakeTestBtn = new Button();



    // Kho lưu trữ Caching cục bộ, giúp Real-time filter không bị trễ

    private final List<JSONObject> allProducts = new ArrayList<>();



    // Map lưu tham chiếu Card theo sessionId - lookup O(1) cho real-time update

    private final Map<Integer, VBox> sessionCardMap = new HashMap<>();

    // Cache ảnh để tránh tải lại mỗi lần render

    private final Map<String, Image> imageCache = new ConcurrentHashMap<>();



    // Executor cho Polling

    private ScheduledExecutorService pollingScheduler;

    private final List<Integer> currentRenderedIds = new ArrayList<>();

    private final Map<Integer, JSONObject> lastSnapshot = new ConcurrentHashMap<>();



    private final Map<Button, String> sidebarButtonTextMap = new java.util.HashMap<>();



    @Override

    public void initialize(URL location, ResourceBundle resources) {

        btnHamburger.setId("btnHamburger");



        Button fakeBtn = new Button();

        fakeTestBtn.setId("btnSidebarCategories");

        fakeTestBtn.setVisible(false);

        fakeTestBtn.setManaged(false);



        // QUAN TRỌNG

        fakeTestBtn.setOnAction(e -> {});



        productContainer.getChildren().add(fakeTestBtn);



        if (btnNotificationBell != null && notificationBadge != null) {

            NotificationBellBinder.bind(btnNotificationBell, notificationBadge);

        }



        // QUAN TRỌNG

        btnHamburger.setOnAction(this::handleToggleSidebar);



        if (User.getFullname() != null) {

            createUserOption("Chào, " + User.getFullname());

        }






        // Khởi tạo ComboBox

        cbCategory.getItems().addAll("Tất cả", "Electronics", "Art", "Vehicle");

        cbCategory.setValue("Tất cả");



        // Khởi tạo các trạng thái hiển thị trên sàn chính, bao gồm cả COMING SOON

        cbStatus.getItems().addAll("Tất cả", "ACTIVE", "COMING SOON", "ENDED");

        cbStatus.setValue("Tất cả");



        // Lắng nghe sự kiện để lọc Real-time

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterAndRenderProducts());

        cbCategory.setOnAction(event -> filterAndRenderProducts());

        cbStatus.setOnAction(event -> filterAndRenderProducts());



        loadProductsFromServer();

        connectHomeSocket();

        // Thuật toán Space-Evenly động cho danh sách sản phẩm

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



//         if (System.getProperty("surefire.test.class.path") == null) {

//             startPolling();

//         }

    }private void updateGridLayout() {
        if (scrollPane == null || productContainer == null) return;

        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        productContainer.setAlignment(Pos.TOP_LEFT);
        productContainer.setHgap(44.0);
        productContainer.setVgap(36.0);
        productContainer.setPadding(new Insets(10.0, 18.0, 32.0, 18.0));

        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        if (viewportWidth <= 0 && scrollPane.getWidth() > 0) {
            viewportWidth = scrollPane.getWidth();
        }
        if (viewportWidth <= 0) return;

        double stableWidth = Math.max(0, Math.floor(viewportWidth) - 36.0);
        productContainer.setPrefWrapLength(stableWidth);
        productContainer.setMinWidth(stableWidth);
        productContainer.setPrefWidth(stableWidth);
        productContainer.setMaxWidth(stableWidth);
    }





    private void scheduleStableGridLayout() {

        Platform.runLater(this::updateGridLayout);

        PauseTransition delay = new PauseTransition(Duration.millis(150));

        delay.setOnFinished(event -> updateGridLayout());

        delay.play();

    }



    private void createUserOption(String text) {

        // userMenuButton.setText(text); // Đã ẩn tên để chỉ hiện Avatar



        MenuItem accountItem = new MenuItem("Tài Khoản Của Tôi");

        accountItem.setId("menuAccount");

        MenuItem depositMoney = new MenuItem("Nạp tiền");

        depositMoney.setId("menuDeposit");

        MenuItem logoutItem = new MenuItem("Đăng Xuất");

        logoutItem.setId("menuLogout");



        accountItem.setOnAction(e -> showAccountScreen());

        depositMoney.setOnAction(e -> handleDepositMoney(e));

        logoutItem.setOnAction(e -> System.out.println("Thực hiện Đăng xuất..."));



        logoutItem.setOnAction(event -> {

            try {

                handleLogout(event); // Gọi cái hàm có sẵn của bạn

            } catch (IOException e) {

                e.printStackTrace();

                System.out.println("Lỗi khi chuyển sang màn hình Login!");

            }

        });

        userMenuButton.getItems().addAll(accountItem, depositMoney, new SeparatorMenuItem(), logoutItem);



    }



    private void startPolling() {

        pollingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {

            Thread t = new Thread(r);

            t.setDaemon(true); // Đảm bảo thread tắt khi tắt app

            return t;

        });



        // Gọi API 5 giây 1 lần

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

                                                "Có giá mới", "Sản phẩm " + name + " vừa có bid mới: ₫ " + formatPrice(newPrice));

                                        notif.setAuctionId(auctionId);

                                        notif.setItemName(name);

                                        NotificationCenterService.getInstance().addNotification(notif);

                                    }

                                    if (!"ENDED".equalsIgnoreCase(oldStatus) && !oldStatus.equals(newStatus) && ("ENDED".equalsIgnoreCase(newStatus) || "FINISHED".equalsIgnoreCase(newStatus))) {

                                        AppNotification notif = new AppNotification(NotificationType.AUCTION_END_LOSE, NotificationSeverity.INFO,

                                                "Phiên đấu giá kết thúc", "Sản phẩm " + name + " đã kết thúc.");

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

                logger.error("Lỗi từ Server: {}", response.statusCode());

                Platform.runLater(() -> showOfflineMode("Máy chủ phản hồi mã lỗi: " + response.statusCode()));

            }



        } catch (Exception e) {

            logger.error("Lỗi hệ thống khi tải sản phẩm!: {}", e.getMessage(), e);

            Platform.runLater(() -> showOfflineMode("Không thể kết nối đến máy chủ. Đang ở chế độ ngoại tuyến (Offline)."));

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

        if (!allProducts.isEmpty()) return; // Nếu đã có dữ liệu cũ thì giữ nguyên hiển thị cũ, không làm mất giao diện



        productContainer.getChildren().clear();

        productContainer.getChildren().add(fakeTestBtn);

        currentRenderedIds.clear();



        VBox offlineBox = new VBox(16);

        offlineBox.setAlignment(Pos.CENTER);

        offlineBox.setPadding(new Insets(40));

        offlineBox.setPrefWidth(productContainer.getPrefWidth() > 0 ? productContainer.getPrefWidth() : 600);



        Label iconLabel = new Label("\uE000"); // Biểu tượng cảnh báo / lỗi trong Material Icons

        iconLabel.setStyle("-fx-font-family: 'Material Icons'; -fx-font-size: 64px; -fx-text-fill: #adb5bd;");



        Label titleLabel = new Label("Mất kết nối máy chủ");

        titleLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2e1a28;");



        Label msgLabel = new Label(message);

        msgLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-text-fill: #604868; -fx-wrap-text: true; -fx-text-alignment: center;");

        msgLabel.setMaxWidth(400);



        Button retryBtn = new Button("Thử lại kết nối");

        retryBtn.setStyle("-fx-background-color: #e040a0; -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 24 10 24; -fx-background-radius: 20; -fx-cursor: hand;");

        retryBtn.setOnAction(e -> {

            retryBtn.setText("Đang thử lại...");

            retryBtn.setDisable(true);

            loadProductsFromServer();

        });



        offlineBox.getChildren().addAll(iconLabel, titleLabel, msgLabel, retryBtn);

        productContainer.getChildren().add(offlineBox);

    }



    /**

     * Hàm trung tâm xử lý Data-Driven UI: Lọc bộ đệm (RAM) và vẽ lại màn hình

     */

    private void filterAndRenderProducts() {

        if (showingAccountScreen || showingCompactListScreen) {

            return;

        }



        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";

        String selectedCategory = cbCategory.getValue();

        String selectedStatus = cbStatus.getValue();



        Platform.runLater(() -> {

            List<Integer> newIdsToRender = new ArrayList<>();



            // Bước 1: Tính toán danh sách ID sẽ hiển thị sau khi lọc

            for (JSONObject sessionObj : allProducts) {

                JSONObject itemObj = getItemObject(sessionObj);



                String name = itemObj.optString("name", "");

                String type = itemObj.optString("type", "");

                String status = sessionObj.optString("status", "ACTIVE");



                // Chỉ chặn đứng các phiên bị hủy

                if ("CANCELED".equalsIgnoreCase(status)) {

                    continue;

                }



                // Xác định trạng thái "Coming Soon" (Chưa bắt đầu bán)

                boolean isComingSoon = "COMING".equalsIgnoreCase(status);

                String startTimeStr = sessionObj.optString("startTime", "");

                if (!isComingSoon && !startTimeStr.isEmpty()) {

                    try {

                        java.time.LocalDateTime startTime = java.time.LocalDateTime.parse(startTimeStr);

                        if (java.time.LocalDateTime.now().isBefore(startTime)) {

                            isComingSoon = true;

                        }

                    } catch (Exception e) {

                        // ignore parse error

                    }

                }



                // Logic lọc 3 lớp

                boolean matchKeyword = keyword.isEmpty() || name.toLowerCase().contains(keyword);

                boolean matchCategory = "Tất cả".equals(selectedCategory) || type.equalsIgnoreCase(selectedCategory);

                boolean matchWatchlist = !showingWatchlistOnly || User.watchlistIds.contains(sessionObj.optInt("id"));

                boolean matchMySessions = !showingMySessionsOnly || isSessionOwnedByCurrentUser(sessionObj);



                boolean matchStatus = false;

                if ("Tất cả".equals(selectedStatus)) {

                    matchStatus = true;

                } else if ("COMING SOON".equals(selectedStatus)) {

                    matchStatus = isComingSoon;

                } else if ("ACTIVE".equals(selectedStatus)) {

                    matchStatus = !isComingSoon && "ACTIVE".equalsIgnoreCase(status);

                } else if ("ENDED".equals(selectedStatus)) {

                    matchStatus = !isComingSoon && "ENDED".equalsIgnoreCase(status);

                }

                if ("PENDING".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)) {

                    continue;

                }



                if (matchKeyword && matchCategory && matchStatus && matchWatchlist && matchMySessions) {

                    newIdsToRender.add(sessionObj.optInt("id"));

                }

            }



            // Bước 2: So sánh xem danh sách hiển thị có bị đổi không (thêm/bớt/đổi bộ lọc)

            if (forceRenderProducts || !currentRenderedIds.equals(newIdsToRender)) {

                forceRenderProducts = false;

                // Có sự thay đổi => Vẽ lại toàn bộ

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

                    String status = sessionObj.optString("status", "ACTIVE");



                    if ("PENDING".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)) {

                        continue;

                    }



                    boolean matchKeyword = keyword.isEmpty() || name.toLowerCase().contains(keyword);

                    boolean matchCategory = "Tất cả".equals(selectedCategory) || type.equalsIgnoreCase(selectedCategory);

                    boolean matchStatus = "Tất cả".equals(selectedStatus) || status.equalsIgnoreCase(selectedStatus);

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

                // Cấu trúc không đổi (chỉ là Polling lấy được giá mới) => Cập nhật Label tại chỗ để không giật UI

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

        });

    }





    private void showCategoryChooser() {

        ChoiceDialog<String> dialog = new ChoiceDialog<>(

                cbCategory.getValue() == null ? "Tất cả" : cbCategory.getValue(),

                "Tất cả", "Electronics", "Art", "Vehicle"

        );

        dialog.setTitle("Danh mục");

        dialog.setHeaderText("Chọn danh mục muốn xem");

        dialog.setContentText("Danh mục:");



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

            showWarning("Yêu cầu đăng nhập", "Vui lòng đăng nhập để xem phiên đấu giá của bạn.");

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

            showWarning("Yêu cầu đăng nhập", "Vui lòng đăng nhập để xem các phiên bạn đang đấu giá.");

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

                    Platform.runLater(() -> showError("Không thể tải My Bids", "Server phản hồi mã lỗi: " + response.statusCode()));

                    return;

                }



                JSONObject responseJson = new JSONObject(response.body());

                if (responseJson.optInt("status", 500) != 200) {

                    Platform.runLater(() -> showError("Không thể tải My Bids", responseJson.optString("message", "Lỗi không xác định.")));

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

                logger.error("Lỗi khi tải phiên đang đấu giá của user {}: {}", bidderId, e.getMessage(), e);

                Platform.runLater(() -> showError("Không thể tải My Bids", "Không thể kết nối đến máy chủ hoặc dữ liệu trả về không hợp lệ."));

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

        if (showingMyBidsOnly) return "Chưa có phiên đang đấu giá";

        if (showingMySessionsOnly) return "Chưa có phiên của bạn";

        if (showingWatchlistOnly) return "Watchlist đang trống";

        return "Không có phiên phù hợp";

    }



    private String getEmptyStateMessage() {

        if (showingMyBidsOnly) return "Các phiên bạn đã từng đặt giá sẽ xuất hiện tại đây.";

        if (showingMySessionsOnly) return "Các phiên đấu giá do bạn tạo sẽ xuất hiện tại đây.";

        if (showingWatchlistOnly) return "Hãy bấm biểu tượng yêu thích trên phiên đấu giá để thêm vào Watchlist.";

        return "Thử đổi từ khóa tìm kiếm, thể loại hoặc trạng thái lọc.";

    }



    private VBox createProductCard(JSONObject sessionObj, JSONObject itemObj) {

        int id = sessionObj.getInt("id");



        String type = itemObj.optString("type", "");

        String name = itemObj.optString("name", "");

        BigDecimal currentPrice = getMoney(sessionObj, "currentPrice", getMoney(sessionObj, "startingPrice", BigDecimal.ZERO));



        String status = sessionObj.optString("status", "ACTIVE");



        String startTime = sessionObj.isNull("startTime") ? "Chưa bắt đầu" : sessionObj.getString("startTime").replace("T", " ");

        String endTime = sessionObj.isNull("endTime") ? "Chưa rõ" : sessionObj.getString("endTime").replace("T", " ");

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

                if (isError && !imageWrapper.getChildren().contains(imageStatusLabel)) {

                    imageWrapper.getChildren().setAll(imageStatusLabel);

                }

            });

        } else {

            imageWrapper.getChildren().add(imageStatusLabel);

        }



        // Tính toán trạng thái Coming Soon cho Card hiển thị

        boolean isComingSoon = "COMING".equalsIgnoreCase(status);

        String startTimeStr = sessionObj.optString("startTime", "");

        if (!isComingSoon && !startTimeStr.isEmpty()) {

            try {

                java.time.LocalDateTime startDT = java.time.LocalDateTime.parse(startTimeStr);

                if (java.time.LocalDateTime.now().isBefore(startDT)) {

                    isComingSoon = true;

                }

            } catch (Exception e) {

                // ignore

            }

        }



        String shortEnd = endTime.length() >= 16 ? endTime.substring(0, 16) : endTime;

        if (isComingSoon) {

            HBox timerBadge = new HBox(4.0);

            timerBadge.setStyle("-fx-background-color: rgba(96, 72, 104, 0.9); -fx-background-radius: 15px; -fx-padding: 4px 8px;");

            timerBadge.setAlignment(Pos.CENTER);

            timerBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);



            Label timerIcon = new Label("\uE8B5");

            timerIcon.getStyleClass().add("material-icon");

            timerIcon.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px;");



            Label timerText = new Label("Coming Soon");

            timerText.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: #ffffff;");



            timerBadge.getChildren().addAll(timerIcon, timerText);

            StackPane.setAlignment(timerBadge, Pos.TOP_RIGHT);

            StackPane.setMargin(timerBadge, new Insets(8, 8, 0, 0));

            imageWrapper.getChildren().add(timerBadge);

        } else if ("ACTIVE".equalsIgnoreCase(status)) {

            HBox timerBadge = new HBox(4.0);

            timerBadge.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 15px; -fx-padding: 4px 8px;");

            timerBadge.setAlignment(Pos.CENTER);

            timerBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);



            Label timerIcon = new Label("\uE8B5");

            timerIcon.getStyleClass().add("material-icon");

            timerIcon.setStyle("-fx-text-fill: #e040a0; -fx-font-size: 14px;");



            Label timerText = new Label(shortEnd.length() > 11 ? shortEnd.substring(11) : shortEnd);

            timerText.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: #e040a0;");



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

        priceLabel.setId("priceLabel_" + id); // Đặt ID để cập nhật nhanh

        priceLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 18px; -fx-text-fill: #e040a0;");

        priceBox.getChildren().addAll(lblCurrentBid, priceLabel);



        Region hSpacer = new Region();

        HBox.setHgrow(hSpacer, Priority.ALWAYS);



        HBox actionBox = new HBox(10);

        actionBox.setAlignment(Pos.CENTER_RIGHT);

        actionBox.setMinWidth(52.0);

        actionBox.setPrefWidth(52.0);

        actionBox.setMaxWidth(52.0);



        Button mainBtn = new Button();

        Label mainPlusIcon = new Label("+");

        mainPlusIcon.setFont(Font.font("System", FontWeight.NORMAL, 28));

        mainPlusIcon.setTextFill(Color.web("#e040a0"));

        mainPlusIcon.setAlignment(Pos.CENTER);

        mainPlusIcon.setMinSize(48.0, 48.0);

        mainPlusIcon.setPrefSize(48.0, 48.0);

        mainPlusIcon.setMaxSize(48.0, 48.0);

        mainBtn.setGraphic(mainPlusIcon);

        mainBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        mainBtn.setMinSize(48.0, 48.0);

        mainBtn.setPrefSize(48.0, 48.0);

        mainBtn.setMaxSize(48.0, 48.0);

        mainBtn.setPadding(Insets.EMPTY);

        mainBtn.setAlignment(Pos.CENTER);

        mainBtn.setStyle("-fx-background-color: #ffd6ee; -fx-background-radius: 24px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.16), 10, 0, 0, 3);");

        Tooltip.install(mainBtn, new Tooltip("Tùy chọn"));



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

        Tooltip.install(btnWatch, new Tooltip(User.watchlistIds.contains(id) ? "Đã yêu thích" : "Thêm vào yêu thích"));



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

        Tooltip.install(btnBid, new Tooltip("Đấu giá ngay"));



        btnWatch.setVisible(false); btnWatch.setManaged(false);

        btnBid.setVisible(false); btnBid.setManaged(false);



        if (!isComingSoon && "ACTIVE".equalsIgnoreCase(status)) {

            btnBid.setOnAction(event -> {

                ClientLogger.logViewHistory(User.getUsername(), name, id, currentPrice);

                try {

                    FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 1280, 800);

                    AuctionPageController controller = loader.getController();

                    controller.setItem(sessionObj, itemObj);

                } catch (IOException e) {

                    throw new RuntimeException(e);

                }

            });



            btnWatch.setOnAction(event -> {

                if (User.getId() == null) {

                    Alert alert = new Alert(Alert.AlertType.WARNING);

                    alert.setTitle("Yêu cầu đăng nhập");

                    alert.setHeaderText(null);

                    alert.setContentText("Vui lòng đăng nhập để sử dụng tính năng Yêu thích!");

                    alert.show();

                    return;

                }

                if (User.watchlistIds.contains(id)) {

                    User.watchlistIds.remove(id);

                    watchIcon.setText("\uE87E");

                    watchIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-text-fill: #604868;");

                    watchIcon.setTranslateY(1.5);

                    Tooltip.install(btnWatch, new Tooltip("Thêm vào yêu thích"));

                    ClientLogger.logFavorite(User.getUsername(), name, id, false);

                } else {

                    User.watchlistIds.add(id);

                    watchIcon.setText("\uE87D");

                    watchIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-text-fill: #e040a0;");

                    watchIcon.setTranslateY(1.5);

                    Tooltip.install(btnWatch, new Tooltip("Đã yêu thích"));

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

                mainBtn.setVisible(false); mainBtn.setManaged(false);

                btnWatch.setVisible(true); btnWatch.setManaged(true);

                btnBid.setVisible(true); btnBid.setManaged(true);

            });



            actionBox.getChildren().addAll(btnWatch, btnBid, mainBtn);

        } else {

            mainPlusIcon.setTextFill(Color.web("#907898"));

            mainBtn.setStyle("-fx-background-color: #f2e8f2; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: center;");

            mainBtn.setDisable(true);

            actionBox.getChildren().add(mainBtn);

        }



        bottomRow.getChildren().addAll(priceBox, hSpacer, actionBox);

        vbox.getChildren().addAll(imageWrapper, nameLabel, categoryLabel, spacer, bottomRow);



        return vbox;

    }





    @FXML

    private void handleNotifications(ActionEvent event) {

        showInfo("Thông báo", buildNotificationSummary());

    }



    @FXML

    private void handleSettings(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, "Settings.fxml", 1280, 800);
        } catch (IOException e) {
            logger.error("Lỗi chuyển sang trang Settings.fxml: ", e);
        }
    }



    @FXML

    private void handleListView(ActionEvent event) {

        showCompactAuctionList();

    }



    private void showAccountScreen() {

        if (User.getId() == null) {

            showWarning("Yêu cầu đăng nhập", "Vui lòng đăng nhập để xem và sửa thông tin tài khoản.");

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

            cbCategory.setValue("Tất cả");

        }

        if (cbStatus != null) {

            cbStatus.setValue("Tất cả");

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

        productContainer.getChildren().clear();

        productContainer.getChildren().add(fakeTestBtn);

        currentRenderedIds.clear();

        productContainer.setAlignment(Pos.TOP_LEFT);



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

            logger.warn("Không thể ẩn/hiện filter controls: {}", e.getMessage());

        }

    }



    private VBox buildAccountHeader() {

        VBox headerBox = new VBox(4);

        headerBox.setMaxWidth(1250);

        headerBox.setAlignment(Pos.TOP_LEFT);



        HBox row = new HBox(14);

        row.setAlignment(Pos.CENTER_LEFT);



        Button backButton = new Button("← Quay lại");

        backButton.getStyleClass().add("account-back-btn");

        backButton.setOnAction(e -> returnToAuctionGrid());



        VBox titleBox = new VBox(2);

        Label title = new Label("Tài khoản của tôi");

        title.getStyleClass().add("account-page-title");

        Label subtitle = new Label("Quản lý hồ sơ, ảnh đại diện và thông tin cá nhân.");

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



        Label nameLabel = new Label(safeText(User.getFullname(), "Người dùng"));

        nameLabel.getStyleClass().add("profile-name");



        Label emailLabel = new Label(safeText(User.getEmail(), ""));

        emailLabel.getStyleClass().add("profile-email");



        Label roleBadge = new Label(safeText(User.getRole(), "user").toUpperCase());

        roleBadge.getStyleClass().add("profile-role-badge");



        Button btnChangeAvatar = new Button("Đổi ảnh đại diện");

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

        chooser.setTitle("Chọn ảnh đại diện");

        chooser.getExtensionFilters().add(

                new javafx.stage.FileChooser.ExtensionFilter("Ảnh", "*.jpg", "*.jpeg", "*.png", "*.webp"));



        javafx.stage.Window window = scrollPane.getScene().getWindow();

        java.io.File file = chooser.showOpenDialog(window);

        if (file == null) return;



        // Client-side size validation

        if (file.length() > 5 * 1024 * 1024) {

            showWarning("File quá lớn", "Ảnh đại diện không được vượt quá 5MB.");

            return;

        }



        // Disable button + loading state

        if (btnChangeAvatar != null) {

            btnChangeAvatar.setDisable(true);

            btnChangeAvatar.setText("Đang tải lên...");

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

                        showInfo("Thành công", "Ảnh đại diện đã được cập nhật.");

                    } else {

                        String msg = responseJson.optString("message", "Upload thất bại.");

                        showError("Upload thất bại", msg);

                        resetAvatarButton(btnChangeAvatar);

                    }

                });

            } catch (Exception e) {

                logger.error("Lỗi upload avatar: {}", e.getMessage(), e);

                Platform.runLater(() -> {

                    showError("Upload thất bại", "Không thể kết nối đến máy chủ.");

                    resetAvatarButton(btnChangeAvatar);

                });

            }

        }, "upload-avatar").start();

    }



    private void resetAvatarButton(Button btn) {

        if (btn != null) {

            btn.setDisable(false);

            btn.setText("Đổi ảnh đại diện");

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

            logger.warn("Không thể cập nhật avatar trên top bar: {}", e.getMessage());

        }

    }



    private VBox buildAccountStats() {

        VBox statsColumn = new VBox(14);

        statsColumn.setAlignment(Pos.TOP_LEFT);

        statsColumn.setMinWidth(200);



        statsColumn.getChildren().addAll(

                createProfileStatCard("Số dư tài khoản", "₫ " + formatPrice(User.getBalance()), "\uE227"),

                createProfileStatCard("Vai trò", safeText(User.getRole(), "Chưa rõ"), "\uE7FD"),

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



        Label formTitle = new Label("Thông tin cá nhân");

        formTitle.getStyleClass().add("account-form-title");



        GridPane form = new GridPane();

        form.setHgap(20);

        form.setVgap(16);



        TextField usernameField = createAccountField(safeText(User.getUsername(), ""), "Tên đăng nhập");

        TextField fullnameField = createAccountField(safeText(User.getFullname(), ""), "Họ tên hiển thị");

        TextField emailField = createAccountField(safeText(User.getEmail(), ""), "email@example.com");

        TextField dobField = createAccountField(safeText(User.getDob(), ""), "YYYY-MM-DD hoặc để trống");

        TextField placeField = createAccountField(safeText(User.getPlace_of_birth(), ""), "Nơi sinh");



        addAccountFormRow(form, 0, "Tên đăng nhập", usernameField);

        addAccountFormRow(form, 1, "Họ tên", fullnameField);

        addAccountFormRow(form, 2, "Email", emailField);

        addAccountFormRow(form, 3, "Ngày sinh", dobField);

        addAccountFormRow(form, 4, "Nơi sinh", placeField);



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



        Button reloadButton = new Button("Tải lại thông tin");

        reloadButton.setDisable(saving);

        reloadButton.getStyleClass().add("btn-account-secondary");

        reloadButton.setOnAction(e -> loadLatestAccountProfileForScreen());



        Button saveButton = new Button(saving ? "Đang lưu..." : "Lưu thay đổi");

        saveButton.setDisable(saving);

        saveButton.getStyleClass().add("btn-account-primary");

        saveButton.setOnAction(e -> {

            String username = readTrimmed(usernameField);

            String fullname = readTrimmed(fullnameField);

            String email = readTrimmed(emailField);

            String dob = readTrimmed(dobField);

            String placeOfBirth = readTrimmed(placeField);



            if (username.isBlank()) {

                showWarning("Thiếu tên đăng nhập", "Tên đăng nhập không được để trống.");

                return;

            }

            if (fullname.isBlank()) {

                showWarning("Thiếu họ tên", "Họ tên không được để trống.");

                return;

            }

            if (email.isBlank() || !email.contains("@")) {

                showWarning("Email không hợp lệ", "Vui lòng nhập email hợp lệ.");

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

                logger.warn("Không thể tải lại thông tin tài khoản: {}", e.getMessage());

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

                String message = responseJson.optString("message", "Cập nhật thông tin hoàn tất.");



                if (response.statusCode() == 200 && status == 200) {

                    JSONObject data = responseJson.optJSONObject("data");

                    if (data != null) {

                        applyUserProfileFromJson(data);

                    } else {

                        User.updateProfile(username, fullname, email, dob, placeOfBirth);

                    }

                    Platform.runLater(() -> {

                        renderAccountScreen(false);

                        showInfo("Tài khoản", message);

                    });

                } else {

                    Platform.runLater(() -> {

                        renderAccountScreen(false);

                        showError("Cập nhật thất bại", message);

                    });

                }

            } catch (Exception e) {

                logger.error("Lỗi khi cập nhật tài khoản: {}", e.getMessage(), e);

                Platform.runLater(() -> {

                    renderAccountScreen(false);

                    showError("Cập nhật thất bại", "Không thể kết nối đến máy chủ hoặc dữ liệu trả về không hợp lệ.");

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



        return "Tổng số phiên đang tải: " + total

                + "\nPhiên đang hoạt động: " + active

                + "\nPhiên đã kết thúc: " + ended

                + "\nPhiên trong Watchlist: " + watchlist

                + "\nPhiên của tôi: " + mySessions

                + "\nSố dư hiện tại: ₫ " + formatPrice(User.getBalance());

    }



    private void showSettingsDialog() {

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);

        dialog.setTitle("Cài đặt nhanh");

        dialog.setHeaderText("Chọn thao tác");

        dialog.setContentText("Bạn muốn làm gì với màn danh sách phiên?");



        ButtonType resetFilters = new ButtonType("Đặt lại bộ lọc");

        ButtonType reloadData = new ButtonType("Tải lại dữ liệu");

        ButtonType close = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getButtonTypes().setAll(resetFilters, reloadData, close);



        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isEmpty() || result.get() == close) {

            return;

        }



        if (result.get() == resetFilters) {

            resetFiltersAndShowAll();

            showInfo("Cài đặt", "Đã đặt lại bộ lọc về mặc định.");

        } else if (result.get() == reloadData) {

            forceRenderProducts = true;

            loadProductsFromServer();

            showInfo("Cài đặt", "Đã yêu cầu tải lại dữ liệu từ máy chủ.");

        }

    }



    private void showCompactAuctionList() {

        showingCompactListScreen = true;

        showingAccountScreen = false;



        List<JSONObject> sessionsToShow = getCurrentlyDisplayedSessions();



        productContainer.getChildren().clear();

        productContainer.getChildren().add(fakeTestBtn);

        productContainer.setAlignment(Pos.TOP_LEFT);



        VBox wrapper = new VBox(16);

        wrapper.setAlignment(Pos.TOP_CENTER);

        wrapper.setPadding(new Insets(22, 24, 40, 24));

        wrapper.setPrefWidth(Math.max(760, productContainer.getPrefWidth() > 0 ? productContainer.getPrefWidth() - 80 : 900));



        HBox header = new HBox(14);

        header.setAlignment(Pos.CENTER_LEFT);

        header.setMaxWidth(900);



        VBox titleBox = new VBox(2);

        Label title = new Label("Danh sách phiên rút gọn");

        title.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;");

        Label subtitle = new Label("Các phiên đang hiển thị theo bộ lọc hiện tại.");

        subtitle.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-text-fill: #907898;");

        titleBox.getChildren().addAll(title, subtitle);



        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);



        Button backButton = new Button("Quay lại dạng lưới");

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

            Label emptyTitle = new Label("Không có phiên nào trong bộ lọc hiện tại");

            emptyTitle.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;");

            Label emptyMsg = new Label("Hãy đổi bộ lọc hoặc quay lại dạng lưới.");

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

    }private HBox createCompactAuctionRow(int index, JSONObject sessionObj) {
        JSONObject itemObj = getItemObject(sessionObj);
        int sessionId = sessionObj.optInt("id");
        String itemName = itemObj.optString("name", "Không tên");
        String itemType = itemObj.optString("type", "Không rõ danh mục");
        BigDecimal currentPrice = getMoney(sessionObj, "currentPrice", getMoney(sessionObj, "startingPrice", BigDecimal.ZERO));
        String status = sessionObj.optString("status", "UNKNOWN");
        boolean canBid = "ACTIVE".equalsIgnoreCase(status);

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
        Label name = new Label("#" + sessionId + " · " + itemName);
        name.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;");
        Label type = new Label(itemType);
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

        Button bidButton = new Button(canBid ? "Đấu giá" : "Hết giờ");
        bidButton.setMinWidth(92);
        bidButton.setPrefHeight(38);
        bidButton.setDisable(!canBid);
        if (canBid) {
            bidButton.setStyle("-fx-background-color: #e040a0; -fx-background-radius: 999; -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 8 18 8 18; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(224,64,160,0.25), 10, 0, 0, 3);");
            bidButton.setOnAction(event -> {
                ClientLogger.logViewHistory(User.getUsername(), itemName, sessionId, currentPrice);
                try {
                    FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 1280, 800);
                    AuctionPageController controller = loader.getController();
                    controller.setItem(sessionObj, itemObj);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            bidButton.setStyle("-fx-background-color: #f2e8f2; -fx-background-radius: 999; -fx-text-fill: #907898; -fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 8 18 8 18;");
        }

        row.getChildren().addAll(order, infoBox, rowSpacer, statusBadge, price, bidButton);
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

        cbCategory.setValue("Tất cả");

        cbStatus.setValue("Tất cả");

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

            logger.error("Lỗi khi chuyển sang trang nạp tiền: ", e);

            showError("Lỗi", "Không thể tải trang Nạp tiền.");

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

        if (title.toLowerCase().contains("thành công")) {

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

     * Kết nối Socket để nhận event real-time từ server (VD: AUCTION_ENDED)

     * Reuse hạ tầng Socket của nhphan0505, gửi command JOIN_HOME để đăng ký nhận event global.

     */

    private void connectHomeSocket() {

        Thread homeSocketThread = new Thread(() -> {

            try {

                Socket socket = new Socket(Config.SOCKET_HOST, Config.PORT_SOCKET);

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);



                // Đăng ký với server rằng đây là client Home

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

     * Cập nhật giao diện Card khi phiên đấu giá kết thúc: xám nhạt + vô hiệu hóa nút.

     */

    private void markCardAsEnded(int sessionId) {

        VBox card = sessionCardMap.get(sessionId);

        if (card != null) {

            card.setOpacity(0.6);

            card.setStyle("-fx-border-color: #dee2e6; -fx-border-radius: 5px; -fx-padding: 10px; -fx-background-color: #f4f4f4;");



            // Tìm Button trong Card và cập nhật

            card.getChildren().stream()

                    .filter(node -> node instanceof Button)

                    .map(node -> (Button) node)

                    .findFirst()

                    .ifPresent(btn -> {

                        btn.setText("Hết giờ");

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

            logger.error("Lỗi khi chuyển về trang Quản lý Seller: ", e);

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

    }@FXML
    private void handleToggleProductView(ActionEvent event) {
        compactProductListMode = !compactProductListMode;

        if (compactProductListMode) {
            showCompactAuctionList();
        } else {
            returnToAuctionGrid();
        }

        updateViewToggleButton(compactProductListMode);
    }private void updateViewToggleButton(boolean compactMode) {
        if (btnToggleProductView == null) {
            return;
        }

        btnToggleProductView.getStyleClass().remove("view-toggle-button-active");
        if (compactMode) {
            btnToggleProductView.getStyleClass().add("view-toggle-button-active");
            btnToggleProductView.setTooltip(new Tooltip("Đang xem dạng danh sách. Bấm để về dạng thẻ."));
        } else {
            btnToggleProductView.setTooltip(new Tooltip("Xem dạng danh sách rút gọn"));
        }

        btnToggleProductView.setText("▦");
        btnToggleProductView.setAlignment(Pos.CENTER);
        btnToggleProductView.setContentDisplay(ContentDisplay.CENTER);
    }





}

