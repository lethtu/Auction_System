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
    @FXML private Button btnDashboard;

    @FXML private SidebarController sidebarController;
    @FXML private Button btnHamburger;

    private boolean showingWatchlistOnly = false;
    private boolean showingMyBidsOnly = false;
    private boolean showingMySessionsOnly = false;
    private boolean forceRenderProducts = false;
    public static boolean initialShowWatchlist = false;
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

        // QUAN TRỌNG
        btnHamburger.setOnAction(this::handleToggleSidebar);
        
        if (User.getFullname() != null) {
            createUserOption("Chào, " + User.getFullname());
        }

        // Nút "Kênh Người Bán" trên thanh trên cùng được ẩn để tránh trùng chức năng với sidebar Selling.
        if (btnDashboard != null) {
            btnDashboard.setVisible(false);
            btnDashboard.setManaged(false);
        }

        // Khởi tạo ComboBox
        cbCategory.getItems().addAll("Tất cả", "Electronics", "Art", "Vehicle");
        cbCategory.setValue("Tất cả");

        // ĐÃ SỬA: Xóa "PENDING" khỏi bộ lọc trên giao diện sàn chính
        cbStatus.getItems().addAll("Tất cả", "ACTIVE", "ENDED");
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

            String requestedMode = initialShowWatchlist ? "WATCHLIST" : initialHomeFilterMode;
            initialShowWatchlist = false;
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
            } else {
                sidebarController.setActiveDashboard();
            }
        }

//         if (System.getProperty("surefire.test.class.path") == null) {
//             startPolling();
//         }
    }

    private void updateGridLayout() {
        if (scrollPane == null || productContainer == null) return;

        // Layout ổn định: không tính lại khoảng cách động theo từng thay đổi rất nhỏ của viewport.
        // JavaFX đôi lúc refresh viewport khi click nền / đổi focus app, khiến gap động đổi qua lại.
        // Vì vậy ta giữ gap cố định và để FlowPane căn giữa hàng sản phẩm.
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        if (viewportWidth <= 0) return;

        double stableWidth = Math.max(0, Math.floor(viewportWidth) - 24.0);

        productContainer.setAlignment(Pos.TOP_CENTER);
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
        // userMenuButton.setText(text); // Đã ẩn tên để chỉ hiện Avatar

        MenuItem accountItem = new MenuItem("Tài Khoản Của Tôi");
        MenuItem depositMoney = new MenuItem("Nạp tiền");
        MenuItem logoutItem = new MenuItem("Đăng Xuất");

        accountItem.setOnAction(e -> showAccountDialog());
        depositMoney.setOnAction(e -> handleDepositMoney());
        logoutItem.setOnAction(e -> System.out.println("Thực hiện Đăng xuất..."));

        logoutItem.setOnAction(event -> {
            try {
                handleLogout(event); // Gọi cái hàm có sẵn của bạn
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Lỗi khi chuyển sang màn hình Login!");
            }
        });

        userMenuButton.getItems().addAll(accountItem, depositMoney, logoutItem);

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

                if ("PENDING".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)) {
                    continue;
                }

                boolean matchKeyword = keyword.isEmpty() || name.toLowerCase().contains(keyword);
                boolean matchCategory = "Tất cả".equals(selectedCategory) || type.equalsIgnoreCase(selectedCategory);
                boolean matchStatus = "Tất cả".equals(selectedStatus) || status.equalsIgnoreCase(selectedStatus);
                boolean matchWatchlist = !showingWatchlistOnly || User.watchlistIds.contains(sessionObj.optInt("id"));
                boolean matchMySessions = !showingMySessionsOnly || isSessionOwnedByCurrentUser(sessionObj);

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
        showingWatchlistOnly = false;
        showingMyBidsOnly = false;
        showingMySessionsOnly = false;
        forceRenderProducts = true;
        loadProductsFromServer();
        filterAndRenderProducts();
    }

    private void showWatchlistSessions() {
        showingWatchlistOnly = true;
        showingMyBidsOnly = false;
        showingMySessionsOnly = false;
        forceRenderProducts = true;
        filterAndRenderProducts();
    }

    private void showMySessions() {
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

        String shortEnd = endTime.length() >= 16 ? endTime.substring(0, 16) : endTime;
        if ("ACTIVE".equalsIgnoreCase(status)) {
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

        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button mainBtn = new Button();
        Label mainPlusIcon = new Label("+");
        mainPlusIcon.setFont(Font.font("System", FontWeight.NORMAL, 28));
        mainPlusIcon.setTextFill(Color.web("#e040a0"));
        mainPlusIcon.setAlignment(Pos.CENTER);
        mainPlusIcon.setMinSize(40.0, 40.0);
        mainPlusIcon.setPrefSize(40.0, 40.0);
        mainPlusIcon.setMaxSize(40.0, 40.0);
        mainBtn.setGraphic(mainPlusIcon);
        mainBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        mainBtn.setMinSize(40.0, 40.0);
        mainBtn.setPrefSize(40.0, 40.0);
        mainBtn.setMaxSize(40.0, 40.0);
        mainBtn.setPadding(Insets.EMPTY);
        mainBtn.setAlignment(Pos.CENTER);
        mainBtn.setStyle("-fx-background-color: #ffd6ee; -fx-background-radius: 20px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand;");
        Tooltip.install(mainBtn, new Tooltip("Tùy chọn"));

        Button btnWatch = new Button();
        Label watchIcon = new Label(User.watchlistIds.contains(id) ? "\uE87D" : "\uE87E"); // heart filled or outline
        watchIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-text-fill: " + (User.watchlistIds.contains(id) ? "#e040a0" : "#604868") + ";");
        watchIcon.setAlignment(Pos.CENTER);
        btnWatch.setGraphic(watchIcon);
        btnWatch.setStyle("-fx-background-color: #f2e8f2; -fx-background-radius: 20px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-max-width: 40px; -fx-max-height: 40px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand;");
        Tooltip.install(btnWatch, new Tooltip(User.watchlistIds.contains(id) ? "Đã yêu thích" : "Thêm vào yêu thích"));

        Button btnBid = new Button();
        Label bidIcon = new Label("\uE8CC"); // shopping cart / bid
        bidIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-text-fill: white;");
        bidIcon.setAlignment(Pos.CENTER);
        btnBid.setGraphic(bidIcon);
        btnBid.setStyle("-fx-background-color: #e040a0; -fx-background-radius: 20px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-max-width: 40px; -fx-max-height: 40px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(224,64,160,0.3), 8, 0, 0, 2);");
        Tooltip.install(btnBid, new Tooltip("Đấu giá ngay"));

        btnWatch.setVisible(false); btnWatch.setManaged(false);
        btnBid.setVisible(false); btnBid.setManaged(false);

        if ("ACTIVE".equalsIgnoreCase(status)) {
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
                    Tooltip.install(btnWatch, new Tooltip("Thêm vào yêu thích"));
                    ClientLogger.logFavorite(User.getUsername(), name, id, false);
                } else {
                    User.watchlistIds.add(id);
                    watchIcon.setText("\uE87D");
                    watchIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-text-fill: #e040a0;");
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
            mainBtn.setStyle("-fx-background-color: #f2e8f2; -fx-background-radius: 20px; -fx-padding: 0; -fx-alignment: center;");
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
        showSettingsDialog();
    }

    @FXML
    private void handleListView(ActionEvent event) {
        showCompactAuctionList();
    }

    private String buildAccountInfo() {
        String fullname = safeText(User.getFullname(), "Chưa có tên");
        String username = safeText(User.getUsername(), "Chưa có username");
        String email = safeText(User.getEmail(), "Chưa có email");
        String dob = safeText(User.getDob(), "Chưa có ngày sinh");
        String placeOfBirth = safeText(User.getPlace_of_birth(), "Chưa có nơi sinh");
        String role = safeText(User.getRole(), "Chưa rõ");
        return "Họ tên: " + fullname
                + "\nTên đăng nhập: " + username
                + "\nEmail: " + email
                + "\nNgày sinh: " + dob
                + "\nNơi sinh: " + placeOfBirth
                + "\nVai trò: " + role;
    }

    private void showAccountDialog() {
        if (User.getId() == null) {
            showWarning("Yêu cầu đăng nhập", "Vui lòng đăng nhập để xem và sửa thông tin tài khoản.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tài khoản của tôi");
        dialog.setHeaderText("Xem và cập nhật thông tin tài khoản");

        ButtonType saveButton = new ButtonType("Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        TextField usernameField = new TextField(safeText(User.getUsername(), ""));
        TextField fullnameField = new TextField(safeText(User.getFullname(), ""));
        TextField emailField = new TextField(safeText(User.getEmail(), ""));
        TextField dobField = new TextField(safeText(User.getDob(), ""));
        TextField placeField = new TextField(safeText(User.getPlace_of_birth(), ""));
        Label roleLabel = new Label(safeText(User.getRole(), "Chưa rõ"));

        usernameField.setPromptText("Tên đăng nhập");
        fullnameField.setPromptText("Họ tên hiển thị");
        emailField.setPromptText("email@example.com");
        dobField.setPromptText("YYYY-MM-DD hoặc để trống");
        placeField.setPromptText("Nơi sinh");

        addProfileRow(grid, 0, "Tên đăng nhập", usernameField);
        addProfileRow(grid, 1, "Họ tên", fullnameField);
        addProfileRow(grid, 2, "Email", emailField);
        addProfileRow(grid, 3, "Ngày sinh", dobField);
        addProfileRow(grid, 4, "Nơi sinh", placeField);
        grid.add(new Label("Vai trò"), 0, 5);
        grid.add(roleLabel, 1, 5);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveButton) {
            return;
        }

        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String fullname = fullnameField.getText() == null ? "" : fullnameField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String dob = dobField.getText() == null ? "" : dobField.getText().trim();
        String placeOfBirth = placeField.getText() == null ? "" : placeField.getText().trim();

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

        updateAccountProfile(username, fullname, email, dob, placeOfBirth);
    }

    private void addProfileRow(GridPane grid, int row, String label, TextField field) {
        Label rowLabel = new Label(label);
        field.setPrefWidth(280);
        grid.add(rowLabel, 0, row);
        grid.add(field, 1, row);
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
                        User.updateProfile(
                                data.optString("username", username),
                                data.optString("fullname", fullname),
                                data.optString("email", email),
                                data.optString("dob", dob),
                                data.optString("placeOfBirth", data.optString("place_of_birth", placeOfBirth))
                        );
                    } else {
                        User.updateProfile(username, fullname, email, dob, placeOfBirth);
                    }
                    Platform.runLater(() -> showInfo("Tài khoản", message));
                } else {
                    Platform.runLater(() -> showError("Cập nhật thất bại", message));
                }
            } catch (Exception e) {
                logger.error("Lỗi khi cập nhật tài khoản: {}", e.getMessage(), e);
                Platform.runLater(() -> showError("Cập nhật thất bại", "Không thể kết nối đến máy chủ hoặc dữ liệu trả về không hợp lệ."));
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
                + "\nPhiên của tôi: " + mySessions;
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
        List<JSONObject> sessionsToShow = new ArrayList<>();
        for (JSONObject sessionObj : allProducts) {
            if (currentRenderedIds.contains(sessionObj.optInt("id"))) {
                sessionsToShow.add(sessionObj);
            }
        }

        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(620, 360);

        if (sessionsToShow.isEmpty()) {
            area.setText("Không có phiên nào trong bộ lọc hiện tại.");
        } else {
            StringBuilder builder = new StringBuilder();
            int index = 1;
            for (JSONObject sessionObj : sessionsToShow) {
                JSONObject itemObj = getItemObject(sessionObj);
                BigDecimal currentPrice = getMoney(sessionObj, "currentPrice", getMoney(sessionObj, "startingPrice", BigDecimal.ZERO));
                builder.append(index++)
                        .append(". #").append(sessionObj.optInt("id"))
                        .append(" | ").append(itemObj.optString("name", "Không tên"))
                        .append(" | ").append(sessionObj.optString("status", "UNKNOWN"))
                        .append(" | ₫ ").append(formatPrice(currentPrice))
                        .append("\n");
            }
            area.setText(builder.toString());
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Danh sách phiên rút gọn");
        dialog.setHeaderText("Các phiên đang hiển thị trên màn hình");
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
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

    private void handleDepositMoney() {
        if (User.getId() == null) {
            showWarning("Yêu cầu đăng nhập", "Vui lòng đăng nhập trước khi nạp tiền.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText(null);
        dialog.setContentText("Nhập số tiền muốn nạp:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        BigDecimal amount;
        try {
            amount = new BigDecimal(result.get().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showWarning("Số tiền không hợp lệ", "Số tiền nạp phải lớn hơn 0.");
                return;
            }
        } catch (Exception e) {
            showWarning("Số tiền không hợp lệ", "Vui lòng nhập số hợp lệ, ví dụ: 100000.");
            return;
        }

        new Thread(() -> {
            try {
                String url = Config.API_URL + "/api/bidder/deposit?bidderId=" + User.getId() + "&amount=" + amount.toPlainString();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        showInfo("Nạp tiền", "Nạp tiền thành công. " + response.body());
                    } else {
                        showError("Nạp tiền thất bại", "Server phản hồi mã lỗi: " + response.statusCode());
                    }
                });
            } catch (Exception e) {
                logger.error("Lỗi khi nạp tiền: {}", e.getMessage(), e);
                Platform.runLater(() -> showError("Nạp tiền thất bại", "Không thể kết nối đến máy chủ."));
            }
        }, "deposit-money").start();
    }

    private void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    private void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    private void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleToggleSidebar(ActionEvent event) {
        if (sidebarController != null) {
            sidebarController.toggleSidebar();
            Platform.runLater(this::updateGridLayout);
        }
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
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1024, 768);
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
    }
}
