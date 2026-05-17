package com.auction.client.controller;

import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.net.Socket;
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
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.control.Separator;
import javafx.animation.KeyFrame;
import javafx.scene.text.Font;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;

public class AuctionPageController {
    private static final Logger logger = LoggerFactory.getLogger(AuctionPageController.class);

    @FXML private MenuButton userMenuButton;
    @FXML private Label productNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidBtn;
    @FXML private Label messageLabel;
    @FXML private Label remainingTimeLabel;
    @FXML private Label startPriceLabel;
    @FXML private ImageView productImageView;
    @FXML private SidebarController sidebarController;
    @FXML private Label endingInTitleLabel;
    @FXML private Label startPriceTitleLabel;
    @FXML private Label highestBidTitleLabel;

    @FXML private Label totalBidsLabel;
    @FXML private Label watchingLabel;
    @FXML private Label itemDescriptionLabel;
    @FXML private Label minIncrementLabel;
    @FXML private VBox chartContainer;
    @FXML private VBox bidHistoryContainer;

    private final java.util.List<BidHistoryItem> bidHistoryData = new java.util.ArrayList<>();
    private javafx.scene.chart.AreaChart<Number, Number> areaChart;
    private javafx.scene.chart.XYChart.Series<Number, Number> priceSeries;
    private int tickCount = 0;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;

    private int currentSessionId;
    private BigDecimal currentPrice;
    private int currentUserId;
    private BigDecimal sessionStepPrice;
    private BigDecimal myLastBidAmount = null;

    private Timeline timeline;

    private static class BidHistoryItem {
        private final BigDecimal amount;
        private final String bidderName;
        private final String timeAgo;

        public BidHistoryItem(BigDecimal amount, String bidderName, String timeAgo) {
            this.amount = amount;
            this.bidderName = bidderName;
            this.timeAgo = timeAgo;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getBidderName() {
            return bidderName;
        }

        public String getTimeAgo() {
            return timeAgo;
        }
    }
    @FXML
    public void initialize() {
        createUserOption("Avatar");
        productNameLabel.setText("Loading...");
        currentPriceLabel.setText("...");
        remainingTimeLabel.setText("00:00:00");

        if (sidebarController != null) {
            sidebarController.setOnBeforeNavigate(this::disconnectSocket);
            sidebarController.forceCollapse();
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
            this.currentPrice = sessionsObj.optBigDecimal("currentPrice", BigDecimal.ZERO);
            
            if (sessionsObj.has("stepPrice") && !sessionsObj.isNull("stepPrice")) {
                this.sessionStepPrice = sessionsObj.getBigDecimal("stepPrice");
            } else {
                this.sessionStepPrice = null;
            }

            productNameLabel.setText(itemObj.optString("name", "Unknown Product"));
            currentPriceLabel.setText("₫ " + formatPrice(currentPrice));
            
            if (sessionsObj.has("startingPrice") && !sessionsObj.isNull("startingPrice")) {
                startPriceLabel.setText("₫ " + formatPrice(sessionsObj.getBigDecimal("startingPrice")));
            } else {
                startPriceLabel.setText("---");
            }

            updateMinIncrementLabel();

            itemDescriptionLabel.setText(itemObj.optString("description", "No description available."));

            int totalBids = 0;
            if (sessionsObj.has("totalBids") && !sessionsObj.isNull("totalBids")) {
                totalBids = sessionsObj.getInt("totalBids");
            } else if (sessionsObj.has("bids") && !sessionsObj.isNull("bids")) {
                totalBids = sessionsObj.getJSONArray("bids").length();
            }
            totalBidsLabel.setText(formatPrice(new BigDecimal(totalBids)));

            watchingLabel.setText("1");

            loadBidHistoryData(sessionsObj);
            initChart();
            renderBidHistoryUI();
            renderLast3BidsToChart();

            String imagePath = itemObj.optString("imagePath", "");
            if (!imagePath.isEmpty()) {
                String imageUrl = Config.API_URL + "/api/files/images/" + imagePath;
                try {
                    productImageView.setImage(new Image(imageUrl, true));
                } catch (Exception e) {
                    logger.error("Error loading product image from {}: {}", imageUrl, e.getMessage());
                }
            }

            String endTimeStr = sessionsObj.optString("endTime", "");
            if (!endTimeStr.isEmpty()) {
                setRemainingTime(endTimeStr);
            }

            connectToServer();
            logger.info("Successfully set item for session ID: {}", currentSessionId);
        } catch (Exception e) {
            logger.error("Error in setItem: {}", e.getMessage());
            e.printStackTrace();
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

                    // 0. CẬP NHẬT SỐ LƯỢNG NGƯỜI XEM (WATCHING)
                    if (serverResponse.startsWith("WATCHING:")) {
                        String jsonString = serverResponse.substring(9);
                        JSONObject noticeObj = new JSONObject(jsonString);
                        Platform.runLater(() -> {
                            watchingLabel.setText(String.valueOf(noticeObj.optInt("watchingCount", 1)));
                        });
                    }

                    // 1. NHẬN THÔNG BÁO CHUNG CHO TOÀN PHÒNG (CÓ NGƯỜI ĐẶT GIÁ MỚI)
                    else if (serverResponse.startsWith("NOTICE:")) {
                        String jsonString = serverResponse.substring(7);
                        JSONObject noticeObj = new JSONObject(jsonString);

                        Platform.runLater(() -> {
                            // Cập nhật giá mới cho biến toàn cục để Validate các lần sau
                            BigDecimal newPrice = noticeObj.getBigDecimal("newPrice");
                            this.currentPrice = newPrice;

                            currentPriceLabel.setText("₫ " + formatPrice(newPrice));
                            updateMinIncrementLabel();

                            int bidderId = noticeObj.optInt("bidderId", 0);
                            boolean isMyBid = (User.getId() != null && bidderId == User.getId()) || newPrice.equals(myLastBidAmount);
                            String bidderName = noticeObj.optString("bidderName", "");
                            if (bidderName.isEmpty()) {
                                if (isMyBid) {
                                    bidderName = "Bạn (You)";
                                } else if (bidderId > 0) {
                                    bidderName = "Bidder #" + bidderId;
                                } else {
                                    bidderName = "Bidder Ẩn Danh";
                                }
                            }

                            bidHistoryData.add(
                                new BidHistoryItem(newPrice, bidderName, "Vừa xong")
                            );

                            renderBidHistoryUI();
                            renderLast3BidsToChart();
                            increaseTotalBidCount();

                            // ==========================================
                            // BẮT SỰ KIỆN ANTI-SNIPING Ở CLIENT
                            // ==========================================
                            if (noticeObj.has("newEndTime") && !noticeObj.isNull("newEndTime")) {
                                String newEndTime = noticeObj.getString("newEndTime");

                                // XỬ LÝ CHUỖI AN TOÀN CHỐNG LỖI THIẾU GIÂY
                                String displayTime = newEndTime.replace("T", " ");

                                if (isMyBid) {
                                    messageLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: 900; -fx-background-color: #d1fae5; -fx-padding: 8 16; -fx-background-radius: 8;");
                                    messageLabel.setText("Bạn đang dẫn đầu! (Phiên vừa gia hạn)");
                                } else {
                                    messageLabel.setStyle("-fx-text-fill: #624bff; -fx-font-weight: bold; -fx-background-color: #ffedd5; -fx-padding: 8 16; -fx-background-radius: 8;");
                                    messageLabel.setText("Phiên đấu giá vừa được gia hạn thêm 60 giây!");
                                }

                                // Xóa đồng hồ đếm ngược cũ và khởi động lại với thời gian mới!
                                if (timeline != null) {
                                    timeline.stop();
                                }
                                setRemainingTime(newEndTime);
                            } else {
                                // Nếu chỉ đổi giá bình thường (không gia hạn)
                                if (isMyBid) {
                                    messageLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: 900; -fx-background-color: #d1fae5; -fx-padding: 8 16; -fx-background-radius: 8;");
                                    messageLabel.setText("Bạn đang dẫn đầu với giá ₫ " + formatPrice(newPrice) + "!");
                                } else {
                                    messageLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-background-color: #dbeafe; -fx-padding: 8 16; -fx-background-radius: 8;");
                                    messageLabel.setText("Có người vừa ra giá mới!");
                                }
                            }
                        });
                    }

                    // 2. NHẬN KẾT QUẢ ĐẶT GIÁ CỦA CHÍNH MÌNH
                    else if (serverResponse.startsWith("RESPONSE:")) {
                        String jsonString = serverResponse.substring(9);
                        JSONObject responseObj = new JSONObject(jsonString);

                        Platform.runLater(() -> {
                            if (responseObj.getBoolean("success")) {
                                messageLabel.setStyle("-fx-text-fill: green;");
                                messageLabel.setText(responseObj.getString("message"));
                                bidAmountField.clear(); // Xóa ô nhập sau khi đặt thành công
                            } else {
                                messageLabel.setStyle("-fx-text-fill: red;");
                                messageLabel.setText(responseObj.getString("message"));
                            }
                        });
                    }
                }
            } catch (EOFException | java.net.SocketException e) {
                System.out.println("Kết nối Socket đã đóng.");
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
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
            bidAmount = new BigDecimal(inputStr);
        } catch (NumberFormatException e) {
            showError("Mức giá phải là con số hợp lệ!");
            return;
        }

        if (bidAmount.compareTo(this.currentPrice) <= 0) {
            showError("Giá đặt phải LỚN HƠN giá hiện tại (₫ " + formatPrice(this.currentPrice) + ")!");
            return;
        }

        BigDecimal minRequired;
        if (this.sessionStepPrice != null && this.sessionStepPrice.compareTo(BigDecimal.ZERO) > 0) {
            minRequired = this.currentPrice.add(this.sessionStepPrice);
        } else {
            BigDecimal increment;
            if (currentPrice.compareTo(new BigDecimal("100000")) < 0) {
                increment = new BigDecimal("10000");
            } else if (currentPrice.compareTo(new BigDecimal("500000")) < 0) {
                increment = new BigDecimal("20000");
            } else if (currentPrice.compareTo(new BigDecimal("1000000")) < 0) {
                increment = new BigDecimal("50000");
            } else if (currentPrice.compareTo(new BigDecimal("5000000")) < 0) {
                increment = new BigDecimal("100000");
            } else {
                increment = new BigDecimal("200000");
            }
            minRequired = this.currentPrice.add(increment);
        }

        if (bidAmount.compareTo(minRequired) < 0) {
            showError("Giá đặt hợp lệ phải từ ₫ " + formatPrice(minRequired) + " trở lên!");
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

        this.myLastBidAmount = bidAmount; // Lưu lại để detect khi nhận NOTICE

        out.println("BID:" + jsonBid.toString());

        messageLabel.setStyle("-fx-text-fill: orange;");
        messageLabel.setText("Đang xử lý yêu cầu...");
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText(msg);
    }

    @FXML
    private void handleToggleSidebar(ActionEvent event) {
        if (sidebarController != null) {
            sidebarController.toggleSidebar();
        }
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
            SceneSwitcher.switchScene(event, "MainTemplate.fxml");
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Lỗi khi quay lại trang trước.");
        }
    }

    private void initChart() {
        javafx.scene.chart.NumberAxis xAxis = new javafx.scene.chart.NumberAxis();
        xAxis.setTickLabelsVisible(false);
        xAxis.setMinorTickVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setOpacity(0);

        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        yAxis.setTickLabelsVisible(false);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickMarkVisible(false);
        yAxis.setOpacity(0);
        yAxis.setAutoRanging(true);

        areaChart = new javafx.scene.chart.AreaChart<>(xAxis, yAxis);
        areaChart.setLegendVisible(false);
        areaChart.setCreateSymbols(true);
        areaChart.setAnimated(true);
        areaChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");

        priceSeries = new javafx.scene.chart.XYChart.Series<>();
        priceSeries.setName("Bid Trajectory");
        areaChart.getData().add(priceSeries);

        Platform.runLater(() -> {
            try {
                areaChart.lookup(".default-color0.chart-series-area-line").setStyle("-fx-stroke: #e040a0; -fx-stroke-width: 3px;");
                areaChart.lookup(".default-color0.chart-series-area-fill").setStyle("-fx-fill: linear-gradient(to bottom, rgba(224,64,160,0.3) 0%, rgba(224,64,160,0) 100%);");
            } catch (Exception e) {}
        });

        chartContainer.getChildren().clear();
        chartContainer.getChildren().add(areaChart);
        
        tickCount = 0;
    }

    private void loadBidHistoryData(JSONObject sessionsObj) {
        bidHistoryData.clear();

        if (!sessionsObj.has("bids") || sessionsObj.isNull("bids")) {
            return;
        }

        org.json.JSONArray bidsArray = sessionsObj.getJSONArray("bids");

        for (int i = 0; i < bidsArray.length(); i++) {
            JSONObject bid = bidsArray.getJSONObject(i);

            BigDecimal amount = bid.getBigDecimal("amount");

            String timeAgo = "Vừa xong";
            if (bid.has("time") && !bid.isNull("time")) {
                timeAgo = calculateTimeAgo(bid.getString("time"));
            }

            String bidderName = "Bidder #Unknown";
            if (bid.has("bidder") && !bid.isNull("bidder")) {
                JSONObject bidderObj = bid.getJSONObject("bidder");
                int bidderId = bidderObj.optInt("id", 0);
                if (User.getId() != null && bidderId == User.getId()) {
                    bidderName = "Bạn (You)";
                } else {
                    bidderName = "Bidder #" + bidderId;
                }
            }

            bidHistoryData.add(new BidHistoryItem(amount, bidderName, timeAgo));
        }
    }

    private void renderBidHistoryUI() {
        bidHistoryContainer.getChildren().clear();

        int start = Math.max(0, bidHistoryData.size() - 3);

        for (int i = bidHistoryData.size() - 1; i >= start; i--) {
            BidHistoryItem item = bidHistoryData.get(i);
            addBidRowToHistoryUI(
                item.getBidderName(),
                item.getAmount(),
                item.getTimeAgo()
            );
        }
    }

    private void increaseTotalBidCount() {
        try {
            String cleanTotal = totalBidsLabel.getText().replace(".", "");
            int currentTotal = Integer.parseInt(cleanTotal);
            totalBidsLabel.setText(formatPrice(new BigDecimal(currentTotal + 1)));
        } catch (Exception e) {
            logger.warn("Cannot increase total bid count", e);
        }
    }

    private String calculateTimeAgo(String timeStr) {
        try {
            LocalDateTime past = LocalDateTime.parse(timeStr);
            LocalDateTime now = LocalDateTime.now();
            long seconds = Duration.between(past, now).getSeconds();
            if (seconds < 60) return seconds + " seconds ago";
            long minutes = seconds / 60;
            if (minutes < 60) return minutes + " minutes ago";
            long hours = minutes / 60;
            if (hours < 24) return hours + " hours ago";
            return (hours / 24) + " days ago";
        } catch (Exception e) {
            return "Past";
        }
    }

    private void updateMinIncrementLabel() {
        if (this.currentPrice == null) return;
        BigDecimal increment;
        if (this.sessionStepPrice != null && this.sessionStepPrice.compareTo(BigDecimal.ZERO) > 0) {
            increment = this.sessionStepPrice;
        } else {
            if (currentPrice.compareTo(new BigDecimal("100000")) < 0) {
                increment = new BigDecimal("10000");
            } else if (currentPrice.compareTo(new BigDecimal("500000")) < 0) {
                increment = new BigDecimal("20000");
            } else if (currentPrice.compareTo(new BigDecimal("1000000")) < 0) {
                increment = new BigDecimal("50000");
            } else if (currentPrice.compareTo(new BigDecimal("5000000")) < 0) {
                increment = new BigDecimal("100000");
            } else {
                increment = new BigDecimal("200000");
            }
        }
        minIncrementLabel.setText("Tăng tối thiểu ₫ " + formatPrice(increment));
    }

    private void addBidToHistory(String bidderName, BigDecimal amount, String timeAgo) {
        Platform.runLater(() -> {
            javafx.scene.layout.HBox historyRow = new javafx.scene.layout.HBox();
            historyRow.setAlignment(javafx.geometry.Pos.CENTER);
            historyRow.setSpacing(10);
            
            Label nameLabel = new Label(bidderName);
            nameLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2e1a28;");
            
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            
            Label amountLabel = new Label("₫ " + formatPrice(amount));
            amountLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;");
            amountLabel.setPrefWidth(95);
            amountLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            
            Label timeLabel = new Label(timeAgo);
            timeLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 10px; -fx-text-fill: #604868;");
            timeLabel.setPrefWidth(65);
            timeLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            
            historyRow.getChildren().addAll(nameLabel, spacer, amountLabel, timeLabel);
            
            if (bidHistoryContainer.getChildren().size() >= 3) {
                bidHistoryContainer.getChildren().remove(bidHistoryContainer.getChildren().size() - 1);
            }
            bidHistoryContainer.getChildren().add(0, historyRow);
            
            if (priceSeries != null && timeAgo.equals("Vừa xong")) {
                priceSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(tickCount++, amount.doubleValue()));
                
                try {
                    String cleanTotal = totalBidsLabel.getText().replace(".", "");
                    int currentTotal = Integer.parseInt(cleanTotal);
                    totalBidsLabel.setText(formatPrice(new BigDecimal(currentTotal + 1)));
                } catch (Exception e) {}
            }
        });
    }

    private void addBidRowToHistoryUI(String bidderName, BigDecimal amount, String timeAgo) {
        javafx.scene.layout.HBox historyRow = new javafx.scene.layout.HBox();
        historyRow.setAlignment(javafx.geometry.Pos.CENTER);
        historyRow.setSpacing(10);

        Label nameLabel = new Label(bidderName);
        nameLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2e1a28;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label amountLabel = new Label("₫ " + formatPrice(amount));
        amountLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;");
        amountLabel.setPrefWidth(95);
        amountLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label timeLabel = new Label(timeAgo);
        timeLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 10px; -fx-text-fill: #604868;");
        timeLabel.setPrefWidth(65);
        timeLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        historyRow.getChildren().addAll(nameLabel, spacer, amountLabel, timeLabel);
        bidHistoryContainer.getChildren().add(historyRow);
    }

    private void renderLast3BidsToChart() {
        if (priceSeries == null) return;

        priceSeries.getData().clear();
        tickCount = 0;

        int start = Math.max(0, bidHistoryData.size() - 3);

        for (int i = start; i < bidHistoryData.size(); i++) {
            BidHistoryItem item = bidHistoryData.get(i);
            priceSeries.getData().add(
                new javafx.scene.chart.XYChart.Data<>(
                    tickCount++,
                    item.getAmount().doubleValue()
                )
            );
        }

        // Nếu chưa có bid nào thì hiển thị current price
        if (bidHistoryData.isEmpty() && currentPrice != null) {
            priceSeries.getData().add(
                new javafx.scene.chart.XYChart.Data<>(
                    tickCount++,
                    currentPrice.doubleValue()
                )
            );
        }
    }
    // ================== LOGIC THỜI GIAN ================== //

    public void setRemainingTime(String endTimeStr) {
        // Parse chuẩn định dạng ISO từ JSON gửi về (VD: "2026-05-15T20:00:00")
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
    private String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("###,###", symbols);
        return df.format(price);
    }
}