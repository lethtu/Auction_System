package com.auction.client.controller;

import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.Duration;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.model.User;
import com.auction.client.Config;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

public class AuctionPageController {
    private static final Logger logger = LoggerFactory.getLogger(AuctionPageController.class);

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
        productNameLabel.setText("Loading...");
        currentPriceLabel.setText("...");
        remainingTimeLabel.setText("00:00:00");
        if (minBidIncrementLabel != null) {
            minBidIncrementLabel.setText("Tăng tối thiểu ₫ 0");
        }
        if (highestBidderLabel != null) {
            highestBidderLabel.setText("Chưa có người đặt giá");
        }
        if (reserveStatusLabel != null) {
            reserveStatusLabel.setText("");
        }
        if (totalBidsLabel != null) {
            totalBidsLabel.setText("0");
        }
        if (watchingCountLabel != null) {
            watchingCountLabel.setText("0");
        }
        if (itemDescriptionLabel != null) {
            itemDescriptionLabel.setText("Chưa có mô tả sản phẩm.");
        }

        // Gắn listeners sau khi scene sẵn sàng
        Platform.runLater(() -> {
            if (currentPriceLabel.getScene() != null) {
                double initialWidth = currentPriceLabel.getScene().getWidth();
                
                // Listener: resize cửa sổ
                currentPriceLabel.getScene().widthProperty().addListener((obs, oldVal, newVal) ->
                    updateResponsiveFonts(newVal.doubleValue())
                );
                
                // Listener: text thay đổi cho giá
                currentPriceLabel.textProperty().addListener((obs, oldVal, newVal) ->
                    updateResponsiveFonts(currentPriceLabel.getScene().getWidth())
                );
                
                // Listener: text thay đổi cho thời gian
                remainingTimeLabel.textProperty().addListener((obs, oldVal, newVal) ->
                    updateResponsiveFonts(currentPriceLabel.getScene().getWidth())
                );
                
                // Trigger lần đầu
                updateResponsiveFonts(initialWidth);
            }
        });
    }

    /**
     * Cập nhật kích thước font cho các thành phần quan trọng dựa theo kích thước cửa sổ và nội dung.
     */
    private void updateResponsiveFonts(double windowWidth) {
        // 1. Xử lý font cho GIÁ (Price)
        String priceText = currentPriceLabel.getText();
        double priceBaseFont = Math.max(22, Math.min(48, windowWidth * 0.034));
        int priceExtraChars = Math.max(0, priceText.length() - 8);
        double priceFont = Math.max(16, priceBaseFont - priceExtraChars * 1.5);

        currentPriceLabel.setStyle(
            "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) priceFont + "px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;"
        );
        highestBidTitleLabel.setStyle(
            "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) Math.max(10, Math.min(14, windowWidth * 0.01)) + "px; -fx-font-weight: 900; -fx-text-fill: #604868;"
        );

        // 2. Xử lý font cho THỜI GIAN (Remaining Time)
        String timeText = remainingTimeLabel.getText();
        // Base font cho time: 14px (600px) -> 22px (1400px)
        double timeBaseFont = Math.max(14, Math.min(22, windowWidth * 0.016));
        // Nếu thông báo kết thúc (dài hơn bình thường) thì giảm size
        int timeExtraChars = Math.max(0, timeText.length() - 8);
        double timeFont = Math.max(12, timeBaseFont - timeExtraChars * 0.8);

        remainingTimeLabel.setStyle(
            "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) timeFont + "px; -fx-font-weight: 900; -fx-text-fill: #e040a0;"
        );
        // Nhãn "ENDING IN" nhỏ hơn số thời gian một chút để tạo sự phân cấp
        double titleFont = Math.max(10, timeFont * 0.7);
        endingInTitleLabel.setStyle(
            "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) titleFont + "px; -fx-font-weight: 900; -fx-text-fill: #7c52aa;"
        );

        // 3. Xử lý font cho START PRICE
        double startPriceFont = Math.max(14, Math.min(20, windowWidth * 0.014));
        startPriceLabel.setStyle(
            "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) startPriceFont + "px; -fx-font-weight: 900; -fx-text-fill: #3d0028;"
        );
        startPriceTitleLabel.setStyle(
            "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) Math.max(8, Math.min(12, windowWidth * 0.008)) + "px; -fx-font-weight: 900; -fx-text-fill: #a02070; -fx-padding: 8 0 0 0;"
        );

        // 4. Xử lý font cho Ô NHẬP GIÁ (Bid Field)
        double fieldFont = Math.max(12, Math.min(24, windowWidth * 0.017));
        bidAmountField.setStyle(
            "-fx-background-color: white; -fx-border-color: #ece2ec; -fx-border-width: 2; " +
            "-fx-border-radius: 999; -fx-padding: 16 16 16 48; " +
            "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) fieldFont + "px; -fx-font-weight: 900;"
        );
    }


    public void setItem(JSONObject sessionsObj, JSONObject itemObj) {
        try {
            this.currentSessionId = sessionsObj.getInt("id");
            applySessionData(sessionsObj, itemObj);
            refreshSessionFromServer();
            connectToServer();
            logger.info("Successfully set item for session ID: {}", currentSessionId);
        } catch (Exception e) {
            logger.error("Error in setItem: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshSessionFromServer() {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/auctions/" + currentSessionId))
                        .GET()
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                    JSONObject session = new JSONObject(response.body());
                    JSONObject item = session.optJSONObject("item");
                    Platform.runLater(() -> applySessionData(session, item));
                }
            } catch (Exception e) {
                logger.warn("Không tải lại được chi tiết phiên {}: {}", currentSessionId, e.getMessage());
            }
        }).start();
    }

    private void applySessionData(JSONObject sessionObj, JSONObject itemObj) {
        this.startingPrice = getMoneyAny(sessionObj, BigDecimal.ZERO, "startingPrice", "startPrice");
        this.currentPrice = getMoneyAny(sessionObj, startingPrice, "currentPrice", "highestBid");
        this.stepPrice = getMoneyAny(sessionObj, BigDecimal.ZERO, "stepPrice", "minBidIncrement");
        this.reservePrice = getMoneyAny(sessionObj, BigDecimal.ZERO, "reservePrice");
        this.highestBidderId = getOptionalInt(sessionObj, "highestBidderId");
        this.bidCount = getBidCount(sessionObj);

        if (currentPrice.compareTo(BigDecimal.ZERO) <= 0 && startingPrice.compareTo(BigDecimal.ZERO) > 0) {
            currentPrice = startingPrice;
        }

        String description;
        if (itemObj != null) {
            productNameLabel.setText(itemObj.optString("name", sessionObj.optString("productName", "Unknown Product")));
            description = itemObj.optString("description", sessionObj.optString("description", ""));
            loadProductImage(itemObj.optString("imagePath", sessionObj.optString("imagePath", "")));
        } else {
            productNameLabel.setText(sessionObj.optString("productName", "Unknown Product"));
            description = sessionObj.optString("description", "");
            loadProductImage(sessionObj.optString("imagePath", ""));
        }

        updateDescription(description);

        currentPriceLabel.setText("₫ " + formatPrice(currentPrice));
        startPriceLabel.setText(startingPrice.compareTo(BigDecimal.ZERO) > 0 ? "₫ " + formatPrice(startingPrice) : "---");
        updateBidInfoLabels();

        String endTimeStr = sessionObj.optString("endTime", "");
        if (!endTimeStr.isEmpty()) {
            setRemainingTime(endTimeStr);
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
            logger.error("Error loading product image from {}: {}", imageUrl, e.getMessage());
        }
    }

    private void updateBidInfoLabels() {
        if (minBidIncrementLabel != null) {
            minBidIncrementLabel.setText("Tăng tối thiểu ₫ " + formatPrice(stepPrice));
        }

        if (highestBidderLabel != null) {
            highestBidderLabel.setText(
                    highestBidderId == null
                            ? "Chưa có người đặt giá"
                            : "Người đang giữ giá: User #" + highestBidderId
            );
        }

        if (reserveStatusLabel != null) {
            if (reservePrice == null || reservePrice.compareTo(BigDecimal.ZERO) <= 0) {
                reserveStatusLabel.setText("");
            } else if (currentPrice.compareTo(reservePrice) >= 0) {
                reserveStatusLabel.setText("Giá sàn đã đạt");
            } else {
                reserveStatusLabel.setText("Giá sàn chưa đạt");
            }
        }

        if (totalBidsLabel != null) {
            totalBidsLabel.setText(String.valueOf(Math.max(0, bidCount)));
        }

        if (watchingCountLabel != null) {
            watchingCountLabel.setText(String.valueOf(Math.max(0, watchingCount)));
        }
    }

    private void connectToServer() {
        listenerThread = new Thread(() -> {
            try {
                socket = new Socket(Config.SOCKET_HOST, Config.PORT_SOCKET);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("JOIN:" + currentSessionId);
                String serverResponse;

                while (!socket.isClosed() && (serverResponse = in.readLine()) != null) {

                    // 1. NHẬN THÔNG BÁO CHUNG CHO TOÀN PHÒNG (CÓ NGƯỜI ĐẶT GIÁ MỚI)
                    if (serverResponse.startsWith("NOTICE:")) {
                        String jsonString = serverResponse.substring(7);
                        JSONObject noticeObj = new JSONObject(jsonString);

                        Platform.runLater(() -> {
                            // Cập nhật giá mới cho biến toàn cục để Validate các lần sau
                            BigDecimal newPrice = noticeObj.getBigDecimal("newPrice");
                            this.currentPrice = newPrice;

                            currentPriceLabel.setText("₫ " + formatPrice(newPrice));
                            this.highestBidderId = getOptionalInt(noticeObj, "highestBidderId");
                            if (noticeObj.has("bidCount") && !noticeObj.isNull("bidCount")) {
                                this.bidCount = noticeObj.optInt("bidCount", bidCount);
                            }
                            updateBidInfoLabels();

                            // ==========================================
                            // BẮT SỰ KIỆN ANTI-SNIPING Ở CLIENT
                            // ==========================================
                            if (noticeObj.has("newEndTime")) {
                                String newEndTime = noticeObj.getString("newEndTime");

                                // XỬ LÝ CHUỖI AN TOÀN CHỐNG LỖI THIẾU GIÂY
                                String displayTime = newEndTime.replace("T", " ");

                                // Đổi màu thông báo sang cam rực rỡ để gây chú ý
                                messageLabel.setStyle("-fx-text-fill: #ff8c00; -fx-font-weight: bold;");
                                messageLabel.setText("Phiên đấu giá vừa được gia hạn thêm 60 giây!");

                                // Xóa đồng hồ đếm ngược cũ và khởi động lại với thời gian mới!
                                if (timeline != null) {
                                    timeline.stop();
                                }
                                setRemainingTime(newEndTime);
                            } else {
                                // Nếu chỉ đổi giá bình thường (không gia hạn)
                                messageLabel.setStyle("-fx-text-fill: blue;");
                                messageLabel.setText("Có người vừa ra giá mới!");
                            }
                        });
                    }

                    // 2. NHẬN KẾT QUẢ ĐẶT GIÁ CỦA CHÍNH MÌNH
                    else if (serverResponse.startsWith("RESPONSE:")) {
                        String jsonString = serverResponse.substring(9);
                        JSONObject responseObj = new JSONObject(jsonString);

                        Platform.runLater(() -> {
                            finishBidProcessing();
                            if (responseObj.getBoolean("success")) {
                                currentPrice = getMoney(responseObj, "currentPrice", currentPrice);
                                highestBidderId = getOptionalInt(responseObj, "highestBidderId");
                                if (responseObj.has("bidCount") && !responseObj.isNull("bidCount")) {
                                    bidCount = responseObj.optInt("bidCount", bidCount);
                                }
                                currentPriceLabel.setText("₫ " + formatPrice(currentPrice));
                                updateBidInfoLabels();
                                messageLabel.setStyle("-fx-text-fill: green;");
                                messageLabel.setText(responseObj.getString("message"));
                                bidAmountField.clear();
                            } else {
                                messageLabel.setStyle("-fx-text-fill: red;");
                                messageLabel.setText(responseObj.getString("message"));
                            }
                        });
                    } else if (serverResponse.startsWith("ROOM_COUNT:")) {
                        int count = Integer.parseInt(serverResponse.substring(11));
                        Platform.runLater(() -> {
                            watchingCount = count;
                            updateBidInfoLabels();
                        });
                    }
                }
            } catch (EOFException | java.net.SocketException e) {
                System.out.println("Kết nối Socket đã đóng.");
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    finishBidProcessing();
                    messageLabel.setStyle("-fx-text-fill: red;");
                    messageLabel.setText("Mất kết nối với máy chủ Socket!");
                });
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void disconnectSocket() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        // 1. Kiểm tra đăng nhập
        if (User.getId() == null) {
            showError("Vui lòng đăng nhập để đấu giá!");
            return;
        }

        // 2. Lấy và kiểm tra định dạng giá nhập vào
        String inputStr = bidAmountField.getText().trim();
        if (inputStr.isEmpty()) {
            showError("Vui lòng nhập mức giá!");
            return;
        }

        BigDecimal bidAmount;
        try {
            bidAmount = parseMoneyInput(inputStr);
        } catch (NumberFormatException e) {
            showError("Mức giá phải là con số hợp lệ!");
            return;
        }

        BigDecimal minimumBid = this.currentPrice.add(this.stepPrice);
        if (bidAmount.compareTo(minimumBid) < 0) {
            showError("Giá đặt tối thiểu là ₫ " + formatPrice(minimumBid) + "!");
            return;
        }

        // 4. Gửi lên server nếu các bước trên đã pass
        if (socket == null || socket.isClosed() || out == null) {
            showError("Lỗi kết nối máy chủ Socket!");
            return;
        }

        JSONObject jsonBid = new JSONObject();
        jsonBid.put("auctionId", currentSessionId);
        jsonBid.put("bidderId", User.getId());
        jsonBid.put("amount", bidAmount);

        out.println("BID:" + jsonBid.toString());

        placeBidBtn.setDisable(true);
        messageLabel.setStyle("-fx-text-fill: orange;");
        messageLabel.setText("Đang xử lý yêu cầu...");
        startBidTimeout();
    }


    private void startBidTimeout() {
        if (bidTimeout != null) {
            bidTimeout.stop();
        }

        bidTimeout = new Timeline(new KeyFrame(javafx.util.Duration.seconds(8), event -> {
            placeBidBtn.setDisable(false);
            if ("Đang xử lý yêu cầu...".equals(messageLabel.getText())) {
                messageLabel.setStyle("-fx-text-fill: orange;");
                messageLabel.setText("Máy chủ phản hồi hơi lâu, kiểm tra kết nối hoặc thử lại.");
            }
        }));
        bidTimeout.setCycleCount(1);
        bidTimeout.play();
    }

    private void finishBidProcessing() {
        if (bidTimeout != null) {
            bidTimeout.stop();
        }
        if (placeBidBtn != null) {
            placeBidBtn.setDisable(false);
        }
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText(msg);
    }

    @FXML
    private void handleToggleSidebar(ActionEvent event) {
        boolean isCollapsed = sideBar.getPrefWidth() <= 70;
        
        if (isCollapsed) {
            // Expand
            sideBar.setPrefWidth(200);
            sideBar.setMaxWidth(200);
            
            mainMenuLabel.setVisible(true); mainMenuLabel.setManaged(true);
            dashboardText.setVisible(true); dashboardText.setManaged(true);
            liveAuctionsText.setVisible(true); liveAuctionsText.setManaged(true);
            myBidsText.setVisible(true); myBidsText.setManaged(true);
            sellingText.setVisible(true); sellingText.setManaged(true);
            
            discoverLabel.setVisible(true); discoverLabel.setManaged(true);
            categoriesText.setVisible(true); categoriesText.setManaged(true);
            activeBidsText.setVisible(true); activeBidsText.setManaged(true);
            watchlistText.setVisible(true); watchlistText.setManaged(true);
            endedSoonText.setVisible(true); endedSoonText.setManaged(true);
            
            otherLabel.setVisible(true); otherLabel.setManaged(true);
            supportText.setVisible(true); supportText.setManaged(true);
            
            startSellingText.setVisible(true); startSellingText.setManaged(true);
        } else {
            // Collapse
            sideBar.setPrefWidth(70);
            sideBar.setMaxWidth(70);
            
            mainMenuLabel.setVisible(false); mainMenuLabel.setManaged(false);
            dashboardText.setVisible(false); dashboardText.setManaged(false);
            liveAuctionsText.setVisible(false); liveAuctionsText.setManaged(false);
            myBidsText.setVisible(false); myBidsText.setManaged(false);
            sellingText.setVisible(false); sellingText.setManaged(false);
            
            discoverLabel.setVisible(false); discoverLabel.setManaged(false);
            categoriesText.setVisible(false); categoriesText.setManaged(false);
            activeBidsText.setVisible(false); activeBidsText.setManaged(false);
            watchlistText.setVisible(false); watchlistText.setManaged(false);
            endedSoonText.setVisible(false); endedSoonText.setManaged(false);
            
            otherLabel.setVisible(false); otherLabel.setManaged(false);
            supportText.setVisible(false); supportText.setManaged(false);
            
            startSellingText.setVisible(false); startSellingText.setManaged(false);
        }
    }

    @FXML
    private void handleStartSelling(ActionEvent event) {
        logger.info("Start selling clicked in auction room");
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        disconnectSocket();

        try {
            SceneSwitcher.switchScene(event, "MainTemplate.fxml");
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Lỗi khi quay lại trang trước.");
        }
    }

    // ================== LOGIC THỜI GIAN ================== //

    public void setRemainingTime(String endTimeStr) {
        if (timeline != null) {
            timeline.stop();
        }

        LocalDateTime timeEnd = LocalDateTime.parse(endTimeStr);

        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            LocalDateTime timeNow = LocalDateTime.now();
            long secondsLeft = Duration.between(timeNow, timeEnd).getSeconds();

            if (secondsLeft <= 0) {
                timeline.stop();
                remainingTimeLabel.setText("Phiên đấu giá đã kết thúc!");
                handleAuctionEnd();
            } else {
                long hours = secondsLeft / 3600;
                long minutes = (secondsLeft % 3600) / 60;
                long seconds = secondsLeft % 60;
                remainingTimeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void handleAuctionEnd() {
        placeBidBtn.setDisable(true);
        bidAmountField.setDisable(true);
        disconnectSocket();
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
        if (itemDescriptionLabel == null) {
            return;
        }
        if (description == null || description.isBlank()) {
            itemDescriptionLabel.setText("Chưa có mô tả sản phẩm.");
        } else {
            itemDescriptionLabel.setText(description.trim());
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