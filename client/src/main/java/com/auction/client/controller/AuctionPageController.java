package com.auction.client.controller;

import java.io.*;
import java.net.Socket;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import java.io.IOException;

public class AuctionPageController {

    // Khai báo các thành phần giao diện (phải khớp chính xác tên fx:id trong FXML)
    @FXML
    private Label productNameLabel;

    @FXML
    private Label currentPriceLabel;

    @FXML
    private Label endTimeLabel;

    @FXML
    private TextField bidAmountField;

    @FXML
    private Button placeBidBtn;

    @FXML
    private Label messageLabel;

    @FXML
    private Label remainingTimeLabel;

//    private Socket socket;
//    private PrintWriter out;
//    private BufferedReader in;
//    private String serverAddress = "localhost";
//    private int port = 8080;

    // Hàm khởi tạo (chạy ngay sau khi FXML được load)
    @FXML
    public void initialize() {
        // Bạn có thể set dữ liệu test ở đây
        productNameLabel.setText("Sản phẩm: Loading...");
        currentPriceLabel.setText("Giá cao nhất hiện tại: Loading...");
        endTimeLabel.setText("Thời gian kết thúc: Loading...");
    }

    private Timeline timeline;
    public void setRemainingTime(String endTimeStr) {
        LocalDateTime timeEnd = LocalDateTime.parse(endTimeStr);

        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            LocalDateTime timeNow = LocalDateTime.now();
            long secondsLeft = Duration.between(timeNow, timeEnd).getSeconds();

            if (secondsLeft <= 0) {
                timeline.stop();
                remainingTimeLabel.setText("Phiên đấu giá đã kết thúc!");
                handleAuctionEnd(); // Gọi hàm khóa nút "Đặt giá" hoặc cập nhật CSD
            }
            long hours = secondsLeft / 3600;
            long minutes = (secondsLeft % 3600) / 60;
            long seconds = secondsLeft % 60;

            remainingTimeLabel.setText(String.format("Thời gian còn lại: %02d:%02d:%02d", hours, minutes, seconds));
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void setItem(JSONObject sessionsObj, JSONObject itemObj) {
        productNameLabel.setText("Sản phẩm: " + itemObj.getString("name"));
        currentPriceLabel.setText("Giá hiện tại: " + sessionsObj.getBigDecimal("currentPrice"));
        endTimeLabel.setText("Thời gian kết thúc: " + sessionsObj.getString("endTime").split("T")[0]);
        setRemainingTime(sessionsObj.getString("endTime"));
    }

    private void handleAuctionEnd() {

    }

    // Sự kiện khi bấm nút "Đặt giá"
    @FXML
    public void handlePlaceBid(ActionEvent event) {
        String inputPrice = bidAmountField.getText();

        if (inputPrice == null || inputPrice.trim().isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Vui lòng nhập mức giá!");
            return;
        }

        try {
            double price = Double.parseDouble(inputPrice);
            // Xử lý logic lưu DB hoặc gửi qua server ở đây

            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Đặt giá thành công: " + price);
            currentPriceLabel.setText("Giá cao nhất hiện tại: " + price + " VNĐ");

        } catch (NumberFormatException e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Mức giá không hợp lệ! Vui lòng nhập số.");
        }
    }

    // Sự kiện khi bấm nút "Quay lại"
    @FXML
    public void handleGoBack(ActionEvent event) {
        try {
            // Đảm bảo bạn có file trang-chu.fxml hoặc đổi lại tên cho đúng
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 500, 400);
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Lỗi khi quay lại trang trước.");
        }
    }
}