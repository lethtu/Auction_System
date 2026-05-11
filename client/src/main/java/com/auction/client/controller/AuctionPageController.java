package com.auction.client.controller;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.Duration;

import org.json.JSONObject;
import com.auction.client.model.User;
import com.auction.client.Config;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

public class AuctionPageController {

    @FXML private Label productNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label endTimeLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidBtn;
    @FXML private Label messageLabel;
    @FXML private Label remainingTimeLabel;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;

    private int currentSessionId;
    private BigDecimal currentPrice;
    private int currentUserId;

    private Timeline timeline;

    @FXML
    public void initialize() {
        productNameLabel.setText("Sản phẩm: Loading...");
        currentPriceLabel.setText("Giá cao nhất hiện tại: Loading...");
        endTimeLabel.setText("Thời gian kết thúc: Loading...");
    }

    public void setItem(JSONObject sessionsObj, JSONObject itemObj) {
        this.currentSessionId = sessionsObj.getInt("id");
        this.currentPrice = sessionsObj.getBigDecimal("currentPrice");

        productNameLabel.setText("Sản phẩm: " + itemObj.getString("name"));
        currentPriceLabel.setText("Giá hiện tại: " + String.format("%,.0f", currentPrice) + " VNĐ");

        String endTimeStr = sessionsObj.getString("endTime");
        endTimeLabel.setText("Thời gian kết thúc: " + endTimeStr.replace("T", " ").substring(0, 16));

        setRemainingTime(endTimeStr);
        connectToServer();
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

                    if (serverResponse.startsWith("NOTICE:")) {
                        String jsonString = serverResponse.substring(7);
                        JSONObject noticeObj = new JSONObject(jsonString);

                        Platform.runLater(() -> {
                            BigDecimal newPrice = noticeObj.getBigDecimal("newPrice");
                            this.currentPrice = newPrice;

                            currentPriceLabel.setText("Giá cao nhất hiện tại: " + String.format("%,.0f", newPrice) + " VNĐ");

                            // LOGIC ANTI-SNIPING (ĐỒNG HỒ NẢY LÊN)
                            if (noticeObj.has("newEndTime")) {
                                String newEndTime = noticeObj.getString("newEndTime");

                                // ==========================================
                                // XỬ LÝ CHUỖI AN TOÀN CHỐNG LỖI THIẾU GIÂY
                                // ==========================================
                                String displayTime = newEndTime.replace("T", " ");
                                if (displayTime.length() == 16) {
                                    displayTime += ":00"; // Bù thêm :00 nếu Java tự động cắt mất
                                } else if (displayTime.length() > 19) {
                                    displayTime = displayTime.substring(0, 19);
                                }

                                endTimeLabel.setText("Thời gian kết thúc: " + displayTime);
                                messageLabel.setStyle("-fx-text-fill: #ff8c00; -fx-font-weight: bold;");
                                messageLabel.setText("Phiên đấu giá vừa được gia hạn thêm 60 giây!");

                                if (timeline != null) timeline.stop();
                                setRemainingTime(newEndTime);
                            } else {
                                messageLabel.setStyle("-fx-text-fill: blue;");
                                messageLabel.setText("Có người vừa ra giá mới!");
                            }
                        });
                    }
                    else if (serverResponse.startsWith("RESPONSE:")) {
                        String jsonString = serverResponse.substring(9);
                        JSONObject responseObj = new JSONObject(jsonString);

                        Platform.runLater(() -> {
                            if (responseObj.getBoolean("success")) {
                                messageLabel.setStyle("-fx-text-fill: green;");
                                messageLabel.setText(responseObj.getString("message"));
                                bidAmountField.clear();
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
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        if (User.getId() == null) {
            showError("Vui lòng đăng nhập để đấu giá!");
            return;
        }

        String inputStr = bidAmountField.getText().trim();
        if (inputStr.isEmpty()) {
            showError("Vui lòng nhập mức giá!");
            return;
        }

        BigDecimal bidAmount;
        try { bidAmount = new BigDecimal(inputStr); }
        catch (NumberFormatException e) { showError("Mức giá phải là con số hợp lệ!"); return; }

        if (bidAmount.compareTo(this.currentPrice) <= 0) {
            showError("Giá đặt phải LỚN HƠN giá hiện tại (" + String.format("%,.0f", this.currentPrice) + ")!");
            return;
        }

        if (socket == null || socket.isClosed() || out == null) {
            showError("Lỗi kết nối máy chủ Socket!");
            return;
        }

        JSONObject jsonBid = new JSONObject();
        jsonBid.put("auctionId", currentSessionId);
        jsonBid.put("bidderId", User.getId());
        jsonBid.put("amount", bidAmount);

        out.println("BID:" + jsonBid.toString());
        messageLabel.setStyle("-fx-text-fill: orange;");
        messageLabel.setText("Đang xử lý yêu cầu...");
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText(msg);
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        disconnectSocket();
        try { SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1024, 768); }
        catch (IOException e) { e.printStackTrace(); messageLabel.setText("Lỗi khi quay lại trang trước."); }
    }

    public void setRemainingTime(String endTimeStr) {
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
                remainingTimeLabel.setText(String.format("Thời gian còn lại: %02d:%02d:%02d", hours, minutes, seconds));
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
}