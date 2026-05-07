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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private Label lblWelcome;
    @FXML private FlowPane productContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbStatus;

    // Kho lưu trữ Caching cục bộ, giúp Real-time filter không bị trễ
    private final List<JSONObject> allProducts = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (User.getFullname() != null) {
            lblWelcome.setText("Chào, " + User.getFullname());
        }

        // Khởi tạo ComboBox
        cbCategory.getItems().addAll("Tất cả", "Electronics", "Art", "Vehicle");
        cbCategory.setValue("Tất cả");

        cbStatus.getItems().addAll("Tất cả", "ACTIVE", "PENDING", "ENDED");
        cbStatus.setValue("Tất cả");

        // Lắng nghe sự kiện để lọc Real-time
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterAndRenderProducts());
        cbCategory.setOnAction(event -> filterAndRenderProducts());
        cbStatus.setOnAction(event -> filterAndRenderProducts());

        loadProductsFromServer();
    }

    private void loadProductsFromServer() {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/auctions/all"))
                        .GET()
                        .build();

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(response.body());

                    if (responseJson.getInt("status") == 200) {
                        Object dataObj = responseJson.get("data");
                        JSONArray jsonArray = new JSONArray();

                        // Xử lý linh hoạt: Nếu API trả về Page (có content) hoặc mảng thuần
                        if (dataObj instanceof JSONObject) {
                            jsonArray = ((JSONObject) dataObj).getJSONArray("content");
                        } else if (dataObj instanceof JSONArray) {
                            jsonArray = (JSONArray) dataObj;
                        }

                        // Lưu vào bộ nhớ Caching
                        allProducts.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            allProducts.add(jsonArray.getJSONObject(i));
                        }

                        // Tiến hành render và gắn luồng UI vào Platform.runLater
                        filterAndRenderProducts();
                    }
                } else {
                    logger.error("Lỗi từ Server: {}", response.statusCode());
                }

            } catch (Exception e) {
                logger.error("Lỗi hệ thống khi tải sản phẩm!: {}", e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Hàm trung tâm xử lý Data-Driven UI: Lọc bộ đệm (RAM) và vẽ lại màn hình
     */
    private void filterAndRenderProducts() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        String selectedCategory = cbCategory.getValue();
        String selectedStatus = cbStatus.getValue();

        // Toàn bộ tương tác dọn dẹp và thêm Component UI bắt buộc phải nhét vào luồng chính JavaFX
        Platform.runLater(() -> {
            productContainer.getChildren().clear();

            for (JSONObject sessionObj : allProducts) {
                JSONObject itemObj = sessionObj.optJSONObject("item");
                if (itemObj == null) continue;

                String name = itemObj.optString("name", "");
                String type = itemObj.optString("type", "");
                String status = sessionObj.optString("status", "ACTIVE");

                // Logic lọc 3 lớp
                boolean matchKeyword = keyword.isEmpty() || name.toLowerCase().contains(keyword);
                boolean matchCategory = "Tất cả".equals(selectedCategory) || type.equalsIgnoreCase(selectedCategory);
                boolean matchStatus = "Tất cả".equals(selectedStatus) || status.equalsIgnoreCase(selectedStatus);

                if (matchKeyword && matchCategory && matchStatus) {
                    int id = sessionObj.optInt("id");
                    double currentPrice = sessionObj.optDouble("currentPrice", 0.0);

                    String startTime = sessionObj.isNull("startTime") ? "Chưa bắt đầu" : sessionObj.getString("startTime").replace("T", " ");
                    String endTime = sessionObj.isNull("endTime") ? "Chưa rõ" : sessionObj.getString("endTime").replace("T", " ");
                    String imagePath = itemObj.optString("imagePath", "default.png");

                    // Truyền cả type (thể loại) và status (trạng thái) vào
                    VBox card = createProductCard(id, name, type, status, currentPrice, startTime, endTime, imagePath);
                    productContainer.getChildren().add(card);
                }
            }
        });
    }

    private VBox createProductCard(int id, String name, String type, String status, double currentPrice, String startTime, String endTime, String imagePath) {
        VBox vbox = new VBox();
        vbox.setSpacing(10.0);
        vbox.setPrefWidth(220.0);
        vbox.setStyle("-fx-border-color: #dee2e6; -fx-border-radius: 5px; -fx-padding: 10px; -fx-background-color: white;");

        ImageView imageView = new ImageView();
        try {
            if (!imagePath.isEmpty()) {
                String imageUrl = Config.API_URL + "/api/files/images/" + imagePath;
                Image image = new Image(imageUrl, true);
                imageView.setImage(image);
            } else {
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

        // THÊM MỚI: In Thể loại
        Label categoryLabel = new Label("Thể loại: " + type);
        categoryLabel.setFont(Font.font("System", 11.0));
        categoryLabel.setTextFill(Color.web("#28a745")); // Màu xanh lá cho thể loại

        // SỬA LẠI: In Trạng thái
        Label statusLabel = new Label("Trạng thái: " + status);
        statusLabel.setFont(Font.font("System", 11.0));
        statusLabel.setStyle("-fx-font-style: italic;");
        statusLabel.setTextFill(Color.web("#6c757d"));

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

        // Tối ưu nút: Nếu phiên Ended hoặc Pending thì disable nút bid
        if (!"ACTIVE".equalsIgnoreCase(type)) {
            bidBtn.setDisable(true);
            bidBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white;");
            bidBtn.setText("Đã đóng / Chờ duyệt");
        } else {
            bidBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-cursor: hand;");
            bidBtn.setOnAction(event -> {
                logger.info(">>> Mở trang chi tiết cho sản phẩm ID: " + id);
                try {
                    FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 500, 400);
                    AuctionPageController controller = loader.getController();
                    // Bạn có thể truyền dữ liệu sang trang chi tiết qua controller ở đây
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        vbox.getChildren().addAll(nameLabel, categoryLabel, statusLabel, priceLabel, startTimeLabel, endTimeLabel, bidBtn);

        return vbox;
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        User.clearSession();
        SceneSwitcher.switchScene(event, "Login.fxml", 400, 500);
    }
}