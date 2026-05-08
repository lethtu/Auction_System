package com.auction.client.controller;

import javafx.fxml.FXMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import org.json.JSONArray;
import org.json.JSONObject;
import com.auction.client.model.User;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private Label lblWelcome;
    @FXML private FlowPane productContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (User.getFullname() != null) {
            lblWelcome.setText("Chào, " + User.getFullname());
        }

        loadProductsFromServer();
    }

    private void loadProductsFromServer() {
        System.out.println("check");
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/bidder/active-sessions"))
                        .GET()
                        .build();

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(response.body());

                    if (responseJson.getInt("status") == 200) {

                        JSONObject pageData = responseJson.getJSONObject("data");

                        JSONArray jsonArray = pageData.getJSONArray("content");

                        Platform.runLater(() -> {
                            productContainer.getChildren().clear();

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject sessionObj = jsonArray.getJSONObject(i);
                                JSONObject itemObj = sessionObj.getJSONObject("item");
                                logger.info("Session: {}, item: {}", sessionObj, itemObj);
                                VBox card = createProductCard(sessionObj, itemObj);
                                productContainer.getChildren().add(card);
                            }
                        });
                    }
                } else {
                    logger.error("Lỗi từ Server: {}", response.statusCode());
                }

            } catch (Exception e) {
                logger.error("Lỗi hệ thống khi tải sản phẩm!: {}", e.getMessage(), e);
            }
        }).start();
    }

    private VBox createProductCard(JSONObject sessionObj, JSONObject itemObj) {
        int id = sessionObj.getInt("id");
        String type = sessionObj.optString("status", "ACTIVE");
        double currentPrice = sessionObj.getDouble("currentPrice");

        String startTime = "Chưa bắt đầu";
        if (!sessionObj.isNull("startTime")) {
            startTime = sessionObj.getString("startTime").replace("T", " ");
        }

        String endTime = "Chưa rõ";
        if (!sessionObj.isNull("endTime")) {
            endTime = sessionObj.getString("endTime").replace("T", " ");
        }

        String name = itemObj.getString("name");
        String imagePath = itemObj.optString("imagePath", "default.png");

        VBox vbox = new VBox();
        vbox.setSpacing(10.0);
        vbox.setPrefWidth(220.0);
        vbox.setStyle("-fx-border-color: #dee2e6; -fx-border-radius: 5px; -fx-padding: 10px; -fx-background-color: white;");

        ImageView imageView = new ImageView();
        boolean hasImage = false;
        try {
            if (!imagePath.isEmpty()) {
                System.out.println(imagePath);
                String imageUrl = Config.API_URL + "/api/files/images/" + imagePath;
                Image image = new Image(imageUrl, true);
                imageView.setImage(image);
                hasImage = true;
            } else {
                logger.info("Không có ảnh");
                throw new Exception("Không có ảnh");
            }
        } catch (Exception e) {
            Label errorLabel = new Label("No Image");
            errorLabel.setAlignment(Pos.CENTER);
            errorLabel.setPrefSize(200.0, 130.0);
            errorLabel.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #adb5bd;");
            vbox.getChildren().add(errorLabel);
        }

        if (imageView.getImage() != null) {
            imageView.setFitHeight(130.0);
            imageView.setFitWidth(200.0);
            imageView.setPreserveRatio(true);
            vbox.getChildren().add(imageView);
        }

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14.0));
        nameLabel.setWrapText(true);

        // 3. Thể loại
        Label typeLabel = new Label("Loại: " + type);
        typeLabel.setFont(Font.font("System", 11.0));
        typeLabel.setStyle("-fx-font-style: italic;");
        typeLabel.setTextFill(Color.web("#6c757d"));

        // 4. Giá hiện tại
        Label priceLabel = new Label("Giá: " + String.format("%,.0f", currentPrice) + " VNĐ");
        priceLabel.setTextFill(Color.web("#d32f2f"));
        priceLabel.setFont(Font.font("System", FontWeight.BOLD, 13.0));

        String shortStart = startTime.length() >= 16 ? startTime.substring(0, 16) : startTime;
        String shortEnd = endTime.length() >= 16 ? endTime.substring(0, 16) : endTime;

        Label startTimeLabel = new Label("Bắt đầu: " + shortStart);
        startTimeLabel.setFont(Font.font("System", 11.0));
        startTimeLabel.setTextFill(Color.web("#6c757d"));

        Label endTimeLabel = new Label("Kết thúc: " + shortEnd);
        endTimeLabel.setFont(Font.font("System", 11.0));
        endTimeLabel.setTextFill(Color.web("#d32f2f"));

        Button bidBtn = new Button("Đấu giá ngay");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-cursor: hand;");

        bidBtn.setOnAction(event -> {
            logger.info(">>> Mở trang chi tiết cho sản phẩm ID: " + id);
            try {
                FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 500, 400);
                AuctionPageController controller = loader.getController();
                controller.setItem(sessionObj, itemObj);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        vbox.getChildren().addAll(nameLabel, typeLabel, priceLabel, startTimeLabel, endTimeLabel, bidBtn);

        return vbox;
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        User.clearSession();
        SceneSwitcher.switchScene(event, "Login.fxml", 400, 500);
    }
}