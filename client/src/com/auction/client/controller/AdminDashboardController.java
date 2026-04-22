package com.auction.client.controller;

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
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được ID admin hiện tại.");
            return;
        }

        try {
            String url = "http://localhost:8080/api/admin/approve/" + selected.getId() + "?adminId=" + adminId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Phê duyệt phiên thành công.");
                loadPendingSessions();
            } else {
                String body = response.body() == null ? "" : response.body();
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Phê duyệt thất bại.\n" + body);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ.");
        }
    }

    private void loadPendingSessions() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/admin/pending"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tải được danh sách phiên chờ duyệt.");
                return;
            }

            String body = response.body();
            List<PendingSessionRow> rows = parsePendingSessions(body);

            tablePending.setItems(FXCollections.observableArrayList(rows));

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể tải dữ liệu pending từ server.");
        }
    }

    private List<PendingSessionRow> parsePendingSessions(String body) {
        List<PendingSessionRow> rows = new ArrayList<>();

        if (body == null || body.isBlank()) {
            return rows;
        }

        JSONArray array;

        String trimmed = body.trim();

        if (trimmed.startsWith("[")) {
            array = new JSONArray(trimmed);
        } else {
            JSONObject obj = new JSONObject(trimmed);

            if (obj.has("data") && obj.get("data") instanceof JSONArray) {
                array = obj.getJSONArray("data");
            } else {
                array = new JSONArray();
            }
        }

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
        if (item.has("startingPrice")) {
            return item.optDouble("startingPrice", 0.0);
        }

        return 0.0;
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
}