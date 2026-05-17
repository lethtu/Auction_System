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
import com.auction.client.service.ClientLogger;
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
    public static boolean initialShowWatchlist = false;

    // Kho lưu trữ Caching cục bộ, giúp Real-time filter không bị trễ
    private final List<JSONObject> allProducts = new ArrayList<>();

    // Cache ảnh để tránh tải lại mỗi lần render
    private final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    // Executor cho Polling
    private ScheduledExecutorService pollingScheduler;
    private final List<Integer> currentRenderedIds = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Button fakeBtn = new Button();
        fakeBtn.setVisible(false);
        fakeBtn.setManaged(false);
        fakeBtn.setId("btnSidebarCategories");

        productContainer.getChildren().add(fakeBtn);
        
        if (User.getFullname() != null) {
            createUserOption("Chào, " + User.getFullname());
        }

        if (User.getRole() != null && User.getRole().equalsIgnoreCase("seller")) {
            btnDashboard.setVisible(true);
            btnDashboard.setManaged(true);
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

        // Thuật toán Space-Evenly động cho danh sách sản phẩm
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            updateGridLayout();
        });

        if (sidebarController != null) {
            sidebarController.setSidebarListener(new SidebarController.SidebarListener() {
                @Override
                public void onFilterWatchlist() {
                    showingWatchlistOnly = true;
                    filterAndRenderProducts();
                }

                @Override
                public void onResetFilter() {
                    showingWatchlistOnly = false;
                    filterAndRenderProducts();
                }
            });

            if (initialShowWatchlist) {
                showingWatchlistOnly = true;
                initialShowWatchlist = false;
                sidebarController.setActiveWatchlist();
            } else {
                sidebarController.setActiveDashboard();
            }
        }

        if (System.getProperty("surefire.test.class.path") == null) {
            startPolling();
        }
    }

    private void updateGridLayout() {
        if (scrollPane == null || productContainer == null) return;
        
        // Tăng buffer lên 8px để triệt tiêu hoàn toàn sai số từ Border hoặc Scrollbar
        double width = scrollPane.getViewportBounds().getWidth() - 8.0;
        if (width <= 0) return;

        double w = 240.0; // Chiều rộng thẻ sản phẩm
        double minGap = 12.0; // Ngưỡng tối thiểu
        
        // Tính số lượng thẻ tối đa có thể vừa 1 hàng (n)
        int n = (int) ((width - minGap) / (w + minGap));
        if (n < 1) n = 1;
        
        // Tính khoảng cách chuẩn g
        double g = Math.floor((width - n * w) / (n + 1));
        if (g < 0) g = 0;
        
        productContainer.setHgap(g);

        // Luôn căn trái: padding = g cho cả trường hợp đủ hàng hay ít sản phẩm
        productContainer.setPadding(new javafx.geometry.Insets(10.0, g, 10.0, g));
    }

    private void createUserOption(String text) {
        // userMenuButton.setText(text); // Đã ẩn tên để chỉ hiện Avatar

        MenuItem accountItem = new MenuItem("Tài Khoản Của Tôi");
        MenuItem depositMoney = new MenuItem("Nạp tiền");
        MenuItem logoutItem = new MenuItem("Đăng Xuất");
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

                if (matchKeyword && matchCategory && matchStatus && matchWatchlist) {
                    newIdsToRender.add(sessionObj.optInt("id"));
                }
            }

            // Bước 2: So sánh xem danh sách hiển thị có bị đổi không (thêm/bớt/đổi bộ lọc)
            if (!currentRenderedIds.equals(newIdsToRender)) {
                // Có sự thay đổi => Vẽ lại toàn bộ
                productContainer.getChildren().clear();
                currentRenderedIds.clear();

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

                    if (matchKeyword && matchCategory && matchStatus && matchWatchlist) {
                        VBox card = createProductCard(sessionObj, itemObj);
                        productContainer.getChildren().add(card);
                        currentRenderedIds.add(sessionObj.optInt("id"));
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


    @FXML
    private void handleApplyFilter(ActionEvent event) {
        filterAndRenderProducts();
    }

    @FXML
    private void handleResetFilter(ActionEvent event) {
        txtSearch.clear();
        cbCategory.setValue("Tất cả");
        cbStatus.setValue("Tất cả");
        filterAndRenderProducts();
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
        Label addIcon = new Label("\uE145"); // + icon
        addIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 22px; -fx-text-fill: #e040a0;");
        addIcon.setAlignment(Pos.CENTER);
        mainBtn.setGraphic(addIcon);
        mainBtn.setStyle("-fx-background-color: #ffd6ee; -fx-background-radius: 20px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-max-width: 40px; -fx-max-height: 40px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand;");
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
            addIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 22px; -fx-text-fill: #907898;");
            mainBtn.setStyle("-fx-background-color: #f2e8f2; -fx-background-radius: 20px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-max-width: 40px; -fx-max-height: 40px;");
            mainBtn.setGraphic(addIcon);
            mainBtn.setDisable(true);
            actionBox.getChildren().add(mainBtn);
        }

        bottomRow.getChildren().addAll(priceBox, hSpacer, actionBox);
        vbox.getChildren().addAll(imageWrapper, nameLabel, categoryLabel, spacer, bottomRow);

        return vbox;
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