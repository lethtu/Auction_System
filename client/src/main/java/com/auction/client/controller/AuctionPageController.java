package com.auction.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

public class AuctionPageController {

    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

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

    private Timeline timeline;

    @FXML
    public void initialize() {
        productNameLabel.setText("Sản phẩm: Loading...");
        currentPriceLabel.setText("Giá cao nhất hiện tại: Loading...");
        endTimeLabel.setText("Thời gian kết thúc: Loading...");

        if (remainingTimeLabel != null) {
            remainingTimeLabel.setText("Thời gian còn lại: Loading...");
        }
    }

    public void setItem(JSONObject sessionObj, JSONObject itemObj) {
        String productName = itemObj.optString("name", "Không rõ");
        BigDecimal currentPrice = sessionObj.optBigDecimal("currentPrice", BigDecimal.ZERO);
        String endTime = sessionObj.optString("endTime", "");

        productNameLabel.setText("Sản phẩm: " + productName);
        currentPriceLabel.setText("Giá hiện tại: " + formatMoney(currentPrice));

        if (!endTime.isBlank()) {
            endTimeLabel.setText("Thời gian kết thúc: " + endTime.split("T")[0]);
            setRemainingTime(endTime);
        }
    }

    public void setRemainingTime(String endTimeStr) {
        LocalDateTime timeEnd = LocalDateTime.parse(endTimeStr);

        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            LocalDateTime timeNow = LocalDateTime.now();
            long secondsLeft = Duration.between(timeNow, timeEnd).getSeconds();

            if (secondsLeft <= 0) {
                timeline.stop();
                remainingTimeLabel.setText("Phiên đấu giá đã kết thúc!");
                handleAuctionEnd();
                return;
            }

            long hours = secondsLeft / 3600;
            long minutes = (secondsLeft % 3600) / 60;
            long seconds = secondsLeft % 60;

            remainingTimeLabel.setText(
                    String.format("Thời gian còn lại: %02d:%02d:%02d", hours, minutes, seconds)
            );
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void handleAuctionEnd() {
        placeBidBtn.setDisable(true);
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText("Phiên đấu giá đã kết thúc, không thể đặt giá.");
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        String inputPrice = bidAmountField.getText();

        if (inputPrice == null || inputPrice.trim().isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Vui lòng nhập mức giá!");
            return;
        }

        try {
            BigDecimal price = parseMoney(inputPrice);

            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText("Mức giá phải lớn hơn 0.");
                return;
            }

            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Đặt giá thành công: " + formatMoney(price));
            currentPriceLabel.setText("Giá cao nhất hiện tại: " + formatMoney(price));

        } catch (NumberFormatException e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Mức giá không hợp lệ! Vui lòng nhập số.");
        }
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 500, 400);
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Lỗi khi quay lại trang trước.");
        }
    }

    private BigDecimal parseMoney(String input) {
        String normalized = input.trim().replace(",", "");
        return new BigDecimal(normalized);
    }

    private String formatMoney(BigDecimal price) {
        return currencyFormat.format(price) + " VNĐ";
    }
}