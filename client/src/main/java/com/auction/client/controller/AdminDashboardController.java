package com.auction.client.controller;

import javafx.event.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import com.auction.client.model.User;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    @FXML
    private TableView<PendingSessionRow> tablePending;

    @FXML
    private TableColumn<PendingSessionRow, Integer> colId;

    @FXML
    private TableColumn<PendingSessionRow, String> colProduct;

    @FXML
    private TableColumn<PendingSessionRow, Double> colPrice;

    private final HttpClient client = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

        loadPendingSessions();
    }

    @FXML
    public void handleApprove() {
        PendingSessionRow selected = tablePending.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn phiên", "Hãy chọn một phiên để phê duyệt.");
            return;
        }

        int adminId = getCurrentAdminId();
        if (adminId <= 0) {
            logger.info("Không lấy được ID admin hiện tại");
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được ID admin hiện tại.");
            return;
        }

        try {
            String url = Config.API_URL + "/api/admin/approve/" + selected.getId() + "?adminId=" + adminId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(), "Phê duyệt phiên thành công.");

            if (api.success) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", api.message);
                loadPendingSessions();
            } else {
                logger.info("Lỗi khi gọi API: {}", api.message);
                showAlert(Alert.AlertType.ERROR, "Lỗi", api.message);
            }

        } catch (Exception e) {
            logger.error("Lỗi không kết nối được đến máy chủ: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ.");
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            // Xóa thông tin user đang lưu ở bộ nhớ tạm
            User.clearSession();
            // Chuyển về màn hình đăng nhập
            SceneSwitcher.switchScene(event, "Login.fxml", 400, 500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPendingSessions() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/admin/pending"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ApiArrayResult api = extractDataArray(response.body(), response.statusCode());

            if (!api.success) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", api.message);
                return;
            }

            List<PendingSessionRow> rows = parsePendingSessions(api.data);
            tablePending.setItems(FXCollections.observableArrayList(rows));

        } catch (Exception e) {
            logger.error("Lỗi không thể tải dữ liệu từ server: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể tải dữ liệu pending từ server.");
        }
    }

    private List<PendingSessionRow> parsePendingSessions(JSONArray array) {
        List<PendingSessionRow> rows = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);

            int id = item.optInt("id", 0);
            String productName = extractProductName(item);
            double startingPrice = extractStartingPrice(item);

            rows.add(new PendingSessionRow(id, productName, startingPrice));
        }

        return rows;
    }

    private String extractProductName(JSONObject item) {
        if (item.has("productName")) {
            return item.optString("productName", "Không rõ");
        }

        if (item.has("product") && item.get("product") instanceof JSONObject) {
            JSONObject product = item.getJSONObject("product");
            return product.optString("name", "Không rõ");
        }

        return "Không rõ";
    }

    private double extractStartingPrice(JSONObject item) {
        if (!item.has("startingPrice") || item.isNull("startingPrice")) {
            return 0.0;
        }

        Object value = item.get("startingPrice");
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private ApiResult parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        if (body == null || body.isBlank()) {
            return new ApiResult(httpStatus >= 200 && httpStatus < 300,
                    httpStatus >= 200 && httpStatus < 300 ? defaultSuccessMessage : "Có lỗi xảy ra từ server.");
        }

        try {
            String trimmed = body.trim();
            if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                int status = obj.optInt("status", httpStatus);
                String message = obj.optString("message",
                        status >= 200 && status < 300 ? defaultSuccessMessage : "Có lỗi xảy ra từ server.");
                return new ApiResult(status >= 200 && status < 300, message);
            }
        } catch (Exception ignored) {
        }

        return new ApiResult(httpStatus >= 200 && httpStatus < 300,
                httpStatus >= 200 && httpStatus < 300 ? defaultSuccessMessage : body);
    }

    private ApiArrayResult extractDataArray(String body, int httpStatus) {
        if (body == null || body.isBlank()) {
            return new ApiArrayResult(false, "Không có dữ liệu từ server.", new JSONArray());
        }

        try {
            String trimmed = body.trim();

            if (trimmed.startsWith("[")) {
                return new ApiArrayResult(true, "OK", new JSONArray(trimmed));
            }

            JSONObject obj = new JSONObject(trimmed);
            int status = obj.optInt("status", httpStatus);
            String message = obj.optString("message", "Có lỗi xảy ra từ server.");

            if (status < 200 || status >= 300) {
                return new ApiArrayResult(false, message, new JSONArray());
            }

            Object data = obj.opt("data");
            if (data instanceof JSONArray) {
                return new ApiArrayResult(true, message, (JSONArray) data);
            }

            return new ApiArrayResult(true, message, new JSONArray());
        } catch (Exception e) {
            logger.error("Không đọc được dữ liệu từ server: {}",e.getMessage(), e);
            return new ApiArrayResult(false, "Không đọc được dữ liệu từ server.", new JSONArray());
        }
    }

    private int getCurrentAdminId() {
        try {
            Method method = User.class.getMethod("getId");
            Object value = method.invoke(null);

            if (value instanceof Integer) {
                return (Integer) value;
            }

            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Exception ignored) {
        }

        return 15;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class PendingSessionRow {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty productName;
        private final SimpleDoubleProperty startingPrice;

        public PendingSessionRow(int id, String productName, double startingPrice) {
            this.id = new SimpleIntegerProperty(id);
            this.productName = new SimpleStringProperty(productName);
            this.startingPrice = new SimpleDoubleProperty(startingPrice);
        }

        public int getId() {
            return id.get();
        }

        public String getProductName() {
            return productName.get();
        }

        public double getStartingPrice() {
            return startingPrice.get();
        }
    }

    private static class ApiResult {
        boolean success;
        String message;

        ApiResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private static class ApiArrayResult {
        boolean success;
        String message;
        JSONArray data;

        ApiArrayResult(boolean success, String message, JSONArray data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }
}