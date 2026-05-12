package com.auction.client.controller;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.Cursor;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.control.Separator;
import javafx.geometry.Insets;
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
    
    // Nạp font chữ tĩnh trước khi FXML parse các component con
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

    // Kho lưu trữ Caching cục bộ, giúp Real-time filter không bị trễ
    private final List<JSONObject> allProducts = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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

        loadProductsFromServer();
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

    private void loadProductsFromServer() {
        new Thread(() -> {
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

                // ĐÃ SỬA: Chặn đứng PENDING, REJECTED, CANCELED. Chỉ cho ACTIVE và ENDED lên sàn chính.
                if ("PENDING".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)) {
                    continue;
                }

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
                    VBox card = createProductCard(sessionObj, itemObj);
                    productContainer.getChildren().add(card);
                }
            }
            // Sau khi đổ xong dữ liệu, tính toán lại Layout
            updateGridLayout();
        });
    }

    private VBox createProductCard(JSONObject sessionObj, JSONObject itemObj) {
        int id = sessionObj.getInt("id");

        String type = itemObj.optString("type", "");
        String name = itemObj.optString("name", "");
        double currentPrice = sessionObj.getDouble("currentPrice");

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
            errorLabel.setStyle("-fx-text-fill: #adb5bd;");
            imageWrapper.getChildren().add(errorLabel);
        }

        if (imageView.getImage() != null) {
            imageView.setFitHeight(192.0);
            imageView.setFitWidth(208.0);
            imageView.setPreserveRatio(true);
            imageWrapper.getChildren().add(imageView);
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
        Label priceLabel = new Label(String.format("%,.0f", currentPrice) + " ₫");
        priceLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 18px; -fx-text-fill: #e040a0;");
        priceBox.getChildren().addAll(lblCurrentBid, priceLabel);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Button bidBtn = new Button();
        Label addIcon = new Label("\uE145");
        addIcon.getStyleClass().add("material-icon");

        if ("ACTIVE".equalsIgnoreCase(status)) {
            addIcon.setStyle("-fx-text-fill: #e040a0;");
            bidBtn.setStyle("-fx-background-color: #ffd6ee; -fx-background-radius: 20px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-cursor: hand;");
            bidBtn.setGraphic(addIcon);
            bidBtn.setOnAction(event -> {
                try {
                    FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 500, 400);
                    AuctionPageController controller = loader.getController();
                    controller.setItem(sessionObj, itemObj);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            addIcon.setStyle("-fx-text-fill: #907898;");
            bidBtn.setStyle("-fx-background-color: #f2e8f2; -fx-background-radius: 20px; -fx-min-width: 40px; -fx-min-height: 40px;");
            bidBtn.setGraphic(addIcon);
            bidBtn.setDisable(true);
        }

        bottomRow.getChildren().addAll(priceBox, hSpacer, bidBtn);

        vbox.getChildren().addAll(imageWrapper, nameLabel, categoryLabel, spacer, bottomRow);

        return vbox;
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
            // Có thể chỉnh lại kích thước width, height cho vừa vặn.
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1024, 768);
        } catch (Exception e) {
            logger.error("Lỗi khi chuyển về trang Quản lý Seller: ", e);
        }
    }
}