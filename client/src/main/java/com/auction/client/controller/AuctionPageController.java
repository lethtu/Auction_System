package com.auction.client.controller;

import com.auction.client.Config;
import com.auction.client.model.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.util.NotificationBellBinder;
import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationType;
import com.auction.client.model.notification.NotificationSeverity;
import com.auction.client.service.NotificationCenterService;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;

public class AuctionPageController {
    private static final Logger logger = LoggerFactory.getLogger(AuctionPageController.class);

    @FXML private MenuButton userMenuButton;

    private static final String AUTOBID_PREFIX = "AUTOBID:";

    private static final String ERROR_STYLE = "-fx-text-fill: red;";
    private static final String SUCCESS_STYLE = "-fx-text-fill: green;";
    private static final String INFO_STYLE = "-fx-text-fill: blue;";
    private static final String WARNING_STYLE = "-fx-text-fill: orange;";
    private static final String EXTENSION_STYLE = "-fx-text-fill: #ff8c00; -fx-font-weight: bold;";

    @FXML private Label productNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidBtn;
    @FXML private Button btnAutoBid;
    @FXML private Label messageLabel;
    @FXML private Label remainingTimeLabel;
    @FXML private Label startPriceLabel;
    @FXML private ImageView productImageView;
    @FXML private SidebarController sidebarController;
    @FXML private VBox sideBar;

    @FXML private Button btnNotificationBell;
    @FXML private Label notificationBadge;

    @FXML private Label mainMenuLabel;
    @FXML private Label dashboardText;
    @FXML private Label liveAuctionsText;
    @FXML private Label myBidsText;
    @FXML private Label sellingText;
    @FXML private Label discoverLabel;
    @FXML private Label categoriesText;
    @FXML private Label activeBidsText;
    @FXML private Label watchlistText;
    @FXML private Label endedSoonText;
    @FXML private Label otherLabel;
    @FXML private Label supportText;
    @FXML private Label startSellingText;

    @FXML private Label endingInTitleLabel;
    @FXML private Label startPriceTitleLabel;
    @FXML private Label highestBidTitleLabel;
    @FXML private Label minBidIncrementLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label reserveStatusLabel;
    @FXML private Label totalBidsLabel;
    @FXML private Label watchingLabel;
    @FXML private Label itemDescriptionLabel;
    @FXML private Label minIncrementLabel;
    @FXML private VBox chartContainer;
    @FXML private VBox bidHistoryContainer;

    private static final String MAIN_TEMPLATE_FXML = "MainTemplate.fxml";
    private static final String JOIN_PREFIX = "JOIN:";
    private static final String BID_PREFIX = "BID:";
    private static final String NOTICE_PREFIX = "NOTICE:";
    private static final String RESPONSE_PREFIX = "RESPONSE:";
    private static final String ROOM_COUNT_PREFIX = "ROOM_COUNT:";

    private static final String MONEY_PREFIX = "₫ ";
    private static final String DEFAULT_PRODUCT_NAME = "Unknown Product";
    private static final String DEFAULT_DESCRIPTION = "Chưa có mô tả sản phẩm.";
    private static final String DEFAULT_HIGHEST_BIDDER = "Chưa có người đặt giá";
    private static final String PROCESSING_MESSAGE = "Đang xử lý yêu cầu...";

    private static final int EXPANDED_SIDEBAR_WIDTH = 200;
    private static final int COLLAPSED_SIDEBAR_WIDTH = 70;
    private static final int BID_TIMEOUT_SECONDS = 8;


    private final java.util.List<com.auction.client.model.BidChartPoint> allBidPoints = new java.util.ArrayList<>();
    private final java.util.Set<Integer> seenBidIds = new java.util.HashSet<>();
    private final java.util.Set<String> seenCompositeKeys = new java.util.HashSet<>();
    private static final int MINI_CHART_POINTS = 4;
    private static final int MAX_CHART_POINTS = 50;
    private static final double[] ACTIVITY_OPACITY = {1.0, 0.7, 0.5, 0.35, 0.25};
    private javafx.stage.Stage fullHistoryPopup = null;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;

    private int currentSessionId;
    private BigDecimal currentPrice = BigDecimal.ZERO;
    private BigDecimal stepPrice = BigDecimal.ZERO;
    private BigDecimal startingPrice = BigDecimal.ZERO;
    private BigDecimal reservePrice = BigDecimal.ZERO;
    private Integer highestBidderId;
    private int bidCount;
    private int watchingCount;
    private int currentUserId;
    private BigDecimal sessionStepPrice;
    private BigDecimal myLastBidAmount = null;

    private Timeline timeline;
    private Timeline bidTimeout;



    @FXML
    public void initialize() {
        createUserOption("Avatar");
        initDefaultView();
        
        if (btnNotificationBell != null && notificationBadge != null) {
            NotificationBellBinder.bind(btnNotificationBell, notificationBadge);
        }
        
        if (sidebarController != null) {
            sidebarController.setOnBeforeNavigate(this::disconnectSocket);
            sidebarController.forceCollapse();
        }
        setupResponsiveFontListeners();
    }

    public void setItem(JSONObject sessionObj, JSONObject itemObj) {
        if (sessionObj == null) return;
        this.currentSessionId = sessionObj.optInt("id", 0);
        applySessionData(sessionObj, itemObj);
        initBidTrajectoryCard();
        connectToServer();
        loadBidHistoryFromServer();
        refreshSessionFromServer();
    }

    private void createUserOption(String text) {
        MenuItem accountItem = new MenuItem("Tài Khoản Của Tôi");
        MenuItem depositMoney = new MenuItem("Nạp tiền");
        MenuItem logoutItem = new MenuItem("Đăng Xuất");

        logoutItem.setOnAction(event -> {
            try {
                handleLogout(event);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Lỗi khi chuyển sang màn hình Login!", e);
            }
        });

        if (userMenuButton != null) {
            userMenuButton.getItems().addAll(accountItem, depositMoney, logoutItem);
        }
    }

    public void handleLogout(ActionEvent event) throws IOException {
        User.clearSession();
        SceneSwitcher.switchScene(event, "Login.fxml", 400, 500);
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        if (!isUserLoggedIn()) {
            showError("Vui lòng đăng nhập để đấu giá!");
            return;
        }

        BigDecimal bidAmount = getValidBidAmount();
        if (bidAmount == null) {
            return;
        }

        if (!isSocketReady()) {
            showError("Lỗi kết nối máy chủ Socket!");
            return;
        }

        myLastBidAmount = bidAmount;
        sendBidRequest(bidAmount);
        showBidProcessing();
    }

    @FXML
    private void handleToggleSidebar(ActionEvent event) {
        if (sidebarController != null) {
            sidebarController.toggleSidebar();
        } else if (sideBar != null) {
            boolean shouldExpand = sideBar.getPrefWidth() <= COLLAPSED_SIDEBAR_WIDTH;
            setSidebarExpanded(shouldExpand);
        }
    }

    @FXML
    public void handleAutoBidConfig(ActionEvent event) {
        if (!isUserLoggedIn()) {
            showError("Vui lòng đăng nhập để cấu hình Auto-bid!");
            return;
        }

        if (!isSocketReady()) {
            showError("Chưa kết nối máy chủ Socket!");
            return;
        }

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        VBox root = new VBox();
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");
        root.setPrefWidth(460);

        VBox mainCard = new VBox(15);
        mainCard.setStyle("-fx-padding: 24; -fx-background-color: #fef7ff; -fx-background-radius: 18; -fx-border-color: #f2e8f2; -fx-border-radius: 18; -fx-border-width: 2;");
        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setColor(javafx.scene.paint.Color.rgb(46, 26, 40, 0.15));
        shadow.setRadius(20);
        shadow.setOffsetY(8);
        mainCard.setEffect(shadow);

        javafx.scene.layout.HBox titleBar = new javafx.scene.layout.HBox(10);
        titleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-padding: 0 0 10 0; -fx-cursor: move;");
        
        Label titleLbl = new Label("⚡ Cấu hình Auto-bidding");
        titleLbl.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;");
        
        javafx.scene.layout.Region titleSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(titleSpacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button minBtn = new Button("−");
        minBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 16px;");
        minBtn.setOnAction(ev -> dialog.setIconified(true));
        minBtn.setOnMouseEntered(ev -> minBtn.setStyle("-fx-background-color: #f2e8f2; -fx-font-weight: bold; -fx-text-fill: #2e1a28; -fx-cursor: hand; -fx-font-size: 16px; -fx-background-radius: 8;"));
        minBtn.setOnMouseExited(ev -> minBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 16px;"));

        Button maxBtn = new Button("◻");
        maxBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 16px;");
        maxBtn.setOnAction(ev -> {
            boolean isMax = dialog.isMaximized();
            dialog.setMaximized(!isMax);
            maxBtn.setText(isMax ? "◻" : "❐");
        });
        maxBtn.setOnMouseEntered(ev -> maxBtn.setStyle("-fx-background-color: #f2e8f2; -fx-font-weight: bold; -fx-text-fill: #2e1a28; -fx-cursor: hand; -fx-font-size: 16px; -fx-background-radius: 8;"));
        maxBtn.setOnMouseExited(ev -> maxBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 16px;"));

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 14px;");
        closeBtn.setOnAction(ev -> dialog.close());
        closeBtn.setOnMouseEntered(ev -> closeBtn.setStyle("-fx-background-color: #ffe4e4; -fx-font-weight: bold; -fx-text-fill: #d32f2f; -fx-cursor: hand; -fx-font-size: 14px; -fx-background-radius: 8;"));
        closeBtn.setOnMouseExited(ev -> closeBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 14px;"));
        
        titleBar.getChildren().addAll(titleLbl, titleSpacer, minBtn, maxBtn, closeBtn);

        final double[] xOffset = {0};
        final double[] yOffset = {0};
        titleBar.setOnMousePressed(ev -> { xOffset[0] = ev.getSceneX(); yOffset[0] = ev.getSceneY(); });
        titleBar.setOnMouseDragged(ev -> { dialog.setX(ev.getScreenX() - xOffset[0]); dialog.setY(ev.getScreenY() - yOffset[0]); });

        Label subtitleLabel = new Label("Hệ thống sẽ tự động đặt giá khi có người trả giá cao hơn bạn.");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-font-family: 'DM Sans'; -fx-text-fill: #907898;");
        subtitleLabel.setWrapText(true);

        Label priceBadge = new Label("💰 Giá hiện tại: " + MONEY_PREFIX + formatPrice(currentPrice));
        priceBadge.setStyle(
                "-fx-background-color: #fff0f8;" +
                "-fx-background-radius: 8px;" +
                "-fx-padding: 10px 14px;" +
                "-fx-font-family: 'DM Sans';" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #e040a0;" +
                "-fx-border-color: #ffe8f2;" +
                "-fx-border-radius: 8px;" +
                "-fx-border-width: 1px;"
        );
        priceBadge.setMaxWidth(Double.MAX_VALUE);

        Label maxBidLabel = new Label("Giá kịch kim (Max Bid)");
        maxBidLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2e1a28;");
        Label maxBidHint = new Label("Mức giá cao nhất bạn chấp nhận trả");
        maxBidHint.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 12px; -fx-text-fill: #a890a8;");

        TextField maxBidField = new TextField();
        maxBidField.setPromptText("VD: 5000000");
        maxBidField.setStyle(
                "-fx-background-color: #faf6fa;" +
                "-fx-border-color: #e8d8e8;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;" +
                "-fx-padding: 10px 14px;" +
                "-fx-font-family: 'DM Sans';" +
                "-fx-font-size: 14px;" +
                "-fx-pref-height: 40px;"
        );
        maxBidField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                maxBidField.setStyle(maxBidField.getStyle() + "-fx-border-color: #e040a0;");
            } else {
                maxBidField.setStyle(maxBidField.getStyle().replace("-fx-border-color: #e040a0;", "-fx-border-color: #e8d8e8;"));
            }
        });

        Label incLabel = new Label("Bước giá tự động (Increment)");
        incLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2e1a28;");
        Label incHint = new Label("Mỗi lần hệ thống sẽ cộng thêm bước giá này");
        incHint.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 12px; -fx-text-fill: #a890a8;");

        TextField incrementField = new TextField();
        incrementField.setPromptText("VD: 100000");
        incrementField.setStyle(
                "-fx-background-color: #faf6fa;" +
                "-fx-border-color: #e8d8e8;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;" +
                "-fx-padding: 10px 14px;" +
                "-fx-font-family: 'DM Sans';" +
                "-fx-font-size: 14px;" +
                "-fx-pref-height: 40px;"
        );
        incrementField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                incrementField.setStyle(incrementField.getStyle() + "-fx-border-color: #e040a0;");
            } else {
                incrementField.setStyle(incrementField.getStyle().replace("-fx-border-color: #e040a0;", "-fx-border-color: #e8d8e8;"));
            }
        });

        VBox maxBidGroup = new VBox(4, maxBidLabel, maxBidHint, maxBidField);
        VBox incGroup = new VBox(4, incLabel, incHint, incrementField);

        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(12);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonBox.setStyle("-fx-padding: 10 0 0 0;");

        Button cancelBtn = new Button("Hủy");
        cancelBtn.setStyle(
                "-fx-background-color: #f2e8f2;" +
                "-fx-text-fill: #604868;" +
                "-fx-font-family: 'DM Sans';" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-background-radius: 10px;" +
                "-fx-padding: 10px 24px;" +
                "-fx-cursor: hand;"
        );
        cancelBtn.setOnAction(ev -> dialog.close());

        Button activateBtn = new Button("Kích hoạt");
        activateBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #e040a0, #f06292);" +
                "-fx-text-fill: white;" +
                "-fx-font-family: 'DM Sans';" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-background-radius: 10px;" +
                "-fx-padding: 10px 24px;" +
                "-fx-cursor: hand;"
        );
        activateBtn.setOnAction(ev -> {
            if (processAutoBidInput(maxBidField.getText(), incrementField.getText())) {
                dialog.close();
            }
        });

        buttonBox.getChildren().addAll(cancelBtn, activateBtn);

        mainCard.getChildren().addAll(titleBar, subtitleLabel, priceBadge, maxBidGroup, incGroup, buttonBox);
        root.getChildren().add(mainCard);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        
        Platform.runLater(maxBidField::requestFocus);
        dialog.showAndWait();
    }

    private boolean processAutoBidInput(String maxBidText, String incrementText) {
        BigDecimal maxBid;
        BigDecimal increment;

        try {
            maxBid = parseMoneyInput(maxBidText);
        } catch (NumberFormatException e) {
            showError("Giá kịch kim phải là con số hợp lệ!");
            return false;
        }

        try {
            increment = parseMoneyInput(incrementText);
        } catch (NumberFormatException e) {
            showError("Bước giá phải là con số hợp lệ!");
            return false;
        }

        if (maxBid.compareTo(currentPrice) <= 0) {
            showError("Giá kịch kim phải lớn hơn giá hiện tại (" + MONEY_PREFIX + formatPrice(currentPrice) + ")!");
            return false;
        }

        if (increment.compareTo(BigDecimal.ZERO) <= 0) {
            showError("Bước giá phải lớn hơn 0!");
            return false;
        }

        JSONObject json = new JSONObject();
        json.put("auctionId", currentSessionId);
        json.put("bidderId", User.getId());
        json.put("maxBid", maxBid);
        json.put("increment", increment);

        out.println(AUTOBID_PREFIX + json.toString());

        messageLabel.setStyle(WARNING_STYLE);
        messageLabel.setText("Đang kích hoạt Auto-bidding...");
        logger.info("Sent AUTOBID request: {}", json);
        
        return true;
    }

    @FXML
    private void handleStartSelling(ActionEvent event) {
        disconnectSocket();
        logger.info("Người dùng nhấn nút Start Selling (+), chuyển sang UpToSeller.fxml");
        try {
            SceneSwitcher.switchScene(event, "UpToSeller.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Lỗi khi chuyển sang trang đăng ký Seller: ", e);
        }
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        disconnectSocket();

        try {
            SceneSwitcher.switchScene(event, MAIN_TEMPLATE_FXML);
        } catch (IOException e) {
            logger.error("Cannot go back to main template", e);
            showError("Lỗi khi quay lại trang trước.");
        }
    }

    private void initBidTrajectoryCard() {
        allBidPoints.clear(); seenBidIds.clear(); seenCompositeKeys.clear();
        if (chartContainer != null) chartContainer.getChildren().clear();
        if (bidHistoryContainer != null) bidHistoryContainer.getChildren().clear();
        if (chartContainer != null && chartContainer.getParent() != null) {
            javafx.scene.Node card = chartContainer.getParent().getParent();
            if (card != null) { card.setCursor(javafx.scene.Cursor.HAND); card.setOnMouseClicked(e -> showFullBidHistoryDialog()); }
        }
    }

    private void loadBidHistoryFromServer() {
        Thread t = new Thread(() -> { try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(Config.API_URL + "/api/auctions/" + currentSessionId + "/bid-history")).GET().build();
            HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (isSuccessfulResponse(res)) {
                org.json.JSONArray arr = new org.json.JSONArray(res.body());
                Platform.runLater(() -> {
                    for (int i = 0; i < arr.length(); i++) { JSONObject b = arr.getJSONObject(i);
                        appendOrMergeBidPoint(b.optInt("bidId",-1), getMoney(b, "amount", BigDecimal.ZERO), b.optString("bidTime",null), b.optInt("bidderId",0), b.optString("bidderName","#????")); }
                    allBidPoints.sort(java.util.Comparator.comparingLong(com.auction.client.model.BidChartPoint::getEpochMillis));
                    bidCount = allBidPoints.size();
                    renderMiniChart(); renderRecentActivity();
                    updateBidInfoLabels();
                    if (fullHistoryUpdater != null) fullHistoryUpdater.run();
                });
            }
        } catch (Exception e) { logger.warn("Lỗi tải bid history: {}", e.getMessage()); } });
        t.setDaemon(true); t.start();
    }

    private void appendOrMergeBidPoint(int bidId, BigDecimal amount, String bidTime, int bidderId, String maskedBidderCode) {
        if (bidId > 0) { if (seenBidIds.contains(bidId)) return; seenBidIds.add(bidId); }
        else { String k = (bidTime != null ? bidTime : "") + "|" + bidderId + "|" + amount; if (seenCompositeKeys.contains(k)) return; seenCompositeKeys.add(k); }
        boolean mine = User.getId() != null && bidderId == User.getId();
        com.auction.client.model.BidChartPoint pt = new com.auction.client.model.BidChartPoint(bidId, amount, bidTime, toEpochMillis(bidTime), bidderId, maskedBidderCode, mine);
        pt.setRelativeTime(formatRelativeTime(bidTime)); allBidPoints.add(pt);
        while (allBidPoints.size() > MAX_CHART_POINTS) allBidPoints.remove(0);
    }

    private void renderMiniChart() {
        if (chartContainer == null) return; chartContainer.getChildren().clear();
        if (allBidPoints.isEmpty()) { Label el = new Label("No bids yet"); el.setStyle("-fx-font-family:'DM Sans';-fx-font-size:11px;-fx-text-fill:#907898;"); chartContainer.getChildren().add(el); return; }
        int n = allBidPoints.size(), start = Math.max(0, n - MINI_CHART_POINTS);
        java.util.List<com.auction.client.model.BidChartPoint> recent = new java.util.ArrayList<>(allBidPoints.subList(start, n));
        javafx.scene.chart.NumberAxis xa = new javafx.scene.chart.NumberAxis(-0.3, recent.size()-1+0.3, 1);
        xa.setTickLabelsVisible(false); xa.setTickMarkVisible(false); xa.setMinorTickVisible(false); xa.setOpacity(0);
        
        double minAmt = Double.MAX_VALUE; double maxAmt = Double.MIN_VALUE;
        for(com.auction.client.model.BidChartPoint p : recent) {
            double a = p.getAmount().doubleValue();
            if(a < minAmt) minAmt = a; if(a > maxAmt) maxAmt = a;
        }
        if(minAmt == Double.MAX_VALUE) { minAmt = 0; maxAmt = 10000; }
        double padding = getEffectiveStepPrice() != null ? getEffectiveStepPrice().doubleValue() : 0;
        if(padding <= 0) padding = currentPrice != null ? currentPrice.doubleValue() * 0.05 : 10000;
        if(padding <= 0) padding = 10000;
        double yLower = recent.get(0).getAmount().doubleValue();
        double yUpper = (currentPrice != null ? currentPrice.doubleValue() : maxAmt) + padding;
        if(yLower >= yUpper) { yLower = Math.max(0, yUpper - padding * 2); }
        
        javafx.scene.chart.NumberAxis ya = new javafx.scene.chart.NumberAxis(yLower, yUpper, (yUpper - yLower)/4);
        ya.setAutoRanging(false); ya.setForceZeroInRange(false); ya.setTickMarkVisible(false); ya.setMinorTickVisible(false);
        ya.setTickLabelsVisible(true);
        ya.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override public String toString(Number num) { 
                double v = num.doubleValue();
                if(v >= 1000000000) return "₫" + String.format("%.1fB", v/1000000000);
                if(v >= 1000000) return "₫" + String.format("%.1fM", v/1000000);
                if(v >= 1000) return "₫" + String.format("%.1fK", v/1000);
                return "₫" + String.format("%.0f", v);
            }
            @Override public Number fromString(String s) { return 0; }
        });
        ya.setStyle("-fx-tick-label-font-family: 'DM Sans'; -fx-tick-label-font-size: 11px; -fx-tick-label-fill: #604868; -fx-font-weight: bold;");

        javafx.scene.chart.AreaChart<Number,Number> mc = new javafx.scene.chart.AreaChart<>(xa, ya);
        mc.setLegendVisible(false); mc.setAnimated(false); mc.setCreateSymbols(true); mc.setHorizontalGridLinesVisible(true); mc.setVerticalGridLinesVisible(false);
        mc.setAlternativeRowFillVisible(false); mc.setAlternativeColumnFillVisible(false); mc.setPrefHeight(140); mc.setMaxHeight(140); mc.setStyle("-fx-padding:0;-fx-background-color:transparent;");
        javafx.scene.chart.XYChart.Series<Number,Number> s = new javafx.scene.chart.XYChart.Series<>();
        for (int i = 0; i < recent.size(); i++) s.getData().add(new javafx.scene.chart.XYChart.Data<>(i, recent.get(i).getAmount().doubleValue()));
        mc.getData().add(s);
        Platform.runLater(() -> { try {
            javafx.scene.Node ln = mc.lookup(".default-color0.chart-series-area-line"); if (ln != null) ln.setStyle("-fx-stroke:#e040a0;-fx-stroke-width:3px;");
            javafx.scene.Node fl = mc.lookup(".default-color0.chart-series-area-fill"); if (fl != null) fl.setStyle("-fx-fill:linear-gradient(to bottom,rgba(224,64,160,0.35),rgba(224,64,160,0.02));");
            javafx.scene.Node bg = mc.lookup(".chart-plot-background"); if (bg != null) bg.setStyle("-fx-background-color:transparent; -fx-border-color: transparent transparent #dcc8e0 #dcc8e0; -fx-border-width: 0 0 1 1;");
            javafx.scene.Node hgl = mc.lookup(".chart-horizontal-grid-lines"); if (hgl != null) hgl.setStyle("-fx-stroke: #f2e8f2; -fx-stroke-dash-array: 4 4;");
            for (int i = 0; i < s.getData().size(); i++) { javafx.scene.Node sym = s.getData().get(i).getNode(); if (sym != null) {
                boolean last = (i == s.getData().size()-1); sym.setStyle("-fx-background-color:#e040a0,white;-fx-background-insets:0,2;-fx-background-radius:"+(last?"10px":"7px")+";-fx-padding:"+(last?"5":"3.5")+";");
                com.auction.client.model.BidChartPoint p = recent.get(i);
                
                String timeStr = p.getBidTime() != null ? p.getBidTime().replace("T", " ") : "";
                if (timeStr.length() > 19) timeStr = timeStr.substring(0, 19);
                javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip("Bid #" + p.getBidId() + "\n" + p.getDisplayName()+"\n₫ "+formatPrice(p.getAmount())+"\n"+timeStr+"\n"+p.getRelativeTime()); 
                tip.setStyle("-fx-font-family:'DM Sans';-fx-font-size:12px; -fx-background-color: rgba(46,26,40,0.9); -fx-text-fill: white; -fx-padding: 8px; -fx-background-radius: 8px;");
                tip.setShowDelay(javafx.util.Duration.millis(100));
                javafx.scene.control.Tooltip.install(sym, tip); 
                
                sym.setCursor(javafx.scene.Cursor.HAND);
                sym.setOnMouseClicked(ev -> showFullBidHistoryDialog());
                sym.setOnMouseEntered(ev -> { sym.setScaleX(1.3); sym.setScaleY(1.3); });
                sym.setOnMouseExited(ev -> { sym.setScaleX(1.0); sym.setScaleY(1.0); });
            } }
        } catch (Exception ignored) {} });
        javafx.scene.layout.HBox xLabels = new javafx.scene.layout.HBox(); xLabels.setAlignment(javafx.geometry.Pos.CENTER); xLabels.setStyle("-fx-padding:4 8 0 8;");
        for (int i = 0; i < recent.size(); i++) { boolean last = (i == recent.size()-1);
            long sec = 999999;
            try { sec = Duration.between(LocalDateTime.parse(recent.get(i).getBidTime()), LocalDateTime.now()).getSeconds(); } catch(Exception ignored){}
            String labelTxt = last ? (sec <= 60 ? "NOW" : "LATEST") : formatShortRelative(recent.get(i).getBidTime());
            Label lbl = new Label(labelTxt);
            lbl.setStyle("-fx-font-family:'DM Sans';-fx-font-size:11px;-fx-font-weight:900;"+(last?"-fx-text-fill:#e040a0;":"-fx-text-fill:#604868;"));
            javafx.scene.layout.HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS); lbl.setMaxWidth(Double.MAX_VALUE); lbl.setAlignment(javafx.geometry.Pos.CENTER); xLabels.getChildren().add(lbl); }
        chartContainer.getChildren().addAll(mc, xLabels);
    }

    private String formatShortRelative(String bidTime) {
        if (bidTime == null) return "START";
        try { long sec = Duration.between(LocalDateTime.parse(bidTime), LocalDateTime.now()).getSeconds();
            if (sec < 60) return sec+"s ago"; if (sec < 3600) return (sec/60)+"m ago"; if (sec < 86400) return (sec/3600)+"h ago"; return (sec/86400)+"d ago";
        } catch (Exception e) { return "START"; }
    }

    private String formatRelativeTime(String bidTime) {
        if (bidTime == null) return "Past";
        try { long sec = Duration.between(LocalDateTime.parse(bidTime), LocalDateTime.now()).getSeconds();
            if (sec < 60) return sec+"s ago"; if (sec < 3600) return (sec/60)+"m ago"; if (sec < 86400) return (sec/3600)+"h ago"; return (sec/86400)+"d ago";
        } catch (Exception e) { return "Past"; }
    }

    private long toEpochMillis(String iso) {
        if (iso == null || iso.isEmpty()) return System.currentTimeMillis();
        try { return LocalDateTime.parse(iso).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(); } catch (Exception e) { return System.currentTimeMillis(); }
    }

    private void renderRecentActivity() {
        if (bidHistoryContainer == null) return; bidHistoryContainer.getChildren().clear();
        int n = allBidPoints.size(), show = Math.min(n, 5);
        for (int i = 0; i < show; i++) {
            com.auction.client.model.BidChartPoint pt = allBidPoints.get(n-1-i); pt.setRelativeTime(formatRelativeTime(pt.getBidTime()));
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10); row.setAlignment(javafx.geometry.Pos.CENTER);
            row.setOpacity(ACTIVITY_OPACITY[Math.min(i, ACTIVITY_OPACITY.length-1)]);
            String dn = pt.isMine() ? pt.getMaskedBidderCode()+" (You)" : pt.getDisplayName();
            Label nl = new Label(dn); nl.setStyle("-fx-font-family:'DM Sans';-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#2e1a28;");
            javafx.scene.layout.Region sp = new javafx.scene.layout.Region(); javafx.scene.layout.HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
            Label al = new Label("₫ "+formatPrice(pt.getAmount())); al.setStyle("-fx-font-family:'DM Sans';-fx-font-size:13px;-fx-font-weight:900;-fx-text-fill:#2e1a28;");
            Label tl = new Label(pt.getRelativeTime()); tl.setStyle("-fx-font-family:'DM Sans';-fx-font-size:10px;-fx-text-fill:#907898;"); tl.setPrefWidth(60); tl.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            row.getChildren().addAll(nl, sp, al, tl); bidHistoryContainer.getChildren().add(row);
        }
    }

    private Runnable fullHistoryUpdater = null;

    private void showFullBidHistoryDialog() {
        if (fullHistoryPopup != null && fullHistoryPopup.isShowing()) {
            fullHistoryPopup.requestFocus();
            return;
        }
        javafx.stage.Stage popup = new javafx.stage.Stage(); 
        popup.initModality(javafx.stage.Modality.NONE);
        popup.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        fullHistoryPopup = popup; 
        popup.setOnCloseRequest(e -> { fullHistoryPopup = null; fullHistoryUpdater = null; });

        VBox root = new VBox();
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");
        root.setPrefSize(780, 740);

        VBox mainCard = new VBox(15);
        mainCard.setStyle("-fx-padding: 24; -fx-background-color: #fef7ff; -fx-background-radius: 18; -fx-border-color: #f2e8f2; -fx-border-radius: 18; -fx-border-width: 2;");
        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setColor(javafx.scene.paint.Color.rgb(46, 26, 40, 0.15));
        shadow.setRadius(20);
        shadow.setOffsetY(8);
        mainCard.setEffect(shadow);
        VBox.setVgrow(mainCard, javafx.scene.layout.Priority.ALWAYS);
        root.getChildren().add(mainCard);
        
        // CSS cho TableView vµ Chart
        String css = ".table-view { -fx-background-color: transparent; -fx-border-color: #f2e8f2; -fx-border-radius: 8px; -fx-background-radius: 8px; } " +
                     ".table-view .column-header-background { -fx-background-color: #faf6fa; -fx-background-radius: 8px 8px 0 0; } " +
                     ".table-view .column-header, .table-view .filler { -fx-background-color: transparent; -fx-size: 40px; -fx-border-color: #e8d8e8; -fx-border-width: 0 0 1 0; } " +
                     ".table-view .column-header .label { -fx-text-fill: #907898; -fx-font-weight: 900; -fx-font-size: 13px; -fx-font-family: 'DM Sans'; } " +
                     ".table-view .table-row-cell { -fx-background-color: white; -fx-border-color: #f2e8f2; -fx-border-width: 0 0 1 0; -fx-cell-size: 45px; } " +
                     ".table-view .table-row-cell:hover { -fx-background-color: #fff0f8; } " +
                     ".table-view .table-row-cell:selected { -fx-background-color: #ffe4f2; -fx-background-insets: 0; } " +
                     ".table-view .table-cell { -fx-font-size: 13px; -fx-font-family: 'DM Sans'; } " +
                     ".table-view .scroll-bar:vertical, .table-view .scroll-bar:horizontal { -fx-background-color: transparent; } " +
                     ".table-view .scroll-bar:vertical .track, .table-view .scroll-bar:horizontal .track { -fx-background-color: transparent; -fx-border-color: transparent; -fx-background-radius: 0; } " +
                     ".table-view .scroll-bar:vertical .thumb, .table-view .scroll-bar:horizontal .thumb { -fx-background-color: #dcc8e0; -fx-background-radius: 8px; } " +
                     ".table-view .scroll-bar:vertical .thumb:hover, .table-view .scroll-bar:horizontal .thumb:hover { -fx-background-color: #c0a8c8; } " +
                     ".table-view .scroll-bar .increment-button, .table-view .scroll-bar .decrement-button { -fx-background-color: transparent; -fx-padding: 0; } " +
                     ".table-view .scroll-bar .increment-arrow, .table-view .scroll-bar .decrement-arrow { -fx-shape: \" \"; -fx-padding: 0; } " +
                     ".table-view .corner { -fx-background-color: transparent; } " +
                     ".chart-vertical-grid-lines { -fx-stroke: transparent; } " +
                     ".chart-horizontal-grid-lines { -fx-stroke: #f2e8f2; -fx-stroke-dash-array: 4 4; } " +
                     ".axis { -fx-tick-label-fill: #907898; -fx-tick-label-font-size: 11px; } " +
                     ".axis-label { -fx-text-fill: #604868; -fx-font-weight: bold; -fx-font-size: 12px; }";
        try {
            java.io.File cssFile = java.io.File.createTempFile("popupStyle", ".css");
            cssFile.deleteOnExit();
            java.nio.file.Files.writeString(cssFile.toPath(), css);
            root.getStylesheets().add(cssFile.toURI().toString());
        } catch (Exception ignored) {}

        javafx.scene.layout.HBox titleBar = new javafx.scene.layout.HBox(10);
        titleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-padding: 0 0 10 0; -fx-cursor: move;");
        
        Label titleLbl = new Label("📊 Bid Trajectory & Full History");
        titleLbl.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;");
        
        javafx.scene.layout.Region titleSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(titleSpacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button minBtn = new Button("−");
        minBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 16px;");
        minBtn.setOnAction(ev -> popup.setIconified(true));
        minBtn.setOnMouseEntered(ev -> minBtn.setStyle("-fx-background-color: #f2e8f2; -fx-font-weight: bold; -fx-text-fill: #2e1a28; -fx-cursor: hand; -fx-font-size: 16px; -fx-background-radius: 8;"));
        minBtn.setOnMouseExited(ev -> minBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 16px;"));

        Button maxBtn = new Button("◻");
        maxBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 16px;");
        maxBtn.setOnAction(ev -> {
            boolean isMax = popup.isMaximized();
            popup.setMaximized(!isMax);
            maxBtn.setText(isMax ? "◻" : "❐");
        });
        maxBtn.setOnMouseEntered(ev -> maxBtn.setStyle("-fx-background-color: #f2e8f2; -fx-font-weight: bold; -fx-text-fill: #2e1a28; -fx-cursor: hand; -fx-font-size: 16px; -fx-background-radius: 8;"));
        maxBtn.setOnMouseExited(ev -> maxBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 16px;"));

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 14px;");
        closeBtn.setOnAction(ev -> { fullHistoryPopup = null; fullHistoryUpdater = null; popup.close(); });
        closeBtn.setOnMouseEntered(ev -> closeBtn.setStyle("-fx-background-color: #ffe4e4; -fx-font-weight: bold; -fx-text-fill: #d32f2f; -fx-cursor: hand; -fx-font-size: 14px; -fx-background-radius: 8;"));
        closeBtn.setOnMouseExited(ev -> closeBtn.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #907898; -fx-cursor: hand; -fx-font-size: 14px;"));
        
        titleBar.getChildren().addAll(titleLbl, titleSpacer, minBtn, maxBtn, closeBtn);

        final double[] xOffset = {0};
        final double[] yOffset = {0};
        titleBar.setOnMousePressed(event -> { xOffset[0] = event.getSceneX(); yOffset[0] = event.getSceneY(); });
        titleBar.setOnMouseDragged(event -> { popup.setX(event.getScreenX() - xOffset[0]); popup.setY(event.getScreenY() - yOffset[0]); });

        javafx.scene.layout.HBox hdr = new javafx.scene.layout.HBox(12); hdr.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        javafx.scene.control.ComboBox<String> flt = new javafx.scene.control.ComboBox<>(); flt.getItems().addAll("Last 10","Last 50","Full History"); 
        flt.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e8d8e8; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 4 8; -fx-font-family:'DM Sans'; -fx-font-weight: bold; -fx-text-fill: #e040a0; -fx-cursor: hand;");
        hdr.getChildren().addAll(flt);
        javafx.scene.chart.NumberAxis fxa = new javafx.scene.chart.NumberAxis(); fxa.setLabel("Time"); fxa.setAutoRanging(true); fxa.setForceZeroInRange(false); fxa.setTickMarkVisible(false); fxa.setMinorTickVisible(false);
        fxa.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            final java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
            @Override public String toString(Number v) { return v==null?"":java.time.Instant.ofEpochMilli(v.longValue()).atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(f); }
            @Override public Number fromString(String s) { return 0; } });
        javafx.scene.chart.NumberAxis fya = new javafx.scene.chart.NumberAxis(); fya.setLabel("Price (VND)"); fya.setAutoRanging(true); fya.setForceZeroInRange(false); fya.setTickMarkVisible(false); fya.setMinorTickVisible(false);
        fya.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override public String toString(Number num) { 
                double v = num.doubleValue();
                if(v >= 1000000000) return String.format("%.1fB", v/1000000000);
                if(v >= 1000000) return String.format("%.1fM", v/1000000);
                if(v >= 1000) return String.format("%.1fK", v/1000);
                return String.format("%.0f", v);
            }
            @Override public Number fromString(String s) { return 0; }
        });
        javafx.scene.chart.LineChart<Number,Number> fc = new javafx.scene.chart.LineChart<>(fxa, fya); fc.setLegendVisible(false); fc.setAnimated(false); fc.setCreateSymbols(true); fc.setPrefHeight(240);
        javafx.scene.layout.HBox sb = new javafx.scene.layout.HBox(16); sb.setStyle("-fx-padding:16;-fx-background-color:#fff0f8;-fx-background-radius:12;-fx-border-color:#f2e8f2;-fx-border-radius:12;-fx-border-width:1;");
        
        javafx.scene.control.TableView<com.auction.client.model.BidChartPoint> tbl = new javafx.scene.control.TableView<>();
        tbl.setStyle("-fx-background-color: transparent; -fx-font-family: 'DM Sans'; -fx-border-color: #f2e8f2; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        javafx.scene.control.TableColumn<com.auction.client.model.BidChartPoint,String> c1 = new javafx.scene.control.TableColumn<>("Time");
        c1.setCellValueFactory(cd -> {
            if (cd.getValue().getBidTime() == null) return new javafx.beans.property.SimpleStringProperty("");
            try {
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(cd.getValue().getBidTime());
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss · dd/MM/yyyy");
                return new javafx.beans.property.SimpleStringProperty(dt.format(dtf));
            } catch (Exception ex) { return new javafx.beans.property.SimpleStringProperty(cd.getValue().getBidTime()); }
        });
        c1.setStyle("-fx-font-weight: bold; -fx-text-fill: #604868;");
        javafx.scene.control.TableColumn<com.auction.client.model.BidChartPoint,String> c2 = new javafx.scene.control.TableColumn<>("Bidder");
        c2.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDisplayName()));
        c2.setStyle("-fx-font-weight: 900; -fx-text-fill: #2e1a28;");
        javafx.scene.control.TableColumn<com.auction.client.model.BidChartPoint,String> c3 = new javafx.scene.control.TableColumn<>("Amount");
        c3.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty("₫ "+formatPrice(cd.getValue().getAmount())));
        c3.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: 900; -fx-text-fill: #2e1a28; -fx-font-size: 14px;");
        javafx.scene.control.TableColumn<com.auction.client.model.BidChartPoint,String> c4 = new javafx.scene.control.TableColumn<>("Increment");
        c4.setCellValueFactory(cd -> { int idx = allBidPoints.indexOf(cd.getValue()); if (idx<=0) return new javafx.beans.property.SimpleStringProperty("-");
            return new javafx.beans.property.SimpleStringProperty("+₫ "+formatPrice(cd.getValue().getAmount().subtract(allBidPoints.get(idx-1).getAmount()))); });
        c4.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #e040a0; -fx-font-weight: 900; -fx-font-size: 14px;");
        
        tbl.getColumns().addAll(java.util.List.of(c1,c2,c3,c4)); tbl.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS); VBox.setVgrow(tbl, javafx.scene.layout.Priority.ALWAYS);
        
        Runnable pop = () -> { 
            if (allBidPoints.isEmpty()) {
                fc.getData().clear();
                sb.getChildren().clear();
                tbl.getItems().clear();
                tbl.setPlaceholder(new Label("No bid history yet"));
                return;
            }
            String fv = flt.getValue(); if (fv == null) fv = "Last 50";
            int lim = "Last 10".equals(fv)?10:"Last 50".equals(fv)?50:allBidPoints.size();
            int ss = Math.max(0, allBidPoints.size()-lim); java.util.List<com.auction.client.model.BidChartPoint> sub = new java.util.ArrayList<>(allBidPoints.subList(ss, allBidPoints.size()));
            fc.getData().clear(); javafx.scene.chart.XYChart.Series<Number,Number> fs = new javafx.scene.chart.XYChart.Series<>();
            for (com.auction.client.model.BidChartPoint p : sub) fs.getData().add(new javafx.scene.chart.XYChart.Data<>(p.getEpochMillis(), p.getAmount().doubleValue()));
            fc.getData().add(fs); Platform.runLater(() -> { 
                javafx.scene.Node l = fc.lookup(".default-color0.chart-series-line"); if (l!=null) l.setStyle("-fx-stroke:#e040a0;-fx-stroke-width:2.5px;"); 
                javafx.scene.Node bg = fc.lookup(".chart-plot-background"); if (bg != null) bg.setStyle("-fx-background-color:transparent; -fx-border-color: transparent transparent #dcc8e0 #dcc8e0; -fx-border-width: 0 0 1 1;");
                for (int i = 0; i < fs.getData().size(); i++) {
                    javafx.scene.Node sym = fs.getData().get(i).getNode();
                    if (sym != null) {
                        sym.setStyle("-fx-background-color: #e040a0, white; -fx-background-insets: 0, 2; -fx-background-radius: 6px; -fx-padding: 4px;");
                        sym.setCursor(javafx.scene.Cursor.HAND);
                        com.auction.client.model.BidChartPoint p = sub.get(i);
                        String timeStr = p.getBidTime() != null ? p.getBidTime().replace("T", " ") : "";
                        if (timeStr.length() > 19) timeStr = timeStr.substring(0, 19);
                        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip("Bid #" + p.getBidId() + "\n" + p.getDisplayName()+"\n₫ "+formatPrice(p.getAmount())+"\n"+timeStr+"\n"+p.getRelativeTime()); 
                        tip.setStyle("-fx-font-family:'DM Sans';-fx-font-size:12px; -fx-background-color: rgba(46,26,40,0.9); -fx-text-fill: white; -fx-padding: 8px; -fx-background-radius: 8px;");
                        tip.setShowDelay(javafx.util.Duration.millis(100));
                        javafx.scene.control.Tooltip.install(sym, tip);
                        sym.setOnMouseEntered(ev -> { sym.setScaleX(1.3); sym.setScaleY(1.3); });
                        sym.setOnMouseExited(ev -> { sym.setScaleX(1.0); sym.setScaleY(1.0); });
                    }
                }
            });
            sb.getChildren().clear();
            BigDecimal hi = allBidPoints.isEmpty()?BigDecimal.ZERO:allBidPoints.get(allBidPoints.size()-1).getAmount();
            BigDecimal lo = allBidPoints.isEmpty()?BigDecimal.ZERO:allBidPoints.get(0).getAmount();
            BigDecimal mxi = BigDecimal.ZERO; for (int i=1;i<allBidPoints.size();i++){BigDecimal d=allBidPoints.get(i).getAmount().subtract(allBidPoints.get(i-1).getAmount());if(d.compareTo(mxi)>0)mxi=d;}
            String lt = allBidPoints.isEmpty()?"-":formatRelativeTime(allBidPoints.get(allBidPoints.size()-1).getBidTime());
            String[][] sts={{"Total Bids",""+allBidPoints.size()},{"Highest","₫ "+formatPrice(hi)},{"Start","₫ "+formatPrice(lo)},{"Max Δ","+₫ "+formatPrice(mxi)},{"Last Bid",lt}};
            for (String[] st : sts) { VBox sv = new VBox(4); sv.setAlignment(javafx.geometry.Pos.CENTER);
                Label k = new Label(st[0]); k.setStyle("-fx-font-family:'DM Sans';-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:#907898;");
                Label v = new Label(st[1]); v.setStyle("-fx-font-family:'DM Sans';-fx-font-size:16px;-fx-font-weight:900;-fx-text-fill:#2e1a28;");
                sv.getChildren().addAll(k,v); javafx.scene.layout.HBox.setHgrow(sv, javafx.scene.layout.Priority.ALWAYS); sb.getChildren().add(sv); }
            javafx.collections.ObservableList<com.auction.client.model.BidChartPoint> items = javafx.collections.FXCollections.observableArrayList();
            for (int i=sub.size()-1;i>=0;i--) items.add(sub.get(i)); tbl.setItems(items); 
        };
        flt.setOnAction(e -> pop.run()); 
        flt.setValue("Last 50"); // This will also trigger the action if it changes, but just to be safe:
        pop.run();
        this.fullHistoryUpdater = pop;
        
        mainCard.getChildren().addAll(titleBar, hdr, fc, sb, tbl); 
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(scene); 
        popup.show();
    }

    public void setRemainingTime(String endTimeStr) {
        stopTimeline();

        LocalDateTime endTime = LocalDateTime.parse(endTimeStr);

        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> updateRemainingTime(endTime)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void initDefaultView() {
        productNameLabel.setText("Loading...");
        currentPriceLabel.setText("...");
        remainingTimeLabel.setText("00:00:00");

        setLabelText(minIncrementLabel, "Tăng tối thiểu ₫ 0");
        setLabelText(highestBidderLabel, DEFAULT_HIGHEST_BIDDER);
        setLabelText(reserveStatusLabel, "");
        setLabelText(totalBidsLabel, "0");
        setLabelText(watchingLabel, "0");
        setLabelText(itemDescriptionLabel, DEFAULT_DESCRIPTION);
    }

    private void setupResponsiveFontListeners() {
        Platform.runLater(() -> {
            if (currentPriceLabel.getScene() == null) {
                return;
            }

            double initialWidth = currentPriceLabel.getScene().getWidth();

            currentPriceLabel.getScene().widthProperty().addListener((obs, oldVal, newVal) ->
                    updateResponsiveFonts(newVal.doubleValue())
            );

            currentPriceLabel.textProperty().addListener((obs, oldVal, newVal) ->
                    updateResponsiveFonts(currentPriceLabel.getScene().getWidth())
            );

            remainingTimeLabel.textProperty().addListener((obs, oldVal, newVal) ->
                    updateResponsiveFonts(currentPriceLabel.getScene().getWidth())
            );

            updateResponsiveFonts(initialWidth);
        });
    }

    private void updateResponsiveFonts(double windowWidth) {
        double priceFont = calculatePriceFont(windowWidth);
        double timeFont = calculateTimeFont(windowWidth);
        double startPriceFont = Math.max(14, Math.min(20, windowWidth * 0.014));
        double bidFieldFont = Math.max(12, Math.min(24, windowWidth * 0.017));

        setNodeStyle(currentPriceLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) priceFont + "px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;"
        );

        setNodeStyle(highestBidTitleLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) Math.max(10, Math.min(14, windowWidth * 0.01)) + "px; -fx-font-weight: 900; -fx-text-fill: #604868;"
        );

        setNodeStyle(remainingTimeLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) timeFont + "px; -fx-font-weight: 900; -fx-text-fill: #e040a0;"
        );

        setNodeStyle(endingInTitleLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) Math.max(10, timeFont * 0.7) + "px; -fx-font-weight: 900; -fx-text-fill: #7c52aa;"
        );

        setNodeStyle(startPriceLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) startPriceFont + "px; -fx-font-weight: 900; -fx-text-fill: #3d0028;"
        );

        setNodeStyle(startPriceTitleLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) Math.max(8, Math.min(12, windowWidth * 0.008)) + "px; -fx-font-weight: 900; -fx-text-fill: #a02070; -fx-padding: 8 0 0 0;"
        );

        setNodeStyle(bidAmountField,
                "-fx-background-color: white; -fx-border-color: #ece2ec; -fx-border-width: 2; " +
                        "-fx-border-radius: 999; -fx-padding: 16 16 16 48; " +
                        "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) bidFieldFont + "px; -fx-font-weight: 900;"
        );
    }

    private double calculatePriceFont(double windowWidth) {
        String priceText = currentPriceLabel.getText();
        double baseFont = Math.max(22, Math.min(48, windowWidth * 0.034));
        int extraChars = Math.max(0, priceText.length() - 8);
        return Math.max(16, baseFont - extraChars * 1.5);
    }

    private double calculateTimeFont(double windowWidth) {
        String timeText = remainingTimeLabel.getText();
        double baseFont = Math.max(14, Math.min(22, windowWidth * 0.016));
        int extraChars = Math.max(0, timeText.length() - 8);
        return Math.max(12, baseFont - extraChars * 0.8);
    }

    private void refreshSessionFromServer() {
        Thread refreshThread = new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/auctions/" + currentSessionId))
                        .GET()
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (isSuccessfulResponse(response)) {
                    JSONObject session = new JSONObject(response.body());
                    JSONObject item = session.optJSONObject("item");
                    Platform.runLater(() -> applySessionData(session, item));
                }
            } catch (Exception e) {
                logger.warn("Không tải lại được chi tiết phiên {}: {}", currentSessionId, e.getMessage());
            }
        });

        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private boolean isSuccessfulResponse(HttpResponse<String> response) {
        return response.statusCode() == 200
                && response.body() != null
                && !response.body().isBlank();
    }

    private void applySessionData(JSONObject sessionObj, JSONObject itemObj) {
        loadPriceData(sessionObj);
        loadProductData(sessionObj, itemObj);
        updatePriceLabels();
        updateBidInfoLabels();

        String endTime = sessionObj.optString("endTime", "");
        if (!endTime.isEmpty()) {
            setRemainingTime(endTime);
        }
    }

    private void loadPriceData(JSONObject sessionObj) {
        startingPrice = getMoneyAny(sessionObj, BigDecimal.ZERO, "startingPrice", "startPrice");
        currentPrice = getMoneyAny(sessionObj, startingPrice, "currentPrice", "highestBid");
        stepPrice = getMoneyAny(sessionObj, BigDecimal.ZERO, "stepPrice", "minBidIncrement");
        reservePrice = getMoneyAny(sessionObj, BigDecimal.ZERO, "reservePrice");
        highestBidderId = getOptionalInt(sessionObj, "highestBidderId");
        bidCount = getBidCount(sessionObj);

        if (currentPrice.compareTo(BigDecimal.ZERO) <= 0 && startingPrice.compareTo(BigDecimal.ZERO) > 0) {
            currentPrice = startingPrice;
        }
    }

    private void loadProductData(JSONObject sessionObj, JSONObject itemObj) {
        JSONObject source = itemObj != null ? itemObj : sessionObj;

        String productName = source.optString(
                "name",
                sessionObj.optString("productName", DEFAULT_PRODUCT_NAME)
        );

        String description = source.optString(
                "description",
                sessionObj.optString("description", "")
        );

        String imagePath = source.optString(
                "imagePath",
                sessionObj.optString("imagePath", "")
        );

        productNameLabel.setText(productName);
        updateDescription(description);
        loadProductImage(imagePath);
    }

    private void updatePriceLabels() {
        currentPriceLabel.setText(MONEY_PREFIX + formatPrice(currentPrice));

        if (startingPrice.compareTo(BigDecimal.ZERO) >= 0) {
            startPriceLabel.setText(MONEY_PREFIX + formatPrice(startingPrice));
        } else {
            startPriceLabel.setText("---");
        }
    }

    private void loadProductImage(String imagePath) {
        String imageUrl = buildImageUrl(imagePath);

        if (imageUrl.isBlank()) {
            productImageView.setImage(null);
            return;
        }

        try {
            Image image = new Image(imageUrl, true);
            image.errorProperty().addListener((obs, wasError, isError) -> {
                if (isError) {
                    logger.warn("Không tải được ảnh sản phẩm từ {}", imageUrl);
                }
            });
            productImageView.setImage(image);
        } catch (Exception e) {
            logger.error("Error loading product image from {}", imageUrl, e);
        }
    }

    private BigDecimal getEffectiveStepPrice() {
        if (stepPrice != null && stepPrice.compareTo(BigDecimal.ZERO) > 0) {
            return stepPrice;
        }
        if (currentPrice == null) return new BigDecimal("10000");
        if (currentPrice.compareTo(new BigDecimal("100000")) < 0) {
            return new BigDecimal("10000");
        } else if (currentPrice.compareTo(new BigDecimal("500000")) < 0) {
            return new BigDecimal("20000");
        } else if (currentPrice.compareTo(new BigDecimal("1000000")) < 0) {
            return new BigDecimal("50000");
        } else if (currentPrice.compareTo(new BigDecimal("5000000")) < 0) {
            return new BigDecimal("100000");
        } else {
            return new BigDecimal("200000");
        }
    }

    private void updateBidInfoLabels() {
        setLabelText(minIncrementLabel, "Tăng tối thiểu ₫ " + formatPrice(getEffectiveStepPrice()));
        setLabelText(highestBidderLabel, formatHighestBidder());
        setLabelText(reserveStatusLabel, formatReserveStatus());
        int displayBidCount = Math.max(Math.max(0, bidCount), allBidPoints.size());
        setLabelText(totalBidsLabel, String.valueOf(displayBidCount));
        setLabelText(watchingLabel, String.valueOf(Math.max(0, watchingCount)));
    }

    private String formatHighestBidder() {
        if (highestBidderId == null) {
            return DEFAULT_HIGHEST_BIDDER;
        }
        if (User.getId() != null && highestBidderId.equals(User.getId())) {
            return "Người đang giữ giá: Bạn (You)";
        }
        return "Người đang giữ giá: User #" + highestBidderId;
    }

    private String formatReserveStatus() {
        if (reservePrice == null || reservePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }

        return currentPrice.compareTo(reservePrice) >= 0
                ? "Giá sàn đã đạt"
                : "Giá sàn chưa đạt";
    }

    private void connectToServer() {
        disconnectSocket();

        listenerThread = new Thread(this::listenToSocketServer);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenToSocketServer() {
        try {
            openSocketConnection();
            listenForServerMessages();
        } catch (EOFException | java.net.SocketException e) {
            logger.info("Kết nối Socket đã đóng.");
        } catch (Exception e) {
            logger.error("Socket connection error", e);
            Platform.runLater(() -> {
                finishBidProcessing();
                showError("Mất kết nối với máy chủ Socket!");
            });
        }
    }

    private void openSocketConnection() throws IOException {
        socket = new Socket(Config.SOCKET_HOST, Config.PORT_SOCKET);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println(JOIN_PREFIX + currentSessionId);
    }

    private void listenForServerMessages() throws IOException {
        String serverResponse;

        while (!socket.isClosed() && (serverResponse = in.readLine()) != null) {
            handleServerMessage(serverResponse);
        }
    }

    private void handleServerMessage(String serverResponse) {
        try {
            if (serverResponse.startsWith(NOTICE_PREFIX)) {
                handleNoticeMessage(serverResponse.substring(NOTICE_PREFIX.length()));
            } else if (serverResponse.startsWith(RESPONSE_PREFIX)) {
                handleBidResponseMessage(serverResponse.substring(RESPONSE_PREFIX.length()));
            } else if (serverResponse.startsWith(ROOM_COUNT_PREFIX)) {
                handleRoomCountMessage(serverResponse.substring(ROOM_COUNT_PREFIX.length()));
            }
        } catch (Exception e) {
            logger.warn("Cannot handle socket message: {}", serverResponse, e);
        }
    }

    private void handleNoticeMessage(String jsonString) {
        JSONObject noticeObj = new JSONObject(jsonString);

        Platform.runLater(() -> {
            BigDecimal newPrice = noticeObj.getBigDecimal("newPrice");
            currentPrice = newPrice;
            currentPriceLabel.setText(MONEY_PREFIX + formatPrice(newPrice));

            highestBidderId = getOptionalInt(noticeObj, "highestBidderId");
            bidCount = getOptionalIntOrDefault(noticeObj, "bidCount", bidCount);
            updateBidInfoLabels();

            // ============ REALTIME CHART UPDATE ============
            int bidId = noticeObj.optInt("bidId", -1);
            int bidderId = noticeObj.optInt("bidderId", 0);
            String bidTime = noticeObj.optString("bidTime", null);
            String maskedCode = noticeObj.optString("maskedBidderCode", "#" + String.format("%04d", Math.abs(bidderId) % 10000));
            appendOrMergeBidPoint(bidId, newPrice, bidTime, bidderId, maskedCode);
            allBidPoints.sort(java.util.Comparator.comparingLong(com.auction.client.model.BidChartPoint::getEpochMillis));
            renderMiniChart();
            renderRecentActivity();
            // ================================================

            if (noticeObj.has("newEndTime")) {
                handleAuctionExtended(noticeObj.getString("newEndTime"));
                AppNotification notif = new AppNotification(NotificationType.AUCTION_EXTENDED, NotificationSeverity.INFO, 
                    "Phiên đã được gia hạn", "Có bid trong những giây cuối nên phiên được kéo dài.");
                notif.setAuctionId(currentSessionId);
                notif.setItemName(productNameLabel.getText());
                NotificationCenterService.getInstance().addNotification(notif);
            } else {
                if (User.getId() == null || highestBidderId == null || !highestBidderId.equals(User.getId())) {
                    showInfo("Có người vừa ra giá mới!");
                    if (myLastBidAmount != null && newPrice.compareTo(myLastBidAmount) > 0) {
                        AppNotification notif = new AppNotification(NotificationType.OUTBID, NotificationSeverity.WARNING, 
                            "Bạn đã bị vượt giá", "Sản phẩm " + productNameLabel.getText() + " hiện đã lên ₫ " + formatPrice(newPrice));
                        notif.setAuctionId(currentSessionId);
                        notif.setItemName(productNameLabel.getText());
                        NotificationCenterService.getInstance().addNotification(notif);
                    } else if (User.watchlistIds.contains(currentSessionId) || myLastBidAmount != null) {
                        AppNotification notif = new AppNotification(NotificationType.NEW_BID, NotificationSeverity.INFO, 
                            "Có giá mới", "Sản phẩm " + productNameLabel.getText() + " vừa có bid mới: ₫ " + formatPrice(newPrice));
                        notif.setAuctionId(currentSessionId);
                        notif.setItemName(productNameLabel.getText());
                        NotificationCenterService.getInstance().addNotification(notif);
                    }
                }
            }
        });
    }

    private void handleAuctionExtended(String newEndTime) {
        messageLabel.setStyle(EXTENSION_STYLE);
        messageLabel.setText("Phiên đấu giá vừa được gia hạn thêm 60 giây!");
        setRemainingTime(newEndTime);
    }

    private void handleBidResponseMessage(String jsonString) {
        JSONObject responseObj = new JSONObject(jsonString);

        Platform.runLater(() -> {
            finishBidProcessing();

            if (responseObj.getBoolean("success")) {
                if ("AUTOBID_CONFIG".equals(responseObj.optString("type"))) {
                    messageLabel.setStyle(SUCCESS_STYLE);
                    messageLabel.setText(responseObj.optString("message"));
                    
                    AppNotification notif = new AppNotification(NotificationType.AUTO_BID_CONFIGURED, NotificationSeverity.SUCCESS, 
                        "Cấu hình Auto-bid thành công", "Hệ thống sẽ tự động đấu giá cho sản phẩm " + productNameLabel.getText());
                    notif.setAuctionId(currentSessionId);
                    notif.setItemName(productNameLabel.getText());
                    NotificationCenterService.getInstance().addNotification(notif);
                } else {
                    handleSuccessfulBid(responseObj);
                }
            } else {
                showError(responseObj.optString("message", "Đặt giá thất bại."));
                AppNotification notif = new AppNotification(NotificationType.BID_FAILED, NotificationSeverity.DANGER, 
                    "Đặt giá thất bại", responseObj.optString("message", "Đặt giá thất bại."));
                notif.setAuctionId(currentSessionId);
                notif.setItemName(productNameLabel.getText());
                NotificationCenterService.getInstance().addNotification(notif);
            }
        });
    }

    private void handleSuccessfulBid(JSONObject responseObj) {
        currentPrice = getMoney(responseObj, "currentPrice", currentPrice);
        highestBidderId = getOptionalInt(responseObj, "highestBidderId");
        bidCount = getOptionalIntOrDefault(responseObj, "bidCount", bidCount);

        currentPriceLabel.setText(MONEY_PREFIX + formatPrice(currentPrice));
        updateBidInfoLabels();

        messageLabel.setStyle(SUCCESS_STYLE);
        messageLabel.setText(responseObj.optString("message", "Đặt giá thành công."));
        bidAmountField.clear();
        
        AppNotification notif = new AppNotification(NotificationType.BID_SUCCESS, NotificationSeverity.SUCCESS, 
            "Đặt giá thành công", "Bạn đã đặt giá thành công cho " + productNameLabel.getText());
        notif.setAuctionId(currentSessionId);
        notif.setItemName(productNameLabel.getText());
        NotificationCenterService.getInstance().addNotification(notif);
    }

    private void handleRoomCountMessage(String countText) {
        int count = Integer.parseInt(countText);

        Platform.runLater(() -> {
            watchingCount = count;
            updateBidInfoLabels();
        });
    }

    private void disconnectSocket() {
        closeQuietly(out);
        closeQuietly(in);
        closeSocketQuietly();

        out = null;
        in = null;
        socket = null;
    }

    private boolean isUserLoggedIn() {
        return User.getId() != null;
    }

    private BigDecimal getValidBidAmount() {
        String input = bidAmountField.getText().trim();

        if (input.isEmpty()) {
            showError("Vui lòng nhập mức giá!");
            return null;
        }

        BigDecimal bidAmount;
        try {
            bidAmount = parseMoneyInput(input);
        } catch (NumberFormatException e) {
            showError("Mức giá phải là con số hợp lệ!");
            return null;
        }

        if (bidAmount.compareTo(currentPrice) <= 0) {
            showError("Giá đặt phải LỚN HƠN giá hiện tại (₫ " + formatPrice(currentPrice) + ")!");
            return null;
        }

        BigDecimal increment = getEffectiveStepPrice();
        BigDecimal minimumBid = currentPrice.add(increment);
        if (bidAmount.compareTo(minimumBid) < 0) {
            showError("Giá đặt tối thiểu là ₫ " + formatPrice(minimumBid) + "!");
            return null;
        }

        return bidAmount;
    }

    private boolean isSocketReady() {
        return socket != null && !socket.isClosed() && out != null;
    }

    private void sendBidRequest(BigDecimal bidAmount) {
        JSONObject jsonBid = new JSONObject();
        jsonBid.put("auctionId", currentSessionId);
        jsonBid.put("bidderId", User.getId());
        jsonBid.put("amount", bidAmount);

        out.println(BID_PREFIX + jsonBid);
    }

    private void showBidProcessing() {
        placeBidBtn.setDisable(true);
        messageLabel.setStyle(WARNING_STYLE);
        messageLabel.setText(PROCESSING_MESSAGE);
        startBidTimeout();
    }

    private void startBidTimeout() {
        stopBidTimeout();

        bidTimeout = new Timeline(new KeyFrame(javafx.util.Duration.seconds(BID_TIMEOUT_SECONDS), event -> {
            placeBidBtn.setDisable(false);

            if (PROCESSING_MESSAGE.equals(messageLabel.getText())) {
                messageLabel.setStyle(WARNING_STYLE);
                messageLabel.setText("Máy chủ phản hồi hơi lâu, kiểm tra kết nối hoặc thử lại.");
            }
        }));

        bidTimeout.setCycleCount(1);
        bidTimeout.play();
    }

    private void finishBidProcessing() {
        stopBidTimeout();

        if (placeBidBtn != null) {
            placeBidBtn.setDisable(false);
        }
    }

    private void setSidebarExpanded(boolean expanded) {
        int width = expanded ? EXPANDED_SIDEBAR_WIDTH : COLLAPSED_SIDEBAR_WIDTH;
        sideBar.setPrefWidth(width);
        sideBar.setMaxWidth(width);

        setLabelsVisible(expanded,
                mainMenuLabel,
                dashboardText,
                liveAuctionsText,
                myBidsText,
                sellingText,
                discoverLabel,
                categoriesText,
                activeBidsText,
                watchlistText,
                endedSoonText,
                otherLabel,
                supportText,
                startSellingText
        );
    }

    private void setLabelsVisible(boolean visible, Label... labels) {
        for (Label label : labels) {
            if (label != null) {
                label.setVisible(visible);
                label.setManaged(visible);
            }
        }
    }

    private void updateRemainingTime(LocalDateTime endTime) {
        long secondsLeft = Duration.between(LocalDateTime.now(), endTime).getSeconds();

        if (secondsLeft <= 0) {
            stopTimeline();
            remainingTimeLabel.setText("Phiên đấu giá đã kết thúc!");
            handleAuctionEnd();
            return;
        }

        long hours = secondsLeft / 3600;
        long minutes = (secondsLeft % 3600) / 60;
        long seconds = secondsLeft % 60;

        remainingTimeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void handleAuctionEnd() {
        placeBidBtn.setDisable(true);
        bidAmountField.setDisable(true);
        disconnectSocket();
        
        if (highestBidderId != null && User.getId() != null && highestBidderId.equals(User.getId())) {
            AppNotification notif = new AppNotification(NotificationType.AUCTION_END_WIN, NotificationSeverity.SUCCESS, 
                "Bạn đã thắng!", "Bạn là người trả giá cao nhất cho " + productNameLabel.getText());
            notif.setAuctionId(currentSessionId);
            notif.setItemName(productNameLabel.getText());
            NotificationCenterService.getInstance().addNotification(notif);
        } else if (User.getId() != null && myLastBidAmount != null) {
            AppNotification notif = new AppNotification(NotificationType.AUCTION_END_LOSE, NotificationSeverity.WARNING, 
                "Phiên đấu giá kết thúc", "Rất tiếc, bạn không thắng phiên đấu giá " + productNameLabel.getText());
            notif.setAuctionId(currentSessionId);
            notif.setItemName(productNameLabel.getText());
            NotificationCenterService.getInstance().addNotification(notif);
        }
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void stopBidTimeout() {
        if (bidTimeout != null) {
            bidTimeout.stop();
            bidTimeout = null;
        }
    }

    private BigDecimal parseMoneyInput(String raw) {
        String normalized = raw == null
                ? ""
                : raw.trim()
                .replace("₫", "")
                .replace("đ", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", "");

        return new BigDecimal(normalized);
    }

    private BigDecimal getMoney(JSONObject object, String key, BigDecimal defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue;
        }

        try {
            return new BigDecimal(object.get(key).toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal getMoneyAny(JSONObject object, BigDecimal defaultValue, String... keys) {
        if (keys == null) {
            return defaultValue == null ? BigDecimal.ZERO : defaultValue;
        }

        for (String key : keys) {
            BigDecimal value = getMoney(object, key, null);
            if (value != null) {
                return value;
            }
        }

        return defaultValue == null ? BigDecimal.ZERO : defaultValue;
    }

    private Integer getOptionalInt(JSONObject object, String key) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return null;
        }

        return object.optInt(key);
    }

    private int getOptionalIntOrDefault(JSONObject object, String key, int defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue;
        }

        return object.optInt(key, defaultValue);
    }

    private int getBidCount(JSONObject object) {
        if (object == null) {
            return 0;
        }

        if (object.has("bidCount") && !object.isNull("bidCount")) {
            return object.optInt("bidCount", 0);
        }

        if (object.has("bids") && !object.isNull("bids")) {
            return object.optJSONArray("bids") == null ? 0 : object.optJSONArray("bids").length();
        }

        return 0;
    }

    private void updateDescription(String description) {
        if (description == null || description.isBlank()) {
            setLabelText(itemDescriptionLabel, DEFAULT_DESCRIPTION);
            return;
        }

        setLabelText(itemDescriptionLabel, description.trim());
    }

    private String buildImageUrl(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }

        String path = rawPath.trim().replace("\\", "/");

        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        path = removeLeadingSlashes(path);
        path = removeKnownImagePrefix(path);

        return path.isBlank() ? "" : Config.API_URL + "/api/files/images/" + path;
    }

    private String removeLeadingSlashes(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }

    private String removeKnownImagePrefix(String path) {
        String[] prefixes = {
                "server/upload/images/",
                "upload/images/",
                "images/"
        };

        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
        }

        return path;
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "0";
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');

        DecimalFormat decimalFormat = new DecimalFormat("###,###", symbols);
        return decimalFormat.format(price);
    }

    private void showError(String message) {
        messageLabel.setStyle(ERROR_STYLE);
        messageLabel.setText(message);
    }

    private void showInfo(String message) {
        messageLabel.setStyle(INFO_STYLE);
        messageLabel.setText(message);
    }

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    private void setNodeStyle(javafx.scene.Node node, String style) {
        if (node != null) {
            node.setStyle(style);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            logger.warn("Cannot close resource", e);
        }
    }

    private void closeSocketQuietly() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.warn("Cannot close socket", e);
        }
    }
}