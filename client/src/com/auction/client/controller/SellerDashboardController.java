package com.auction.client.controller;

import com.auction.client.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardController {

    @FXML private ListView<String> mySessionsList;
    @FXML private ComboBox<String> productTypeCombo;
    @FXML private TextField productNameField;
    @FXML private TextField imageUrlField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startingPriceField;
    @FXML private TextField stepPriceField;
    @FXML private TextField endTimeField;
    @FXML private TextArea statsArea;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final List<SessionItem> allSessions = new ArrayList<>();
    private final List<SessionItem> displayedSessions = new ArrayList<>();

    @FXML
    public void initialize() {
        productTypeCombo.setItems(FXCollections.observableArrayList(
                "Electronics", "Art", "Vehicle"
        ));
        productTypeCombo.setValue("Electronics");
        fillDefaultEndTime();
        loadMySessions();
    }

    @FXML
    private void handleCreateSession() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        String productName = productNameField.getText().trim();
        String productType = productTypeCombo.getValue();
        String imageUrl = productNameOrEmpty(imageUrlField);
        String description = productNameOrEmpty(descriptionArea);
        String startingPriceText = productNameOrEmpty(startingPriceField);
        String stepPriceText = productNameOrEmpty(stepPriceField);
        String endTime = productNameOrEmpty(endTimeField);

        if (productName.isEmpty() || productType == null || startingPriceText.isEmpty() || stepPriceText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu",
                    "Vui lòng nhập tên sản phẩm, loại, giá khởi điểm và bước giá.");
            return;
        }

        if (endTime.isEmpty()) {
            endTime = defaultEndTime();
            endTimeField.setText(endTime);
        }

        try {
            BigDecimal startingPrice = new BigDecimal(startingPriceText.trim());
            BigDecimal stepPrice = new BigDecimal(stepPriceText.trim());

            JSONObject body = new JSONObject();
            body.put("productName", productName);
            body.put("productType", productType);
            body.put("imageUrl", imageUrl);
            body.put("description", description);
            body.put("startingPrice", startingPrice);
            body.put("stepPrice", stepPrice);
            body.put("endTime", endTime);
            body.put("sellerId", sellerId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/seller/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                clearForm();
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tạo phiên đấu giá thành công.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", safeMessage(response.body()));
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Giá khởi điểm và bước giá phải là số.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    @FXML
    private void handleShowAllSessions() {
        renderSessions(allSessions);
    }

    @FXML
    private void handleShowPendingSessions() {
        renderSessions(filterByStatus("PENDING"));
    }

    @FXML
    private void handleShowActiveSessions() {
        renderSessions(filterByStatus("ACTIVE"));
    }

    @FXML
    private void handleShowRejectedSessions() {
        renderSessions(filterByStatus("REJECTED"));
    }

    @FXML
    private void handleEditSelectedSession() {
        showAlert(Alert.AlertType.INFORMATION,
                "Tạm khóa chức năng",
                "Chức năng sửa phiên đang tạm khóa để bản demo ổn định hơn.\nBạn có thể demo tạo, xem, lọc, hủy phiên và phân quyền.");
    }

    @FXML
    private void handleCancelSelectedSession() {
        SessionItem selected = getSelectedSession();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn phiên", "Bạn hãy chọn một phiên để hủy.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(selected.status)) {
            showAlert(Alert.AlertType.WARNING, "Không thể hủy",
                    "Chỉ được hủy phiên đang ở trạng thái PENDING.");
            return;
        }

        Integer sellerId = User.getId();
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn hủy phiên #" + selected.id + " không?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/seller/cancel-session/" + selected.id + "?sellerId=" + sellerId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã hủy phiên thành công.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", safeMessage(response.body()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    @FXML
    private void goBack(javafx.event.ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1024, 768);
    }

    private void loadMySessions() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/seller/my-sessions/" + sellerId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tải được danh sách phiên.");
                return;
            }

            JSONArray array = new JSONArray(response.body());
            allSessions.clear();

            for (int i = 0; i < array.length(); i++) {
                allSessions.add(parseSession(array.getJSONObject(i)));
            }

            renderSessions(allSessions);
            updateStats();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể tải dữ liệu seller từ server.");
        }
    }

    private SessionItem parseSession(JSONObject item) {
        SessionItem s = new SessionItem();
        s.id = item.optInt("id", 0);

        if (item.has("productName")) {
            s.productName = item.optString("productName", "Không rõ");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.productName = product != null ? product.optString("name", "Không rõ") : "Không rõ";
        } else {
            s.productName = "Không rõ";
        }

        if (item.has("productType")) {
            s.productType = item.optString("productType", "");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.productType = product != null ? product.optString("type", "") : "";
        }

        if (item.has("imageUrl")) {
            s.imageUrl = item.optString("imageUrl", "");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.imageUrl = product != null ? product.optString("imageUrl", "") : "";
        }

        if (item.has("description")) {
            s.description = item.optString("description", "");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.description = product != null ? product.optString("description", "") : "";
        }

        s.startingPrice = parseBigDecimal(item, "startingPrice");
        s.currentPrice = parseBigDecimal(item, "currentPrice");
        s.stepPrice = parseBigDecimal(item, "stepPrice");
        s.endTime = item.optString("endTime", "");
        s.status = item.optString("status", "UNKNOWN");
        return s;
    }

    private BigDecimal parseBigDecimal(JSONObject item, String key) {
        if (!item.has(key) || item.isNull(key)) {
            return BigDecimal.ZERO;
        }

        Object value = item.get(key);

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }

        String text = value.toString().trim();
        if (text.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(text);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void renderSessions(List<SessionItem> sessions) {
        displayedSessions.clear();
        displayedSessions.addAll(sessions);

        List<String> rendered = new ArrayList<>();
        for (SessionItem s : sessions) {
            rendered.add("Session #" + s.id + " | " + s.productName + " | " + s.status);
        }
        mySessionsList.setItems(FXCollections.observableArrayList(rendered));
    }

    private List<SessionItem> filterByStatus(String status) {
        List<SessionItem> filtered = new ArrayList<>();
        for (SessionItem s : allSessions) {
            if (s.status != null && s.status.equalsIgnoreCase(status)) {
                filtered.add(s);
            }
        }
        return filtered;
    }

    private SessionItem getSelectedSession() {
        int index = mySessionsList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= displayedSessions.size()) {
            return null;
        }
        return displayedSessions.get(index);
    }

    private void updateStats() {
        int pending = 0, active = 0, rejected = 0, completed = 0, canceled = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        for (SessionItem s : allSessions) {
            if (s.status == null) continue;
            switch (s.status.toUpperCase()) {
                case "PENDING" -> pending++;
                case "ACTIVE" -> active++;
                case "REJECTED" -> rejected++;
                case "COMPLETED" -> {
                    completed++;
                    if (s.currentPrice != null) {
                        revenue = revenue.add(s.currentPrice);
                    }
                }
                case "CANCELED" -> canceled++;
            }
        }

        DecimalFormat df = new DecimalFormat("#,##0.##");
        statsArea.setText(
                "Tổng số phiên: " + allSessions.size() + "\n" +
                        "Số phiên chờ duyệt: " + pending + "\n" +
                        "Số phiên đang hoạt động: " + active + "\n" +
                        "Số phiên bị từ chối: " + rejected + "\n" +
                        "Số phiên đã hoàn thành: " + completed + "\n" +
                        "Số phiên đã hủy: " + canceled + "\n" +
                        "Tổng doanh thu phiên hoàn thành: " + df.format(revenue)
        );
    }

    private void clearForm() {
        productNameField.clear();
        imageUrlField.clear();
        descriptionArea.clear();
        startingPriceField.clear();
        stepPriceField.clear();
        fillDefaultEndTime();
        productTypeCombo.setValue("Electronics");
    }

    private void fillDefaultEndTime() {
        if (endTimeField != null && endTimeField.getText().trim().isEmpty()) {
            endTimeField.setText(defaultEndTime());
        }
    }

    private String defaultEndTime() {
        return LocalDateTime.now().plusDays(7).withSecond(0).withNano(0).toString();
    }

    private String productNameOrEmpty(TextInputControl input) {
        return input == null ? "" : input.getText().trim();
    }

    private String safeMessage(String body) {
        if (body == null || body.isBlank()) return "Có lỗi xảy ra từ server.";
        return body;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class SessionItem {
        int id;
        String productName;
        String productType;
        String imageUrl;
        String description;
        BigDecimal startingPrice = BigDecimal.ZERO;
        BigDecimal currentPrice = BigDecimal.ZERO;
        BigDecimal stepPrice = BigDecimal.ZERO;
        String endTime;
        String status;
    }
}