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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final String ERROR_STYLE = "-fx-text-fill: red;";
    private static final String SUCCESS_STYLE = "-fx-text-fill: green;";
    private static final String INFO_STYLE = "-fx-text-fill: blue;";
    private static final String WARNING_STYLE = "-fx-text-fill: orange;";
    private static final String EXTENSION_STYLE = "-fx-text-fill: #ff8c00; -fx-font-weight: bold;";

    @FXML private Label productNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidBtn;
    @FXML private Label messageLabel;
    @FXML private Label remainingTimeLabel;
    @FXML private Label startPriceLabel;
    @FXML private ImageView productImageView;
    @FXML private VBox sideBar;

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
    @FXML private Label watchingCountLabel;
    @FXML private Label itemDescriptionLabel;

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

    private Timeline timeline;
    private Timeline bidTimeout;

    @FXML
    public void initialize() {
        initDefaultView();
        setupResponsiveFontListeners();
    }

    public void setItem(JSONObject sessionObj, JSONObject itemObj) {
        try {
            currentSessionId = sessionObj.getInt("id");
            applySessionData(sessionObj, itemObj);
            refreshSessionFromServer();
            connectToServer();
            logger.info("Successfully set item for session ID: {}", currentSessionId);
        } catch (Exception e) {
            logger.error("Error while setting auction item", e);
            showError("Không thể tải thông tin phiên đấu giá.");
        }
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

        sendBidRequest(bidAmount);
        showBidProcessing();
    }

    @FXML
    private void handleToggleSidebar(ActionEvent event) {
        boolean shouldExpand = sideBar.getPrefWidth() <= COLLAPSED_SIDEBAR_WIDTH;
        setSidebarExpanded(shouldExpand);
    }

    @FXML
    private void handleStartSelling(ActionEvent event) {
        logger.info("Start selling clicked in auction room");
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

        setLabelText(minBidIncrementLabel, "Tăng tối thiểu ₫ 0");
        setLabelText(highestBidderLabel, DEFAULT_HIGHEST_BIDDER);
        setLabelText(reserveStatusLabel, "");
        setLabelText(totalBidsLabel, "0");
        setLabelText(watchingCountLabel, "0");
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

        if (startingPrice.compareTo(BigDecimal.ZERO) > 0) {
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

    private void updateBidInfoLabels() {
        setLabelText(minBidIncrementLabel, "Tăng tối thiểu ₫ " + formatPrice(stepPrice));
        setLabelText(highestBidderLabel, formatHighestBidder());
        setLabelText(reserveStatusLabel, formatReserveStatus());
        setLabelText(totalBidsLabel, String.valueOf(Math.max(0, bidCount)));
        setLabelText(watchingCountLabel, String.valueOf(Math.max(0, watchingCount)));
    }

    private String formatHighestBidder() {
        return highestBidderId == null
                ? DEFAULT_HIGHEST_BIDDER
                : "Người đang giữ giá: User #" + highestBidderId;
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

            if (noticeObj.has("newEndTime")) {
                handleAuctionExtended(noticeObj.getString("newEndTime"));
            } else {
                showInfo("Có người vừa ra giá mới!");
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
                handleSuccessfulBid(responseObj);
            } else {
                showError(responseObj.optString("message", "Đặt giá thất bại."));
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

        BigDecimal minimumBid = currentPrice.add(stepPrice);
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